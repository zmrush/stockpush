package push.registry.zookeeper;

import org.apache.zookeeper.KeeperException;

import java.util.concurrent.Callable;

/**
 * 重试
 *
 */
public class RetryLoop {
    private boolean isDone = false;
    private int retryTimes;
    private long retryInterval;

    protected RetryLoop(int retryTimes, long retryInterval) {
        this.retryTimes = retryTimes;
        this.retryInterval = retryInterval;
    }

    public static <T> T callWithRetry(ZKClient client, Callable<T> proc) throws Exception {
        if (client == null || proc == null) {
            return null;
        }
        T result = null;
        RetryLoop retryLoop = client.createRetryLoop();
        int retryCount = 0;
        while (retryLoop.shouldContinue()) {
            try {
                result = proc.call();
                retryLoop.markComplete();
            } catch (Exception e) {
                retryLoop.takeException(e, retryCount++);
            }
        }
        return result;
    }

    /**
     * Utility - return true if the given Zookeeper result code is retry-able
     *
     * @param rc result code
     * @return true/false
     */
    protected static boolean shouldRetry(int rc) {
        return (rc == KeeperException.Code.CONNECTIONLOSS.intValue()) || (rc == KeeperException.Code.OPERATIONTIMEOUT
                .intValue()) || (rc == KeeperException.Code.SESSIONMOVED.intValue()) || (rc == KeeperException.Code
                .SESSIONEXPIRED.intValue());
    }

    /**
     * Utility - return true if the given exception is retry-able
     *
     * @param exception exception to check
     * @return true/false
     */
    protected static boolean isRetryException(Throwable exception) {
        if (exception instanceof KeeperException) {
            KeeperException keeperException = (KeeperException) exception;
            return shouldRetry(keeperException.code().intValue());
        }
        return false;
    }

    /**
     * If true is returned, make an attempt at the operation
     *
     * @return true/false
     */
    protected boolean shouldContinue() {
        return !isDone;
    }

    /**
     * Call this when your operation has successfully completed
     */
    protected void markComplete() {
        isDone = true;
    }

    protected void takeException(Exception exception, int retryCount) throws Exception {
        if (!isRetryException(exception) || retryCount >= retryTimes) {
            throw exception;
        }
        Thread.sleep(retryInterval);
    }
}