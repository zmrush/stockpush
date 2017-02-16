package push.registry;


import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 节点监听器基类，缓存节点及其数据
 *
 */
public abstract class PathCache<L extends EventListener<E>, E> {

    protected Registry registry;
    protected boolean closed = false;
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    //广播事件
    protected EventManager<E> eventManager = new EventManager<E>();
    //监听的节点
    protected String path;

    protected void setup() {
        eventManager.start();
    }

    /**
     * 关闭
     */
    public void close() {
        lock.writeLock().lock();
        try {
            closed = true;
            eventManager.stop();
            doClose();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void doClose() {

    }

    /**
     * 增加监听器
     *
     * @param listener 监听器
     */
    public void addListener(L listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if(eventManager.addListener(listener)&&!closed){
                onAddListener(listener);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 广播初始数据
     *
     * @param listener 监听器
     */
    protected void onAddListener(L listener) {

    }

    /**
     * 删除监听器
     *
     * @param listener 监听器
     */
    public void removeListener(L listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            eventManager.removeListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 子节点增加
     *
     * @param node 子节点名称
     * @param data 数据
     * @throws Exception
     */
    protected abstract void onChildCreated(String node, byte[] data) throws Exception;

    /**
     * 子节点删除
     *
     * @param node 子节点名称
     * @param data 数据
     * @throws Exception
     */
    protected abstract void onChildRemoved(String node, byte[] data) throws Exception;

    /**
     * 子节点修改
     *
     * @param node 子节点名称
     * @param data 数据
     * @throws Exception
     */
    protected void onChildUpdated(String node, byte[] data) throws Exception {

    }

    /**
     * 出现异常
     *
     * @param e    异常
     * @param path 路径
     */
    protected void onException(Throwable e, String path) {

    }

}
