package push.registry;


import push.registry.listener.ChildrenDataListener;
import push.registry.listener.ChildrenEvent;
import push.registry.util.PathUtil;

/**
 * 子节点数据监听器基类，缓存子节点及其数据
 */
public abstract class ChildrenDataCache<L extends EventListener<E>, E> extends PathCache<L, E> {

    protected ChildrenDataListener pathListener = new PathListener();

    @Override
    protected void setup() {
        //增加监听器
        this.registry.addListener(path, pathListener);
        super.setup();
    }

    protected void doClose() {
        registry.removeListener(path, pathListener);

    }

    /**
     * 监听禁用变化
     */
    private class PathListener implements ChildrenDataListener {

        @Override
        public void onEvent(ChildrenEvent event) {
            String node = PathUtil.getNodeFromPath(event.getPath());
            lock.writeLock().lock();
            try {
                if (closed) {
                    return;
                }
                ChildrenEvent.ChildrenEventType type = event.getType();
                if (type == ChildrenEvent.ChildrenEventType.CHILD_CREATED) {
                    onChildCreated(node, event.getData());
                } else if (type == ChildrenEvent.ChildrenEventType.CHILD_REMOVED) {
                    onChildRemoved(node, event.getData());
                } else if (type == ChildrenEvent.ChildrenEventType.CHILD_UPDATED) {
                    onChildUpdated(node, event.getData());
                }
            } catch (Exception e) {
                onException(e, event.getPath());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

}
