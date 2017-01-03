package push;


import com.creditease.toumi.dte.plugin.PluginUtil;
import com.creditease.toumi.dte.registry.listener.*;
import com.creditease.toumi.dte.util.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 注册中心代理
 */
public class RegistryProxy implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(RegistryProxy.class);
    //注册中心URL地址
    protected URL url;
    //存活节点
    protected String live;
    protected Registry registry;
    //初始化等待时间，单位毫秒
    protected long waitTime = 5000;

    public RegistryProxy() {
    }

    public RegistryProxy(String url) {
        this(url, null, 5000);
    }

    public RegistryProxy(String url, String live) {
        this(url, live, 5000);
    }

    public RegistryProxy(String url, String live, long waitTime) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("address is null");
        }
        this.url = URL.valueOf(url);
        this.live = live;
        this.waitTime = waitTime;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    public void setLive(String live) {
        this.live = live;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    public void open() throws RegistryException {
        if (url == null) {
            throw new IllegalStateException("url is null");
        }
        RegistryFactory factory = PluginUtil.createService(RegistryFactory.class, url);
        if (factory != null) {
            registry = factory.getRegistry();
        }
        if (registry == null) {
            registry = PluginUtil.createService(Registry.class, url);
        }
        if (registry == null) {
            throw new IllegalStateException("registry is null," + url.toString());
        }
        registry.open();
        if (live != null && !live.isEmpty()) {
            registry.createLive(live, null);
        }
        if (waitTime > 0) {
            //延迟等待初始化数据
            logger.info(String.format("waiting for data initialization in %d ms", waitTime));
            CountDownLatch latch = new CountDownLatch(1);
            try {
                latch.await(waitTime, TimeUnit.MILLISECONDS);
                logger.info("finished data initialization");
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (registry != null) {
            return registry.isConnected();
        }
        return false;
    }

    @Override
    public void close() throws RegistryException {
        if (registry != null) {
            registry.close();
        }
    }

    @Override
    public void create(String path, byte[] data) throws RegistryException {
        if (registry != null) {
            registry.create(path, data);
        }
    }

    @Override
    public void create(List<String> path) throws RegistryException {
        if (registry != null) {
            registry.create(path);
        }
    }

    @Override
    public void createLive(String path, byte[] data) {
        if (registry != null) {
            registry.createLive(path, data);
        }
    }

    @Override
    public void deleteLive(String path) {
        if (registry != null) {
            registry.deleteLive(path);
        }
    }

    @Override
    public void update(String path, byte[] data) throws RegistryException {
        if (registry != null) {
            registry.update(path, data);
        }
    }

    @Override
    public void update(String path, byte[] data, byte[] parent) throws RegistryException {
        if (registry != null) {
            registry.update(path, data, parent);
        }
    }

    @Override
    public void delete(String path) throws RegistryException {
        if (registry != null) {
            registry.delete(path);
        }
    }

    @Override
    public void delete(List<String> path) throws RegistryException {
        if (registry != null) {
            registry.delete(path);
        }
    }

    @Override
    public boolean exists(String path) throws RegistryException {
        if (registry != null) {
            return registry.exists(path);
        }
        return false;
    }

    @Override
    public boolean isLeader(String path) throws RegistryException {
        if (registry != null) {
            return registry.isLeader(path);
        }
        return false;
    }

    @Override
    public PathData getData(String path) throws RegistryException {
        if (registry != null) {
            return registry.getData(path);
        }
        return null;
    }

    @Override
    public List<PathData> getChildData(String path) throws RegistryException {
        if (registry != null) {
            return registry.getChildData(path);
        }
        return new ArrayList<PathData>();
    }

    @Override
    public List<String> getChildren(String path) throws RegistryException {
        if (registry != null) {
            return registry.getChildren(path);
        }
        return new ArrayList<String>();
    }

    @Override
    public void addListener(String path, ChildrenListener listener) {
        if (registry != null) {
            registry.addListener(path, listener);
        }
    }

    @Override
    public void addListener(String path, ChildrenDataListener listener) {
        if (registry != null) {
            registry.addListener(path, listener);
        }
    }

    @Override
    public void addListener(String path, PathListener listener) {
        if (registry != null) {
            registry.addListener(path, listener);
        }
    }

    @Override
    public void addListener(String path, LeaderListener listener) {
        if (registry != null) {
            registry.addListener(path, listener);
        }
    }

    @Override
    public void addListener(String path, ClusterListener listener) {
        if (registry != null) {
            registry.addListener(path, listener);
        }
    }

    @Override
    public void addListener(ConnectionListener listener) {
        if (registry != null) {
            registry.addListener(listener);
        }
    }

    @Override
    public void removeListener(String path, PathListener listener) {
        if (registry != null) {
            registry.removeListener(path, listener);
        }
    }

    @Override
    public void removeListener(String path, ChildrenListener listener) {
        if (registry != null) {
            registry.removeListener(path, listener);
        }
    }

    @Override
    public void removeListener(String path, ChildrenDataListener listener) {
        if (registry != null) {
            registry.removeListener(path, listener);
        }
    }

    @Override
    public void removeListener(String path, LeaderListener listener) {
        if (registry != null) {
            registry.removeListener(path, listener);
        }
    }

    @Override
    public void removeListener(String path, ClusterListener listener) {
        if (registry != null) {
            registry.removeListener(path, listener);
        }
    }

    @Override
    public void removeListener(ConnectionListener listener) {
        if (registry != null) {
            registry.removeListener(listener);
        }
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
