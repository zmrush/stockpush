package push.registry.listener;


import push.registry.EventListener;

/**
 * 节点监听器，数据有变更才通知
 */
public interface PathListener extends EventListener<PathEvent> {

}
