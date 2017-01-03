package push.listener;


import push.listener.ConnectionEvent;
import push.listener.ConnectionListener;
import push.listener.LiveListener;
import push.ZKClient;
import push.EventListener;
import push.EventManager;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 监听器管理
 *
 */
public abstract class ListenerManager<L extends EventListener<E>, E> {
    private static final Logger logger = LoggerFactory.getLogger(ListenerManager.class);
    //Zookeeper客户端
    protected ZKClient zkClient;
    //路径
    protected String path;
    //事件监听器
    protected EventManager<E> eventManager;
    //更新事件监听器
    protected EventManager<UpdateType> updateManager;
    //是否关闭
    protected volatile boolean closed = false;
    //连接监听器
    protected ConnectionWatcher connectionWatcher = new ConnectionWatcher();
    //读写锁
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    //更新监听器
    protected UpdateWatcher updateWatcher = new UpdateWatcher();
    //计算器
    protected AtomicLong updateCounter = new AtomicLong(0);

    protected void initialize(final ZKClient zkClient, String path) {
        if (zkClient == null) {
            throw new IllegalArgumentException("zkClient is null");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path is null");
        }
        this.zkClient = zkClient;
        this.path = path;

        eventManager = new EventManager<E>() {

            @Override
            protected void publish(Ownership event) {
                // 过滤掉存活的初始化事件
                if (event.getOwner() != null && (event.getOwner() instanceof LiveListener) && updateCounter
                        .get() <= 1) {
                    return;
                }
                super.publish(event);
            }

            @Override
            protected void onIdle() {
                // 空闲事件，则重新更新一下数据，防止zookeeper丢失事件
                if (zkClient.isConnected()) {
                    updateManager.add(UpdateType.UPDATE);
                }
                super.onIdle();
            }
        };
        eventManager.start();


        updateManager = new EventManager<UpdateType>();
        updateManager.addListener(new UpdateListener());
        // 随机时间间隔，减少同时访问zookeeper的压力
        updateManager.setInterval(1000 + (long) (1000 * Math.random()));
        // 随机空闲时间(1分钟-5分钟),触发空闲事件更新数据，防止zookeeper丢失事件
        updateManager.setIdleTime(60 * 1000 + (long) (5 * 60 * 1000 * Math.random()));
        updateManager.start();

        // 注册connection listener
        this.zkClient.addListener(connectionWatcher);
    }

    /**
     * 移除监听器
     *
     * @param listener 监听器
     */
    public void removeListener(L listener) {
        eventManager.removeListener(listener);
    }

    /**
     * 关闭
     */
    public void close() {
        closed = true;
        zkClient.removeListener(connectionWatcher);
        eventManager.stop();
        updateManager.stop();
        updateCounter.set(0);
    }

    /**
     * 连接事件处理
     */
    protected void onConnectedEvent() {
        //触发数据更新事件
        updateManager.add(UpdateType.UPDATE);
    }

    /**
     * 重新连接上，Session没有失效事件处理
     */
    protected void onReconnectedEvent() {

    }

    /**
     * 连接断开事件处理，Session没有失效
     */
    protected void onSuspendedEvent() {

    }

    /**
     * 连接断开事件处理，Session失效
     */
    protected void onLostEvent() {

    }

    /**
     * 数据更新事件
     *
     * @throws Exception
     */
    protected void onUpdateEvent() throws Exception {

    }

    /**
     * 数据更新类型
     */
    public static enum UpdateType {
        UPDATE
    }

    /**
     * 连接监听器
     */
    protected class ConnectionWatcher implements ConnectionListener {
        @Override
        public void onEvent(ConnectionEvent event) {
            if (event.getType() == ConnectionEvent.ConnectionEventType.CONNECTED) {
                onConnectedEvent();
            } else if (event.getType() == ConnectionEvent.ConnectionEventType.LOST) {
                onLostEvent();
            } else if (event.getType() == ConnectionEvent.ConnectionEventType.RECONNECTED) {
                onReconnectedEvent();
            } else if (event.getType() == ConnectionEvent.ConnectionEventType.SUSPENDED) {
                onSuspendedEvent();
            }

        }
    }

    /**
     * 数据更新事件派发器
     */
    protected class UpdateListener implements EventListener<UpdateType> {

        @Override
        public void onEvent(UpdateType event) {
            try {
                //转换成通知事件
                if (!closed && zkClient.isConnected()) {
                    updateCounter.incrementAndGet();
                    onUpdateEvent();
                }
            } catch (KeeperException.ConnectionLossException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.MarshallingErrorException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.UnimplementedException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.BadArgumentsException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.APIErrorException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.NoAuthException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.BadVersionException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.AuthFailedException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.InvalidACLException e) {
                logger.error("error occurs when update data ", e);
            } catch (KeeperException.SessionExpiredException e) {
                logger.error("error occurs when update data ", e);
            } catch (InterruptedException ignored) {
                //中断
            } catch (Throwable e) {
                // 必须捕获Throwable，否则调度会终止
                updateManager.add(UpdateType.UPDATE);
                logger.warn("error occurs when update data ", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * 路径监听器
     */
    protected class UpdateWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            updateManager.add(UpdateType.UPDATE);
        }
    }
}