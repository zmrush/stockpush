package push.registry.zookeeper.listener;


import push.registry.listener.ChildrenListener;
import push.registry.listener.LiveListener;
import push.registry.util.PathUtil;
import push.registry.zookeeper.ZKClient;
import push.registry.listener.ChildrenEvent;
import push.registry.listener.ChildrenEvent.ChildrenEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 子节点变化监听器管理
 *
 */
public class ChildrenManager extends ListenerManager<ChildrenListener, ChildrenEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ChildrenManager.class);
    private Set<String> lastChildren = new HashSet<String>();

    public ChildrenManager(ZKClient zkClient, String path) {
        super.initialize(zkClient, path);
    }

    public void addListener(ChildrenListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if (eventManager.addListener(listener) && !lastChildren.isEmpty() && !(listener instanceof LiveListener)) {
                for (String child : lastChildren) {
                    String path = PathUtil.makePath(this.path, child);
                    ChildrenEvent event = new ChildrenEvent(ChildrenEventType.CHILD_CREATED, path, null);
                    eventManager.add(event, listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void onUpdateEvent() throws Exception {
        // 得到当前节点,只包含子节点的名称，不包括全路径
        List<String> childrenList = zkClient.getChildren(path, updateWatcher);
        Set<String> children = new HashSet<String>(childrenList);
        List<String> addedChildren = new ArrayList<String>();
        List<String> removedChildren = new ArrayList<String>();
        lock.writeLock().lock();
        try {
            // 得到新增的节点
            for (String child : children) {
                if (!lastChildren.contains(child)) {
                    //新增的
                    addedChildren.add(child);
                    String path = PathUtil.makePath(this.path, child);
                    eventManager.add(new ChildrenEvent(ChildrenEventType.CHILD_CREATED, path, null));
                }
            }
            // 得到删除的节点
            for (String child : lastChildren) {
                if (!children.contains(child)) {
                    //删除的
                    removedChildren.add(child);
                    String path = PathUtil.makePath(this.path, child);
                    eventManager.add(new ChildrenEvent(ChildrenEventType.CHILD_REMOVED, path, null));
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("removed children:" + removedChildren.toString());
                logger.debug("added children:" + addedChildren.toString());
            }
            lastChildren = children;
        } finally {
            lock.writeLock().unlock();
        }
    }

}