package push.registry.listener;


/**
 * 路径事件
 */
public class PathEvent {

    private PathEventType type;
    private String path;
    private byte[] data;

    public PathEvent(PathEventType type, String path, byte[] data) {
        this.type = type;
        this.path = path;
        this.data = data;
    }

    /**
     * @return 事件类型
     */
    public PathEventType getType() {
        return type;
    }

    /**
     * @return 全路径
     */
    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PathEvent [type=" + type + ", path=" + path + ", data=" + ((data == null || data.length < 1) ? "" :
                new String(
                data)) + "]";
    }

    public static enum PathEventType {
        CREATED, REMOVED, UPDATED
    }

}
