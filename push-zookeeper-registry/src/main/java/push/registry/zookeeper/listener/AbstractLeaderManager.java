package push.registry.zookeeper.listener;


import push.registry.Charsets;
import push.registry.EventListener;
import push.registry.NetUtil;
import push.registry.util.PathUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抽象的Leader监听管理器
 */
public abstract class AbstractLeaderManager<L extends EventListener<E>, E> extends ListenerManager<L, E> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLeaderManager.class);
    protected AtomicBoolean leader = new AtomicBoolean(false);
    protected AtomicReference<String> myName = new AtomicReference<String>();
    protected boolean observer;

    @Override
    protected void onReconnectedEvent() {
        onConnectedEvent();
    }

    @Override
    protected void onSuspendedEvent() {
        onLostEvent();
    }

    @Override
    protected void onUpdateEvent() throws Exception {
        // first time create ephemeral node
        lock.writeLock().lock();
        try {
            //没有业务逻辑关注，所以不需要创建临时节点
            if (eventManager.getListeners().isEmpty()) {
                //如果原来已经注册过了则删除掉
                if (myName.get() != null) {
                    leader.set(false);
                    deleteElectionNode();
                }
                return;
            }
            // 判断是否是观察者模式
            if (!observer) {
                // 参与投票，判断是否创建了选举节点
                if (myName.get() == null) {
                    // 创建Leader选举的临时节点
                    createElectionNode();
                } else {
                    // 获取当前节点信息
                    Stat stat = new Stat();
                    String leaderPath = PathUtil.makePath(this.path, myName.get());
                    zkClient.getData(leaderPath, stat);
                    if (stat.getCtime() == 0) {
                        //不存在,创建Leader选举的临时节点
                        createElectionNode();
                    } else if (stat.getEphemeralOwner() != zkClient.getSessionId()) {
                        //Session变化了
                        zkClient.delete(leaderPath);
                        createElectionNode();
                    }
                }
            }
            doUpdateEvent();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected abstract void doUpdateEvent() throws Exception;

    public abstract void addListener(L listener);

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            try {
                //删除掉Leader节点
                deleteElectionNode();
            } catch (Exception e) {
            }
            myName.set(null);
            leader.set(false);
            super.close();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeListener(L listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            eventManager.removeListener(listener);
            if (myName.get() != null) {
                updateManager.add(UpdateType.UPDATE);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 是否是Leader
     *
     * @return 是否是Leader
     */
    public boolean isLeader() {
        lock.readLock().lock();
        try {
            return !closed && leader.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 删除选举节点
     *
     * @throws Exception
     */
    protected void deleteElectionNode() throws Exception {
        if (myName.get() == null) {
            return;
        }
        String childFullPath = PathUtil.makePath(path, myName.get());
        if (zkClient.exists(childFullPath)) {
            zkClient.delete(childFullPath);
        }
        myName.set(null);
        if (logger.isInfoEnabled()) {
            logger.info("delete EPHEMERAL_SEQUENTIAL path" + childFullPath);
        }
    }

    protected String getNodeName() throws Exception {
        return NetUtil.getLocalIP();
    }

    /**
     * 创建选举节点
     *
     * @throws Exception
     */
    protected void createElectionNode() throws Exception {
        //增加IP地址，便于在Zookeeper中查看是那台机器创建的节点
        byte[] data = null;
        try {
            String nodeName = getNodeName();
            if (nodeName != null) {
                data = nodeName.getBytes(Charsets.UTF8);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        //创建有序临时节点
        String childFullPath =
                zkClient.create(PathUtil.makePath(path, "member-"), data, CreateMode.EPHEMERAL_SEQUENTIAL);
        //得到节点名称
        String childName = childFullPath.substring(path.length() + 1);
        myName.set(childName);
        if (logger.isInfoEnabled()){
            logger.info("added EPHEMERAL_SEQUENTIAL path" + childFullPath);
        }
    }

}
