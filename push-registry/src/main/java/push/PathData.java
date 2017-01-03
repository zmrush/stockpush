package push;

/**
 * 数据节点
 */
public class PathData {

    /**
     * 路径
     */
    private String path;
    /**
     * 数据
     */
    private byte[] data;

    public PathData(String path, byte[] data) {
        this.path = path;
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PathData pathData = (PathData) o;
        if (path != null ? !path.equals(pathData.path) : pathData.path != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}
