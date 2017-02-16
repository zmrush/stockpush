package push.registry.zookeeper;

import push.registry.NetUtil;
import push.registry.URL;
import push.registry.listener.ChildrenEvent;
import push.registry.listener.ChildrenListener;
import push.registry.util.PathUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mingzhu7 on 2017/1/4.
 */
public class ZookeeperTest {
    public static void main(String[] args) throws Exception {
        String ip = NetUtil.getLocalIP();
        if (ip == null) {
            throw new Exception("Fail to get ip of the server!");
        }
        String nodeName = ip + "_" + "8912";
        ZKRegistry zkRegistry = new ZKRegistry(URL.valueOf("zookeeper://(10.100.138.12:2181,10.100.138.235:2181,10.100.138.236:2181)"));
        zkRegistry.open();
        zkRegistry.createLive(PathUtil.makePath("/push/test", "live", nodeName), null);
        zkRegistry.addListener(PathUtil.makePath("/push/test", "live"),new ExecutorLiveListener());
        Thread.currentThread().sleep(1000000);
    }
    private static ReentrantLock mutex=new ReentrantLock();
    private static Set<String> lives=new HashSet<String>();
    public static class ExecutorLiveListener implements ChildrenListener {
        @Override
        public void onEvent(ChildrenEvent event) {
            // 当新添加执行器、执行器宕机时通知 任务分配
            mutex.lock();
            try {
                String node = PathUtil.getNodeFromPath(event.getPath());
                if (event.getType() == ChildrenEvent.ChildrenEventType.CHILD_REMOVED) {
                    lives.remove(node);
                } else if (event.getType() == ChildrenEvent.ChildrenEventType.CHILD_CREATED) {
                     lives.add(node);
                }
            } finally {
                mutex.unlock();
            }
        }
    }
}
