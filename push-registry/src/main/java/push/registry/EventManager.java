package push.registry;

import org.w3c.dom.events.EventException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 事件管理器
 *
 */
public class EventManager<E> {
    protected CopyOnWriteArrayList<EventListener<E>> listeners = new CopyOnWriteArrayList<EventListener<E>>();
    protected BlockingQueue<Ownership> events;
    protected String name;
    // 事件派发线程
    protected Thread dispatcher;
    // 事件派发处理器
    protected EventDispatcher eventDispatcher = new EventDispatcher();
    // 启动标示
    protected AtomicBoolean started = new AtomicBoolean(false);
    // 没有事件的也触发监听器
    protected boolean triggerNoEvent;
    // 事件的间隔（毫秒），并合并事件
    protected long interval;
    // 空闲时间
    protected long idleTime;
    // 获取事件超时时间
    protected long timeout = 1000;

    public EventManager() {
        this(null, 0);
    }

    public EventManager(EventListener<E> listener) {
        this(null, listener, 0);
    }

    public EventManager(String name) {
        this(name, 0);
    }

    public EventManager(String name, int capacity) {
        this.name = name;
        if (capacity > 0) {
            events = new ArrayBlockingQueue<Ownership>(capacity);
        } else {
            events = new LinkedBlockingDeque<Ownership>();
        }
    }

    public EventManager(String name, EventListener<E> listener) {
        this(name, 0);
        addListener(listener);
    }

    public EventManager(String name, EventListener<E> listener, int capacity) {
        this(name, capacity);
        addListener(listener);
    }

    public EventManager(String name, List<? extends EventListener<E>> listeners) {
        this(name, 0);
        if (listeners != null) {
            for (EventListener<E> listener : listeners) {
                addListener(listener);
            }
        }
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
    }

    public boolean isTriggerNoEvent() {
        return triggerNoEvent;
    }

    public void setTriggerNoEvent(boolean triggerNoEvent) {
        this.triggerNoEvent = triggerNoEvent;
    }

    /**
     * 开始
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            // 清理一下，防止里面有数据
            events.clear();
            eventDispatcher.start();
            if (name != null) {
                dispatcher = new Thread(eventDispatcher, name);
            } else {
                dispatcher = new Thread(eventDispatcher);
            }
            dispatcher.setDaemon(true);
            dispatcher.start();
        }
    }

    /**
     * 结束
     */
    public void stop() {
        stop(false);
    }

    /**
     * 结束
     *
     * @param gracefully 优雅停止
     */
    public void stop(boolean gracefully) {
        if (started.compareAndSet(true, false)) {
            if (dispatcher != null) {
                dispatcher.interrupt();
                dispatcher = null;
                eventDispatcher.stop(gracefully);
                events.clear();
            }
        }
    }

    /**
     * 是否启动
     *
     * @return 启动标示
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * 增加监听器
     *
     * @param listener
     */
    public boolean addListener(EventListener<E> listener) {
        if (listener != null) {
            return listeners.addIfAbsent(listener);
        }
        return false;
    }

    /**
     * 删除监听器
     *
     * @param listener
     */
    public boolean removeListener(EventListener<E> listener) {
        if (listener != null) {
            return listeners.remove(listener);
        }
        return false;
    }

    public List<EventListener<E>> getListeners() {
        return listeners;
    }


    /**
     * 异步发布事件
     *
     * @param event 事件
     */
    public boolean add(final E event) {
        return add(event, null);
    }

    /**
     * 异步发布事件
     *
     * @param event   事件
     * @param timeout 超时
     * @param unit    时间单位
     */
    public boolean add(final E event, final long timeout, final TimeUnit unit) {
        return add(event, null, timeout, unit);
    }

    /**
     * 异步发布发布事件
     *
     * @param event 事件
     * @param owner 所有者
     */
    public boolean add(final E event, EventListener<E> owner) {
        if (event == null) {
            return false;
        }
        try {
            events.put(new Ownership(event, owner));
            return true;
        } catch (InterruptedException e) {
            // 让当前线程中断
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 异步发布发布事件
     *
     * @param event   事件
     * @param owner   所有者
     * @param timeout 超时
     * @param unit    时间单位
     */
    public boolean add(final E event, EventListener<E> owner, final long timeout, final TimeUnit unit) {
        if (event == null) {
            return false;
        }
        try {
            return events.offer(new Ownership(event, owner), timeout, unit);
        } catch (InterruptedException e) {
            // 让当前线程中断
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 同步通知事件
     *
     * @param event 事件
     */
    public void inform(E event) throws Exception {
        if (event == null) {
            return;
        }
        for (EventListener<E> listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (EventException e) {
                throw (Exception) e.getCause();
            }
        }
    }

    /**
     * 空闲事件
     */
    protected void onIdle() {

    }

    /**
     * 合并事件
     *
     * @param events 事件列表
     */
    protected void publish(List<Ownership> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        // 发布最后一个事件
        publish(events.get(events.size() - 1));
    }

    /**
     * 派发消息
     *
     * @param event 事件
     */
    protected void publish(Ownership event) {
        // 当triggerNoEvent为真时候，event可以为空
        if (event != null && event.owner != null) {
            try {
                event.owner.onEvent(event.event);
            } catch (Throwable ignored) {
            }
        } else {
            E e = event == null ? null : event.event;
            for (EventListener<E> listener : listeners) {
                try {
                    listener.onEvent(e);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 事件
     */
    protected class Ownership {
        private E event;
        private EventListener<E> owner;

        public Ownership(E event) {
            this.event = event;
        }

        public Ownership(E event, EventListener<E> owner) {
            this.event = event;
            this.owner = owner;
        }

        public E getEvent() {
            return event;
        }

        public EventListener<E> getOwner() {
            return owner;
        }
    }

    /**
     * 事件派发
     */
    protected class EventDispatcher implements Runnable {

        private AtomicBoolean started = new AtomicBoolean();
        private AtomicBoolean gracefully = new AtomicBoolean(false);
        private CountDownLatch latch;

        public void start() {
            if (started.compareAndSet(false, true)) {
                latch = new CountDownLatch(1);
                gracefully.set(false);
            }
        }

        public void stop(boolean gracefully) {
            if (started.compareAndSet(true, false)) {
                this.gracefully.set(gracefully);
                if (gracefully) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        // 让当前线程中断
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        /**
         * 线程是否存活
         *
         * @return 存活标示
         */
        protected boolean isAlive() {
            return started.get() && !Thread.currentThread().isInterrupted();
        }

        @Override
        public void run() {
            long lastTime = SystemClock.getInstance().now();
            long now;
            Ownership event;
            while (true) {
                try {
                    event = null;
                    // 判断是否关闭
                    if (isAlive()) {
                        // 没有关闭，则获取数据
                        event = events.poll(timeout, TimeUnit.MILLISECONDS);
                    }
                    // 再次判断是否关闭
                    if (!isAlive()) {
                        if (!gracefully.get()) {
                            // 非优雅关闭
                            break;
                        }
                        // 优雅关闭，如果当前事件为空，则重新取一次
                        if (event == null) {
                            if (!Thread.currentThread().isInterrupted()) {
                                event = events.poll(50, TimeUnit.MILLISECONDS);
                            } else {
                                event = events.poll();
                            }
                        }
                        if (event == null) {
                            // 优雅退出
                            break;
                        }
                    }
                    // 当前事件不为空
                    if (event != null) {
                        if (idleTime > 0) {
                            // 启用空闲检测
                            lastTime = SystemClock.getInstance().now();
                        }

                        // 合并事件
                        if (interval > 0) {
                            // 获取当前所有事件
                            List<Ownership> currents = new ArrayList<Ownership>();
                            currents.add(event);
                            while (!events.isEmpty()) {
                                event = events.poll();
                                if (event != null) {
                                    currents.add(event);
                                }
                            }
                            // 合并事件
                            publish(currents);
                        } else {
                            publish(event);
                        }
                    } else {
                        if (triggerNoEvent) {
                            // 出发空事件
                            publish((Ownership) null);
                        }
                        if (idleTime > 0) {
                            // 启用空闲检测
                            now = SystemClock.getInstance().now();
                            if (now - lastTime > idleTime) {
                                lastTime = now;
                                onIdle();
                            }
                        }
                    }
                    if (interval > 0 && isAlive()) {
                        // 休息间隔时间
                        Thread.sleep(interval);
                    }
                } catch (InterruptedException e) {
                    // 让当前线程中断
                    Thread.currentThread().interrupt();
                }
            }
            if (latch != null) {
                latch.countDown();
            }

        }
    }

}


