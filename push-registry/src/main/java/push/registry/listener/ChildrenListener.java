package push.registry.listener;


import push.registry.EventListener;

/**
 * 孩子监听器，不感知数据变化
 */
public interface ChildrenListener extends EventListener<ChildrenEvent> {
	
}
