package push.registry;


import java.util.List;

/**
 * 抽象的注册中心
 */
public abstract class AbstractRegistry implements Registry {
    @Override
    public URL getUrl() {
        return null;
    }

    public void setUrl(URL url) {
    }

    @Override
    public void open() throws RegistryException {
    }

    @Override
    public void close() throws RegistryException {
    }

    @Override
    public void create(String path, byte[] data) throws RegistryException {
    }

    @Override
    public void create(List<String> path) throws RegistryException {
    }

    @Override
    public void createLive(String path, byte[] data) {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void deleteLive(String path) {
    }

    @Override
    public void update(String path, byte[] data) throws RegistryException {
    }

    @Override
    public void update(String path, byte[] data, byte[] parent) throws RegistryException {
    }

    @Override
    public void delete(String path) throws RegistryException {
    }

    @Override
    public void delete(List<String> path) throws RegistryException {
    }

    @Override
    public boolean exists(String path) throws RegistryException {
        return false;
    }

    @Override
    public boolean isLeader(String path) throws RegistryException {
        return false;
    }

    @Override
    public PathData getData(String path) throws RegistryException {
        return null;
    }

    @Override
    public List<PathData> getChildData(String path) throws RegistryException {
        return null;
    }

    @Override
    public List<String> getChildren(String path) throws RegistryException {
        return null;
    }

    @Override
    public void addListener(String path, push.registry.listener.ChildrenListener listener) {
    }

    @Override
    public void addListener(String path, push.registry.listener.ChildrenDataListener listener) {
    }

    @Override
    public void addListener(String path, push.registry.listener.PathListener listener) {
    }

    @Override
    public void addListener(String path, push.registry.listener.LeaderListener listener) {
    }

    @Override
    public void addListener(String path, push.registry.listener.ClusterListener listener) {
    }

    @Override
    public void addListener(push.registry.listener.ConnectionListener listener) {
    }

    @Override
    public void removeListener(String path, push.registry.listener.PathListener listener) {
    }

    @Override
    public void removeListener(String path, push.registry.listener.ChildrenListener listener) {
    }

    @Override
    public void removeListener(String path, push.registry.listener.ChildrenDataListener listener) {
    }

    @Override
    public void removeListener(String path, push.registry.listener.LeaderListener listener) {
    }

    @Override
    public void removeListener(String path, push.registry.listener.ClusterListener listener) {
    }

    @Override
    public void removeListener(push.registry.listener.ConnectionListener listener) {
    }

    public String getType() {
        return null;
    }
}
