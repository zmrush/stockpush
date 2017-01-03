package push;

public interface EventListener<E> {
    /**
     * 事件
     *
     * @param event
     */
    void onEvent(E event);
}
