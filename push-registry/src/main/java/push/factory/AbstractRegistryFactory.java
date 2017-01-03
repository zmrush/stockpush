package push.factory;


import com.creditease.toumi.dte.plugin.PluginUtil;
import com.creditease.toumi.dte.registry.Registry;
import com.creditease.toumi.dte.registry.RegistryException;
import com.creditease.toumi.dte.registry.RegistryFactory;
import com.creditease.toumi.dte.util.URL;

/**
 * 注册中心抽象基础类
 *
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    /**
     * url
     */
    protected URL url;
    /**
     * 注册中心
     */
    protected Registry cache;

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public Registry getRegistry() throws RegistryException {
        if (cache != null) {
            return cache;
        }
        synchronized (this) {
            if (cache != null) {
                return cache;
            }

            //获取可用的注册中心地址
            try {
                this.cache = createRegistry();
            } catch (RegistryException e) {
                throw e;
            } catch (Exception e) {
                throw new RegistryException(e);
            }
        }

        return cache;
    }

    /**
     * 创建注册中心
     *
     * @return
     */
    protected Registry createRegistry() throws Exception {
        return PluginUtil.createService(Registry.class, URL.valueOf(getRegistryUrl()));
    }

    /**
     * 获取注册中心地址
     *
     * @return
     */
    protected String getRegistryUrl() throws Exception {
        return null;
    }

}
