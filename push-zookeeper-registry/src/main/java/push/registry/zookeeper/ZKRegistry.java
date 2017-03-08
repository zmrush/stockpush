package push.registry.zookeeper;

import org.apache.zookeeper.CreateMode;
import push.registry.PathData;
import push.registry.listener.*;
import push.registry.Registry;
import push.registry.RegistryException;
import push.registry.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Zookeeper注册中心
 *
 */
public class ZKRegistry implements Registry {
    protected Map<String, push.registry.zookeeper.listener.AbstractLeaderManager> leaderManagers = new HashMap<String, push.registry.zookeeper.listener.AbstractLeaderManager>();
    protected Map<String, push.registry.zookeeper.listener.PathManager> pathManagers = new HashMap<String, push.registry.zookeeper.listener.PathManager>();
    protected Map<String, push.registry.zookeeper.listener.ChildrenManager> childrenManagers = new HashMap<String, push.registry.zookeeper.listener.ChildrenManager>();
    protected Map<String, push.registry.zookeeper.listener.ChildrenDataManager> childrenDataManagers = new HashMap<String, push.registry.zookeeper.listener.ChildrenDataManager>();
    protected List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<ConnectionListener>();
    protected push.registry.zookeeper.listener.LiveManager liveManager;
    protected ZKClient zkClient;
    protected URL url;
    protected boolean opened;
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ZKRegistry() {
    }

    public ZKRegistry(URL url) {
        this.url = url;
    }

    public String getType() {
        return "zookeeper";
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void open() throws RegistryException {
        if (opened) {
            return;
        }
        if (url == null) {
            url = URL.valueOf("zookeeper://arch-yz.zookeeper.360buy.com:2181");
        } else if (url.getHost() == null || url.getHost().isEmpty()) {
            url = url.setHost("arch-yz.zookeeper.360buy.com").setPort(2181);
        }
        lock.writeLock().lock();
        try {
            if (opened) {
                return;
            }
            zkClient = new ZKClient(url, connectionListeners);
            liveManager = new push.registry.zookeeper.listener.LiveManager(zkClient);
            opened = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isConnected() {
        if (zkClient == null) {
            return false;
        }
        return zkClient.isConnected();
    }

    @Override
    public void close() throws RegistryException {
        lock.writeLock().lock();
        try {
            for (push.registry.zookeeper.listener.AbstractLeaderManager leaderManager : leaderManagers.values()) {
                leaderManager.close();
            }
            for (push.registry.zookeeper.listener.PathManager pathManager : pathManagers.values()) {
                pathManager.close();
            }
            for (push.registry.zookeeper.listener.ChildrenManager childrenManager : childrenManagers.values()) {
                childrenManager.close();
            }
            for (push.registry.zookeeper.listener.ChildrenDataManager childrenDataManager : childrenDataManagers.values()) {
                childrenDataManager.close();
            }
            leaderManagers.clear();
            pathManagers.clear();
            childrenManagers.clear();
            childrenDataManagers.clear();
            opened = false;
            if (liveManager != null) {
                liveManager.close();
            }
            if (zkClient != null) {
                zkClient.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void create(String path, byte[] data) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            zkClient.create(path, data, CreateMode.PERSISTENT);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("create failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void create(List<String> paths) throws RegistryException {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            zkClient.create(paths);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("create failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void createLive(String path, byte[] data) {
        if (path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            liveManager.addLive(new PathData(path, data));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteLive(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            liveManager.deleteLive(new PathData(path, null));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void update(String path, byte[] data) throws RegistryException {
        this.update(path, data, null);
    }

    @Override
    public void update(String path, byte[] data, byte[] parent) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            zkClient.update(path, data, parent);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("update failed", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void delete(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            zkClient.delete(path);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("delete failed", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void delete(List<String> paths) throws RegistryException {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            zkClient.delete(paths);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("delete failed", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public boolean exists(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return false;
        }
        lock.readLock().lock();
        try {
            checkState();
            return zkClient.exists(path);
        } catch (Exception e) {
            throw new RegistryException("exists failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isLeader(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return false;
        }
        lock.readLock().lock();
        try {
            push.registry.zookeeper.listener.AbstractLeaderManager leaderManager = leaderManagers.get(path);
            if (leaderManager == null) {
                return false;
            }
            return leaderManager.isLeader();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PathData getData(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return null;
        }
        lock.readLock().lock();
        try {
            checkState();
            return zkClient.getData(path);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("get data failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<PathData> getChildData(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return new ArrayList<PathData>();
        }
        lock.readLock().lock();
        try {
            checkState();
            return zkClient.getChildData(path);
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException("get child data failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<String> getChildren(String path) throws RegistryException {
        if (path == null || path.isEmpty()) {
            return new ArrayList<String>();
        }
        lock.readLock().lock();
        try {
            checkState();
            return zkClient.getChildren(path);
        } catch (Exception e) {
            throw new RegistryException("get children failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, ChildrenListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            push.registry.zookeeper.listener.ChildrenManager manager;
            synchronized (childrenManagers) {
                manager = childrenManagers.get(path);
                if (manager == null) {
                    manager = new push.registry.zookeeper.listener.ChildrenManager(zkClient, path);
                    childrenManagers.put(path, manager);
                }
            }
            manager.addListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, ChildrenDataListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            push.registry.zookeeper.listener.ChildrenDataManager manager;
            synchronized (childrenManagers) {
                manager = childrenDataManagers.get(path);
                if (manager == null) {
                    manager = new push.registry.zookeeper.listener.ChildrenDataManager(zkClient, path);
                    childrenDataManagers.put(path, manager);

                }
            }
            manager.addListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, PathListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            push.registry.zookeeper.listener.PathManager manager;
            synchronized (pathManagers) {
                manager = pathManagers.get(path);
                if (manager == null) {
                    manager = new push.registry.zookeeper.listener.PathManager(zkClient, path);
                    pathManagers.put(path, manager);

                }
            }
            manager.addListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, LeaderListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            push.registry.zookeeper.listener.LeaderManager manager;
            synchronized (leaderManagers) {
                manager = (push.registry.zookeeper.listener.LeaderManager) leaderManagers.get(path);
                if (manager == null) {
                    manager = new push.registry.zookeeper.listener.LeaderManager(zkClient, path);
                    leaderManagers.put(path, manager);
                }
            }
            manager.addListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, ClusterListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            checkState();
            push.registry.zookeeper.listener.ClusterManager manager;
            synchronized (leaderManagers) {
                manager = (push.registry.zookeeper.listener.ClusterManager) leaderManagers.get(path);
                if (manager == null) {
                    manager = new push.registry.zookeeper.listener.ClusterManager(zkClient, path, listener);
                    leaderManagers.put(path, manager);
                }
            }
            manager.addListener(listener);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(ConnectionListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (connectionListeners) {
                connectionListeners.add(listener);
                // open时会自动注册所有ConnectionListener，不需要重复添加，
                // 初始化完成后添加的listener再注册到zkClient中
                if (opened) {
                    zkClient.addListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(String path, PathListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (pathManagers) {
                push.registry.zookeeper.listener.PathManager manager = pathManagers.get(path);
                if (manager != null) {
                    manager.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(String path, ChildrenListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (childrenManagers) {
                push.registry.zookeeper.listener.ChildrenManager manager = childrenManagers.get(path);
                if (manager != null) {
                    manager.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(String path, ChildrenDataListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (childrenDataManagers) {
                push.registry.zookeeper.listener.ChildrenDataManager manager = childrenDataManagers.get(path);
                if (manager != null) {
                    manager.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(String path, LeaderListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (leaderManagers) {
                push.registry.zookeeper.listener.AbstractLeaderManager manager = leaderManagers.get(path);
                if (manager != null) {
                    manager.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(String path, ClusterListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (leaderManagers) {
                push.registry.zookeeper.listener.AbstractLeaderManager manager = leaderManagers.get(path);
                if (manager != null) {
                    manager.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeListener(ConnectionListener listener) {
        if (listener == null) {
            return;
        }
        lock.readLock().lock();
        try {
            synchronized (connectionListeners) {
                if (connectionListeners.remove(listener)) {
                    zkClient.removeListener(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查状态
     */
    protected void checkState() {
        if (!opened) {
            throw new IllegalStateException("ZKRegistry has not been opened yet!");
        }
    }
}