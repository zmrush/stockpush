package push.registry.zookeeper;

import push.registry.EventListener;
import push.registry.EventManager;
import push.registry.PathData;
import push.registry.URL;
import push.registry.listener.ConnectionEvent;
import push.registry.listener.ConnectionEvent.ConnectionEventType;
import push.registry.listener.ConnectionListener;
import push.registry.util.PathUtil;
import org.apache.zookeeper.*;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.common.PathUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * zookeeper客户端
 *
 */
public class ZKClient {
    private static final Logger logger = LoggerFactory.getLogger(ZKClient.class);
    //保留当前的SessionID
    protected final AtomicLong zkSessionId = new AtomicLong();
    //zookeeper的连接URL
    protected URL url;
    //zookeeper对象
    protected volatile ZooKeeper zooKeeper;
    //用于多注册中心时手动控制重连次数
    protected int connectionRetryTimes = 0;
    //重试次数
    protected int retryTimes = 1;
    //重试间隔
    protected long retryInterval = 1000;
    //会话超时
    protected int sessionTimeout = 30000;
    //是否能从Observe节点读取
    protected boolean canBeReadOnly = false;
    //是否关闭
    protected String env;
    protected AtomicBoolean closed = new AtomicBoolean(false);
    protected AtomicBoolean connected = new AtomicBoolean(false);
    //连接成功
    protected BlockingQueue<ConnectionState> mailbox = new ArrayBlockingQueue<ConnectionState>(1);
    //连接事件
    protected EventManager<ConnectionEvent> eventManager;
    //连接请求
    protected EventManager<ConnectionTask> taskManager;

    public ZKClient(URL url, List<ConnectionListener> listeners) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        this.url = url;
        this.connectionRetryTimes = url.getParameter("connectionRetryTimes", connectionRetryTimes);
        this.retryTimes = url.getParameter("retryTimes", retryTimes);
        this.retryInterval = url.getPositiveParameter("retryInterval", retryInterval);
        this.sessionTimeout = url.getPositiveParameter("sessionTimeout", sessionTimeout);
        this.canBeReadOnly = url.getParameter("canBeReadOnly", canBeReadOnly);
        this.env=url.getParameter("env","test");
        // 连接事件管理器
        eventManager = new EventManager<ConnectionEvent>("zk_connection_event", listeners);
        eventManager.start();
        // 连接任务
        taskManager = new EventManager<ConnectionTask>("zk_connection_task", new TaskListener());
        taskManager.start();
        // 每个interval都会触发事件
        taskManager.setTriggerNoEvent(true);
        // 增加连接任务
        taskManager.add(new ConnectionTask());
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            closed.set(true);
            connected.set(false);
            //关闭zookeeper
            closeZookeeper();
            eventManager.stop();
            taskManager.stop();
        } finally {
        }
    }

    /**
     * 关闭zookeeper
     */
    protected void closeZookeeper() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * 检查状态
     */
    protected void checkState() throws KeeperException {
        if (zooKeeper == null) {
            throw new KeeperException.ConnectionLossException();
        }
        if (connected.get()) {
            return;
        }
        States states = zooKeeper.getState();
        if (states == States.AUTH_FAILED) {
            throw new KeeperException.AuthFailedException();
        }
        throw new KeeperException.ConnectionLossException();
    }

    /**
     * 创建节点
     *
     * @param path       路径
     * @param data       数据
     * @param createMode 创建模式
     * @return 节点路径
     * @throws Exception
     */
    public String create(final String path, final byte[] data, final CreateMode createMode) throws Exception {
        if (path == null) {
            return null;
        }
        return RetryLoop.callWithRetry(this, new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    //检查状态
                    checkState();
                    //确保父节点创建好了
                    mkdirs(PathUtil.getParentFromPath(path));
                    return zooKeeper.create(path, data, Ids.OPEN_ACL_UNSAFE, createMode);
                } catch (NodeExistsException e) {
                    if (createMode == CreateMode.EPHEMERAL || createMode == CreateMode.EPHEMERAL_SEQUENTIAL) {
                        Stat stat = new Stat();
                        zooKeeper.getData(path, null, stat);
                        if (stat.getEphemeralOwner() != zkSessionId.get()) {
                            //sessionId已经发生变化
                            zooKeeper.delete(path, -1);
                            zooKeeper.create(path, data, Ids.OPEN_ACL_UNSAFE, createMode);
                        }
                    } else if (data != null && data.length > 0) {
                        update(path, data);
                    }
                    return path;
                }
            }
        });

    }

    /**
     * 创建多个节点
     *
     * @param paths 节点列表
     * @throws Exception
     */
    public void create(final List<String> paths) throws Exception {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //检查状态
                checkState();
                //循环创建节点
                for (String path : paths) {
                    create(path, null, CreateMode.PERSISTENT);
                }
                return null;
            }
        });
    }

    /**
     * 更新数据
     *
     * @param path 路径
     * @param data 数据
     * @throws Exception
     */
    public void update(String path, byte[] data) throws Exception {
        update(path, data, null);
    }

    /**
     * 更新数据
     *
     * @param path   路径
     * @param data   数据
     * @param parent 父节点数据
     * @throws Exception
     */
    public void update(final String path, final byte[] data, final byte[] parent) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //检查状态
                checkState();
                //创建节点
                mkdirs(path);
                //设置节点数据
                if (parent != null) {
                    zooKeeper.setData(path, data, -1);
                    zooKeeper.setData(PathUtil.getParentFromPath(path), parent, -1);
                } else {
                    zooKeeper.setData(path, data, -1);
                }
                return null;
            }
        });
    }

    /**
     * 删除路径
     *
     * @param path 路径
     * @throws Exception
     */
    public void delete(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //检查状态
                checkState();
                try {
                    //删除节点
                    zooKeeper.delete(path, -1);
                } catch (NoNodeException e) {
                    // ignore
                }
                return null;
            }
        });
    }

    /**
     * 删除多个路径
     *
     * @param paths 路径集合
     * @throws Exception
     */
    public void delete(final List<String> paths) throws Exception {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //检查状态
                checkState();
                //循环删除节点
                for (String path : paths) {
                    delete(path);
                }
                return null;
            }
        });
    }

    /**
     * 递归删除路径
     *
     * @param path 路径
     * @throws Exception
     */
    public void deleteRecursive(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //检查状态
                checkState();
                PathUtils.validatePath(path);
                //遍历删除子节点
                List<String> tree = ZKUtil.listSubTreeBFS(zooKeeper, path);
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting " + tree);
                    logger.debug("Deleting " + tree.size() + " subnodes ");
                }
                for (int i = tree.size() - 1; i >= 0; --i) {
                    try {
                        // Delete the leaves first and eventually get rid of the root
                        // Delete all versions of the node with -1.
                        zooKeeper.delete(tree.get(i), -1);
                    } catch (NoNodeException e) {
                        // ignore
                    }
                }
                return null;
            }
        });
    }

    /**
     * 获取数据
     *
     * @param path 路径
     * @return 数据
     * @throws Exception
     */
    public PathData getData(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return getData(path, null, null);
    }

    /**
     * 获取数据
     *
     * @param path 路径
     * @param stat 状态数据
     * @return 数据
     * @throws Exception
     */
    public PathData getData(final String path, final Stat stat) throws Exception {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return getData(path, null, stat);
    }

    /**
     * 获取数据
     *
     * @param path    路径
     * @param watcher 监听器
     * @return 数据
     * @throws Exception
     */
    public PathData getData(String path, Watcher watcher) throws Exception {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return getData(path, watcher, null);
    }

    /**
     * 获取数据
     *
     * @param path    路径
     * @param watcher 监听器
     * @param stat    stat
     * @return PathData
     * @throws Exception
     */
    public PathData getData(final String path, final Watcher watcher, final Stat stat) throws Exception {
        return RetryLoop.callWithRetry(this, new Callable<PathData>() {
            @Override
            public PathData call() throws Exception {
                //检查状态
                checkState();
                byte[] data = null;
                if (watcher == null) {
                    //没有监听器，直接获取数据
                    try {
                        data = zooKeeper.getData(path, false, stat);
                    } catch (NoNodeException e) {
                        //ignore
                    }
                } else {
                    //有监听器，先创建节点
                    mkdirs(path);
                    data = zooKeeper.getData(path, watcher, stat);
                }
                return new PathData(PathUtil.getNodeFromPath(path), data);
            }
        });
    }

    /**
     * 获取子节点数据
     *
     * @param path 路径
     * @return 子节点数据集合
     * @throws Exception
     */
    public List<PathData> getChildData(String path) throws Exception {
        List<String> children = getChildren(path);
        if (children == null || children.size() < 1) {
            return new ArrayList<PathData>();
        }
        List<PathData> result = new ArrayList<PathData>(children.size());
        for (String childPath : children) {
            result.add(getData(PathUtil.makePath(path, childPath)));
        }
        return result;
    }

    /**
     * 获取排序的子节点数据
     *
     * @param path 路径
     * @return 子节点数据集合
     * @throws Exception
     */
    public List<PathData> getSortedChildData(String path) throws Exception {
        List<String> children = getChildren(path);
        if (children == null || children.size() < 1) {
            return new ArrayList<PathData>();
        }
        Collections.sort(children);
        List<PathData> result = new ArrayList<PathData>(children.size());
        for (String childPath : children) {
            result.add(getData(PathUtil.makePath(path, childPath)));
        }
        return result;
    }

    /**
     * 获取子节点数据
     *
     * @param path    路径
     * @param watcher 监听器
     * @return 子节点数据集合
     * @throws Exception
     */
    public List<PathData> getChildData(String path, final Watcher watcher) throws Exception {
        List<String> children = getChildren(path, watcher);
        if (children == null || children.size() < 1) {
            return new ArrayList<PathData>();
        }
        List<PathData> result = new ArrayList<PathData>(children.size());
        for (String childPath : children) {
            result.add(getData(PathUtil.makePath(path, childPath)));
        }
        return result;
    }

    /**
     * 获取排序的子节点数据
     *
     * @param path    路径
     * @param watcher 监听器
     * @return 子节点数据集合
     * @throws Exception
     */
    public List<PathData> getSortedChildData(String path, final Watcher watcher) throws Exception {
        List<String> children = getChildren(path, watcher);
        if (children == null || children.size() < 1) {
            return new ArrayList<PathData>();
        }
        Collections.sort(children);
        List<PathData> result = new ArrayList<PathData>(children.size());
        for (String childPath : children) {
            result.add(getData(PathUtil.makePath(path, childPath)));
        }
        return result;
    }

    /**
     * 获取子节点
     *
     * @param path 路径
     * @return 子节点
     * @throws Exception
     */
    public List<String> getChildren(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return new ArrayList<String>();
        }
        return RetryLoop.callWithRetry(this, new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                //检查状态
                checkState();

                //获取子节点
                List<String> children = null;
                try {
                    children = zooKeeper.getChildren(path, false);
                } catch (NoNodeException e) {
                    //ignore
                }
                return children == null ? new ArrayList<String>() : children;
            }
        });
    }

    /**
     * 获取排序的子节点
     *
     * @param path 路径
     * @return 子节点
     * @throws Exception
     */
    public List<String> getSortedChildren(final String path) throws Exception {
        List<String> children = getChildren(path);
        Collections.sort(children);
        return children;
    }

    /**
     * 获取子节点，并设置监听器
     *
     * @param path    路径
     * @param watcher 监听器
     * @return 子节点
     * @throws Exception
     */
    public List<String> getChildren(final String path, final Watcher watcher) throws Exception {
        if (path == null || path.isEmpty()) {
            return new ArrayList<String>();
        }
        return RetryLoop.callWithRetry(this, new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                //检查状态
                checkState();
                //创建节点
                mkdirs(path);
                //获取子节点数据，并设置监听器
                List<String> children = zooKeeper.getChildren(path, watcher);
                return children == null ? new ArrayList<String>() : children;
            }
        });
    }

    /**
     * 得到当前的SessionId
     *
     * @return 当前的SessionId
     */
    public long getSessionId() {
        return zkSessionId.get();
    }

    /**
     * 判断节点是否存在
     *
     * @param path 路径
     * @return 节点是否存在
     * @throws Exception
     */
    public boolean exists(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return RetryLoop.callWithRetry(this, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //检查状态
                checkState();
                return (zooKeeper.exists(path, false) != null);
            }
        });
    }

    /**
     * 判断节点是否存在
     *
     * @param path    路径
     * @param watcher 监听器
     * @return 节点是否存在
     * @throws Exception
     */
    public boolean exists(final String path, final Watcher watcher) throws Exception {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return RetryLoop.callWithRetry(this, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //检查状态
                checkState();
                return zooKeeper.exists(path, watcher) != null;
            }
        });
    }

    /**
     * 获取Zookeeper连接状态
     *
     * @return 连接状态
     */
    public States getStates() {
        if (zooKeeper == null) {
            return null;
        }
        return zooKeeper.getState();
    }

    /**
     * 创建重试
     *
     * @return 重试
     */
    protected RetryLoop createRetryLoop() {
        return new RetryLoop(retryTimes, retryInterval);
    }

    /**
     * 增加连接监听器
     *
     * @param listener 连接监听器
     */
    public void addListener(ConnectionListener listener) {
        if (eventManager.addListener(listener)) {
            if (isConnected()) {
                eventManager.add(new ConnectionEvent(ConnectionEventType.CONNECTED, url), listener);
            }
        }
    }

    /**
     * 删除连接监听器
     *
     * @param listener 连接监听器
     */
    public void removeListener(ConnectionListener listener) {
        eventManager.removeListener(listener);
    }

    /**
     * 创建节点
     *
     * @param path 路径
     * @throws Exception
     */
    public void mkdirs(final String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (exists(path)) {
            //路径已经存在
            return;
        }
        RetryLoop.callWithRetry(this, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                checkState();
                PathUtils.validatePath(path);

                int pos = 1; // skip first slash, root is guaranteed to exist
                do {
                    pos = path.indexOf('/', pos + 1);
                    if (pos == -1) {
                        pos = path.length();
                    }

                    String subPath = path.substring(0, pos);
                    if (!exists(subPath)) {
                        try {
                            zooKeeper.create(subPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        } catch (KeeperException.NodeExistsException e) {
                            // ignore... someone else has created it since we checked
                        }
                    }

                } while (pos < path.length());
                return null;
            }
        });
    }

    /**
     * 是否连接上
     *
     * @return 是否连接上
     */
    public boolean isConnected() {
        if (zooKeeper == null) {
            return false;
        }
        return connected.get();
    }

    /**
     * 连接状态
     */
    protected enum ConnectionState {
        /**
         * 成功
         */
        SUCCESS,
        /**
         * 超时失败
         */
        TIMEOUT,
    }

    /**
     * 连接类型
     */
    protected enum ConnectionTaskType {
        /**
         * 连接
         */
        CONNECTION,
        /**
         * 等到自动重连
         */
        RECONNECTION
    }

    /**
     * 连接请求
     */
    protected class ConnectionTask {
        //任务创建时间
        private long createTime;
        //连接类型
        private ConnectionTaskType type;

        public ConnectionTask() {
            this(ConnectionTaskType.CONNECTION);
        }

        public ConnectionTask(ConnectionTaskType type) {
            this.createTime = System.currentTimeMillis();
            this.type = type;
        }

        public long getCreateTime() {
            return createTime;
        }

        public ConnectionTaskType getType() {
            return type;
        }

        public void setType(ConnectionTaskType type) {
            this.type = type;
        }
    }

    /**
     * 连接监听器
     */
    protected class ConnectionWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            //已经关闭了
            if (closed.get()) {
                return;
            }

            //处理连接事件
            if (logger.isDebugEnabled()) {
                logger.debug("Zookeeper event: " + event);
            }
            //确保zookeeper被赋值
            switch (event.getState()) {
                case SyncConnected: {
                    connected.set(true);
                    onSyncConnected();
                    break;
                }
                case Expired: {
                    connected.set(false);
                    onExpired();
                    break;
                }
                case Disconnected: {
                    connected.set(false);
                    onDisconnected();
                    break;
                }
                default:
                    connected.set(false);
            }
        }

        /**
         * 连接断开
         */
        protected void onDisconnected() {
            // Disconnected -- zookeeper library will handle reconnects
            logger.info("Disconnected from Zookeeper,publishing suspended event.");
            eventManager.add(new ConnectionEvent(ConnectionEventType.SUSPENDED, url));
            //尝试重新恢复连接
            taskManager.add(new ConnectionTask(ConnectionTaskType.RECONNECTION));
        }

        /**
         * 会话过期
         */
        protected void onExpired() {
            logger.info("Zookeeper session is expired,publishing lost event.");
            eventManager.add(new ConnectionEvent(ConnectionEventType.LOST, url));
            // Session was expired; create a new zookeeper connection
            taskManager.add(new ConnectionTask(ConnectionTaskType.CONNECTION));
        }

        /**
         * 连接上
         */
        protected void onSyncConnected() {
            //连接上
            if (mailbox.offer(ConnectionState.SUCCESS)) {
                //调用线程没有超时关闭
                //获取SessionId
                long newSessionId = zooKeeper.getSessionId();
                long oldSessionId = zkSessionId.getAndSet(newSessionId);
                if (oldSessionId != newSessionId) {
                    logger.info("SyncConnected to Zookeeper,publishing connected event.");
                    //session超时，sessionId改变了，则发布CONNECTED事件
                    eventManager.add(new ConnectionEvent(ConnectionEventType.CONNECTED, url));
                } else {
                    logger.info("Reconnected to Zookeeper,publishing reconnected event.");
                    //session没有超时，则发布RECONNECTED事件
                    eventManager.add(new ConnectionEvent(ConnectionEventType.RECONNECTED, url));
                }
            }

        }
    }

    /**
     * 连接任务监听器
     */
    protected class TaskListener implements EventListener<ConnectionTask> {
        @Override
        public void onEvent(ConnectionTask event) {
            if (closed.get()) {
                return;
            }
            try {
                if (isConnected()) {
                    // 当前连接上
                    if (event != null) {
                        // 等待1秒钟判断是否连接上
                        Thread.sleep(1000);
                        if (isConnected()) {
                            // 丢弃任务
                            logger.info("zookeeper is connected,discard the event");
                        } else if (!closed.get()) {
                            // 重连
                            logger.info("Attempting to connect to zookeeper servers " + url.getAddress());
                            connectWithRetry(event);
                        }
                    }
                } else if (event == null) {
                    // 当前没有连接上，则添加一个连接任务
                    taskManager.add(new ConnectionTask());
                } else {
                    // 重连
                    logger.info("Attempting to connect to zookeeper servers " + url.getAddress());
                    connectWithRetry(event);
                }
            } catch (InterruptedException ignored) {
                //被中断了
            }
        }

        /**
         * 连接，出现异常并重试
         *
         * @param task 任务
         * @throws InterruptedException
         */
        protected void connectWithRetry(ConnectionTask task) throws InterruptedException {
            if (task == null) {
                return;
            }
            //等待连接上
            long retryCount = 0;
            while (!Thread.currentThread().isInterrupted() && !closed.get()) {
                if (connect(task)) {
                    return;
                }

                //重试次数加1
                retryCount++;
                //超过了指定的最大重试次数，则退出
                if (connectionRetryTimes > 0 && retryCount >= connectionRetryTimes) {
                    eventManager.add(new ConnectionEvent(ConnectionEventType.FAILED, url));
                    break;
                }
                //休息指定的间隔
                Thread.sleep(retryInterval);
                //清除信号量
                mailbox.clear();
            }
        }

        /**
         * 连接
         *
         * @return 是否成功
         */
        protected boolean connect(ConnectionTask task) {
            if (task == null) {
                return false;
            }
            if (closed.get() || isConnected()) {
                return true;
            }

            try {
                //连接
                if (task.getType() == ConnectionTaskType.CONNECTION) {
                    closeZookeeper();
                    logger.info("try to connect to zookeeper " + url.getAddress());
                    zooKeeper = new ZooKeeper(url.getAddress(), sessionTimeout, new ConnectionWatcher());
                }
                //等待连接成功通知
                ConnectionState state = mailbox.poll(sessionTimeout, TimeUnit.MILLISECONDS);
                if (closed.get()) {
                    // 已经关闭了
                    return false;
                }
                if (state == ConnectionState.SUCCESS && isConnected()) {
                    //连接上
                    if(env.equals("prod")|| env.equals("pre"));
                        zooKeeper.addAuthInfo("digest","tmra:tmra".getBytes());
                    return true;
                }
                //超时连接
                if (!mailbox.offer(ConnectionState.TIMEOUT)) {
                    //连接上
                    state = mailbox.poll();
                    if (state == ConnectionState.SUCCESS && isConnected()) {
                        if(env.equals("prod")|| env.equals("pre"));
                            zooKeeper.addAuthInfo("digest","tmra:tmra".getBytes());
                        return true;
                    }
                }
                //判断是否Session失效了
                if (task.getType() == ConnectionTaskType.RECONNECTION) {
                    //session失效，发通知
                    eventManager.add(new ConnectionEvent(ConnectionEventType.LOST, url));
                    //修改类型为CONNECTION
                    task.type = ConnectionTaskType.CONNECTION;
                }
                //关闭Zookeeper的自动连接
                closeZookeeper();
                return false;
            } catch (IOException e) {
                //创建Zookeeper并连接出错
                logger.error("error connected to zookeeper servers," + url.toString(), e);
                closeZookeeper();
                return false;
            } catch (InterruptedException e) {
                //condition被中断了，则关闭Zookeeper直接返回
                closeZookeeper();
                return false;
            }

        }

    }


}