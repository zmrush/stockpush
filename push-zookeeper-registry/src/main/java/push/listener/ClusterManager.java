package push.listener;


import com.creditease.toumi.dte.registry.PathData;
import com.creditease.toumi.dte.registry.listener.ClusterEvent;
import com.creditease.toumi.dte.registry.listener.ClusterListener;
import com.creditease.toumi.dte.registry.util.PathUtil;
import com.creditease.toumi.dte.registry.zookeeper.ZKClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 集群管理器，Observer节点不参与选举，只接收选举结果
 */
public class ClusterManager extends AbstractLeaderManager<ClusterListener, ClusterEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManager.class);
    private ClusterListener myListener;
    // 以前的数据节点
    private Map<String, PathData> lastChildren = new HashMap<String, PathData>();

    public ClusterManager(ZKClient zkClient, String path, ClusterListener myListener) {
        super.initialize(zkClient, path);
        this.myListener = myListener;
    }

    public void addListener(ClusterListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if (eventManager.addListener(listener) && !closed) {
                if (listener instanceof Observer) {
                    observer = true;
                }
                if (!observer && myName.get() == null) {
                    // 初始化创建Leader选举的临时节点
                    updateManager.add(UpdateType.UPDATE);
                } else {
                    // 广播事件给该监听器进行初始化
                    ClusterEvent event = new ClusterEvent(path, zkClient.getSortedChildData(path));
                    eventManager.add(event, listener);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void createElectionNode() throws Exception {
        //增加IP地址，便于在Zookeeper中查看是那台机器创建的节点
        byte[] data = null;
        try {
            String nodeName = getNodeName();
            if (nodeName != null) {
                data = nodeName.getBytes();
            }
        } catch (Exception ignored) {
        }

        // 先查找是否有原来遗留下来的选举节点
        try {
            List<PathData> children = zkClient.getChildData(path);
            for (PathData child : children) {
                if (Arrays.equals(data, child.getData())) {
                    zkClient.delete(PathUtil.makePath(path, child.getPath()));
                    break;
                }
            }
        } catch (Exception ignored) {
            // 可以忽略异常
        }

        //创建有序临时节点
        String childFullPath =
                zkClient.create(PathUtil.makePath(path, "member-"), data, CreateMode.EPHEMERAL_SEQUENTIAL);
        //得到节点名称
        String childName = childFullPath.substring(path.length() + 1);
        myName.set(childName);
        if (logger.isInfoEnabled()) {
            logger.info("added EPHEMERAL_SEQUENTIAL path" + childFullPath);
        }
    }

    @Override
    protected void doUpdateEvent() throws Exception {
        //得到当前节点数据，并注册Watcher
        List<PathData> children = zkClient.getSortedChildData(path, updateWatcher);

        // 比较数据是否发生变更
        Map<String, PathData> current = new HashMap<String, PathData>();
        Map<String, PathData> last = lastChildren;
        boolean changed = false;
        PathData old;

        // 遍历当前数据，判断是否发生变更
        for (PathData child : children) {
            current.put(child.getPath(), child);
            old = last.get(child.getPath());
            if (old == null || !Arrays.equals(old.getData(), child.getData())) {
                changed = true;
            }
        }
        if (!changed && last.size() != current.size()) {
            changed = true;
        }

        if (changed) {
            if (children.size() >= 1) {
                String leaderName = children.get(0).getPath();
                if (!leader.get() && leaderName.equals(myName.get())) {
                    // 以前不是leader，现在是leader
                    leader.set(true);
                }
            }

            lastChildren = current;
            eventManager.add(new ClusterEvent(path, children));
        }
    }

    @Override
    protected String getNodeName() throws Exception {
        String name = myListener.getNodeName();
        if (name == null || name.isEmpty()) {
            name = super.getNodeName();
        }
        return name;
    }

    @Override
    protected void onLostEvent() {
        lock.writeLock().lock();
        try {
            if (leader.compareAndSet(true, false)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("lost leader." + PathUtil.makePath(path, myName.get()));
                }
            }
            // 丢失连接，清理缓存的节点数据
            lastChildren = new HashMap<String, PathData>();
            eventManager.add(new ClusterEvent(path, new ArrayList<PathData>()));
        } finally {
            lock.writeLock().unlock();
        }
    }
}
