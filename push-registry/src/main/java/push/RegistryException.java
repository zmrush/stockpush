package push;

/**
 * 注册中心异常类
 * Created by Eric on 2015/8/1.
 */
public class RegistryException extends Exception {
    public RegistryException() {
        super();
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(Throwable cause) {
        super(cause);
    }

    public static class ConnectionLossException extends RegistryException {
        public ConnectionLossException() {
            super("CONNECTIONLOSS");
        }
    }

    public static class OperationTimeoutException extends RegistryException {
        public OperationTimeoutException() {
            super("OPERATIONTIMEOUT");
        }
    }

    public static class SessionExpiredException extends RegistryException {
        public SessionExpiredException() {
            super("SESSIONEXPIRED");
        }
    }

    public static class RegistryOpenFailedException extends RegistryException {
        public RegistryOpenFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public RegistryOpenFailedException(String message) {
            super(message);
        }
    }
}
