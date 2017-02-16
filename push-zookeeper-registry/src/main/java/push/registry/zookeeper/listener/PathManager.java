package push.registry.zookeeper.listener;


import push.registry.listener.ListenAllUpdate;
import push.registry.listener.PathEvent;
import push.registry.listener.PathListener;
import push.registry.EventListener;
import push.registry.zookeeper.ZKClient;

import java.util.Arrays;

/**
 * 节点监听器
 */
public class PathManager extends ListenerManager<PathListener, PathEvent> {
    private byte[] lastData;

    public PathManager(ZKClient zkClient, String path) {
        super.initialize(zkClient, path);
    }

    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    public void addListener(PathListener listener) {
        eventManager.addListener(listener);
    }

    @Override
    protected void onUpdateEvent() throws Exception {
        byte[] data = zkClient.getData(path, updateWatcher).getData();
        if (!Arrays.equals(data, lastData)) {
            eventManager.add(new PathEvent(PathEvent.PathEventType.UPDATED, path, data));
        } else {
            for (EventListener<PathEvent> listener : eventManager.getListeners()) {
                if (listener instanceof ListenAllUpdate) {
                    eventManager.add(new PathEvent(PathEvent.PathEventType.UPDATED, path, data), listener);
                }
            }
        }
        lastData = data;
    }

}