package push.listener;

import push.PathData;
import push.listener.ChildrenDataListener;
import push.listener.ChildrenEvent;
import push.listener.ChildrenEvent.ChildrenEventType;
import push.util.PathUtil;
import push.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 子节点变化及数据修改管理
 *
 */
public class ChildrenDataManager extends ListenerManager<ChildrenDataListener, ChildrenEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ChildrenDataManager.class);
    private Map<String, PathData> lastChildren = new HashMap<String, PathData>();

    public ChildrenDataManager(ZKClient zkClient, String path) {
        super.initialize(zkClient, path);
    }

    public void addListener(ChildrenDataListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if (eventManager.addListener(listener) && !lastChildren.isEmpty()) {
                for (PathData data : lastChildren.values()) {
                    String path = PathUtil.makePath(this.path, data.getPath());
                    ChildrenEvent event = new ChildrenEvent(ChildrenEventType.CHILD_CREATED, path, data.getData());
                    eventManager.add(event, listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void onUpdateEvent() throws Exception {
        // 得到当前节点数据
        zkClient.getData(path, updateWatcher);
        // 得到子节点数据
        List<String> childrenList = zkClient.getChildren(path, updateWatcher);
        Set<String> children = new HashSet<String>(childrenList);
        List<String> addedChildren = new ArrayList<String>();
        List<String> removedChildren = new ArrayList<String>();
        List<String> updatedChildren = new ArrayList<String>();
        Map<String, PathData> result = new HashMap<String, PathData>();
        PathData lastData;
        PathData currentData;

        lock.writeLock().lock();
        try {
            // 遍历当前节点
            for (String child : children) {
                lastData = lastChildren.get(child);
                String path = PathUtil.makePath(this.path, child);
                currentData = zkClient.getData(path);
                if (lastData == null) {
                    //以前不存在，新增的
                    addedChildren.add(child);
                    result.put(child, currentData);
                    eventManager.add(new ChildrenEvent(ChildrenEventType.CHILD_CREATED, path, currentData.getData()));
                } else {
                    //现在存在，比较是否修改
                    if (!Arrays.equals(lastData.getData(), currentData.getData())) {
                        //数据变化了
                        updatedChildren.add(child);
                        result.put(child, currentData);
                        eventManager
                                .add(new ChildrenEvent(ChildrenEventType.CHILD_UPDATED, path, currentData.getData()));
                    } else {
                        result.put(child, lastData);
                    }

                }
            }
            // 得到删除的节点
            for (Map.Entry<String, PathData> entry : lastChildren.entrySet()) {
                if (!children.contains(entry.getKey())) {
                    //删除的
                    removedChildren.add(entry.getKey());
                    String path = PathUtil.makePath(this.path, entry.getKey());
                    eventManager
                            .add(new ChildrenEvent(ChildrenEventType.CHILD_REMOVED, path, entry.getValue().getData()));
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("removed children:" + removedChildren.toString());
                logger.debug("added children:" + addedChildren.toString());
                logger.debug("updated children:" + updatedChildren.toString());
            }

            lastChildren = result;

        } finally {
            lock.writeLock().unlock();
        }

    }

}