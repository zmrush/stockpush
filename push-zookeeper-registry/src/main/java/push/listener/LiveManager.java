package push.listener;


import push.PathData;
import push.listener.ConnectionEvent;
import push.listener.ConnectionListener;
import push.ZKClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * 存活管理
 *
 */
public class LiveManager {
    private static final Logger logger = LoggerFactory.getLogger(LiveManager.class);
    //Zookeeper客户端
    protected ZKClient zkClient;
    protected Set<PathData> lives = new CopyOnWriteArraySet<PathData>();
    //创建存活队列
    protected BlockingQueue<PathData> liveEvents = new LinkedBlockingQueue<PathData>();
    //是否关闭
    protected volatile boolean closed = false;
    //连接监听器
    protected ConnectionWatcher connectionWatcher = new ConnectionWatcher();
    //任务调度
    protected ExecutorService scheduler = Executors.newSingleThreadExecutor();

    public LiveManager(ZKClient zkClient) {
        if (zkClient == null) {
            throw new IllegalArgumentException("zkClient is null");
        }
        this.zkClient = zkClient;
        // 注册connection listener
        this.zkClient.addListener(connectionWatcher);
        this.scheduler.execute(new LiveEventDispatcher());
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            closed = true;
            zkClient.removeListener(connectionWatcher);
            scheduler.shutdownNow();
            lives.clear();
            liveEvents.clear();
        } finally {
            scheduler = null;
        }
    }

    /**
     * 添加存活节点
     *
     * @param data 存活节点
     */
    public void addLive(PathData data) {
        if (data == null) {
            return;
        }
        if (lives.add(data)) {
            liveEvents.offer(data);
        }
    }

    /**
     * 删除存活节点
     *
     * @param data 存活节点
     */
    public void deleteLive(PathData data) {
        if (data == null) {
            return;
        }
        lives.remove(data);
    }

    /**
     * 连接监听器
     */
    protected class ConnectionWatcher implements ConnectionListener {
        @Override
        public void onEvent(ConnectionEvent event) {
            if (event.getType() == ConnectionEvent.ConnectionEventType.CONNECTED) {
                liveEvents.clear();
                for (PathData data : lives) {
                    liveEvents.offer(data);
                }
            } else if (event.getType() == ConnectionEvent.ConnectionEventType.LOST) {
                liveEvents.clear();
            }

        }
    }

    /**
     * 事件广播
     */
    protected class LiveEventDispatcher implements Runnable {
        @Override
        public void run() {

            //永久执行
            while (!Thread.interrupted() && !closed) {
                try {
                    //获取连接事件
                    PathData data = liveEvents.poll(1000, TimeUnit.MILLISECONDS);
                    if (data != null) {
                        //确保Zookeeper连接上，任务没有被删除
                        if (zkClient.isConnected() && lives.contains(data)) {
                            try {
                                zkClient.create(data.getPath(), data.getData(), CreateMode.EPHEMERAL);
                            } catch (Exception e) {
                                liveEvents.offer(data);
                                logger.warn("create live path error." + data.getPath() + ", retry....");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    //被中断了则退出
                    return;
                }
            }

        }
    }
}
