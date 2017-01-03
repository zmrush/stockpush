package push;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class SystemClock {

    private static final SystemClock instance = new SystemClock();

    private final long precision;
    private final AtomicLong now;
    private ScheduledExecutorService scheduler;

    public static SystemClock getInstance() {
        return instance;
    }

    public SystemClock() {
        this(1L);
    }

    public SystemClock(long precision) {
        this.precision = precision;
        now = new AtomicLong(System.currentTimeMillis());
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "System_Clock");
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduler.scheduleAtFixedRate(new Timer(now), precision, precision, TimeUnit.MILLISECONDS);
    }

    public long now() {
        return now.get();
    }

    public long precision() {
        return precision;
    }

    protected class Timer implements Runnable {
        // 注入进来，避免访问SystemClock.now占用很多CPU
        private final AtomicLong now;

        private Timer(AtomicLong now) {
            this.now = now;
        }

        @Override
        public void run() {
            now.set(System.currentTimeMillis());
        }
    }
}
