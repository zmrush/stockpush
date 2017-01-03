package push.listener;


import com.creditease.toumi.dte.registry.listener.LeaderEvent;
import com.creditease.toumi.dte.registry.listener.LeaderEvent.LeaderEventType;
import com.creditease.toumi.dte.registry.listener.LeaderListener;
import com.creditease.toumi.dte.registry.util.PathUtil;
import com.creditease.toumi.dte.registry.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Leader管理
 *
 */
public class LeaderManager extends AbstractLeaderManager<LeaderListener, LeaderEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LeaderManager.class);

    public LeaderManager(ZKClient zkClient, String path) {
        super.initialize(zkClient, path);
    }

    @Override
    protected void onLostEvent() {
        lock.writeLock().lock();
        try {
            if (leader.compareAndSet(true, false)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("lost leader." + PathUtil.makePath(path, myName.get()));
                }
                eventManager.add(new LeaderEvent(LeaderEventType.LOST, path));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    public void addListener(LeaderListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if (eventManager.addListener(listener) && !closed) {
                if (myName.get() == null) {
                    //初始化创建Leader选举的临时节点
                    updateManager.add(UpdateType.UPDATE);
                } else if (leader.get()) {
                    //当前已经是Leader，广播Take事件给该监听器进行初始化
                    LeaderEvent event = new LeaderEvent(LeaderEventType.TAKE, path);
                    eventManager.add(event, listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    protected void doUpdateEvent() throws Exception {
        //得到当前节点数据，并注册Watcher
        List<String> children = zkClient.getChildren(path, updateWatcher);
        Collections.sort(children);
        if (children.isEmpty()) {
            eventManager.add(new LeaderEvent(LeaderEventType.LOST, path));
        } else {
            String leaderName = children.get(0);
            if (leader.get() && !leaderName.equals(myName.get())) {
                // 以前是leader，现在不是leader
                eventManager.add(new LeaderEvent(LeaderEventType.LOST, path));
                leader.set(false);
                if (logger.isDebugEnabled()) {
                    logger.debug("lost leader." + PathUtil.makePath(path, leaderName));
                }
            } else if (!leader.get() && leaderName.equals(myName.get())) {
                // 以前不是leader，现在是leader
                eventManager.add(new LeaderEvent(LeaderEventType.TAKE, path));
                leader.set(true);
                if (logger.isDebugEnabled()) {
                    logger.debug("take leader." + PathUtil.makePath(path, leaderName));
                }
            }
        }
    }

}