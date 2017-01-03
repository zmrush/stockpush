package push.factory;


import com.creditease.toumi.dte.plugin.PluginUtil;
import com.creditease.toumi.dte.registry.Registry;
import com.creditease.toumi.dte.registry.RegistryFactory;
import com.creditease.toumi.dte.util.URL;

/**
 * Failover注册中心
 *
 */
public class FailoverRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry() throws Exception {
        if (url.getHost() == null) {
            url = url.setHost("http://;file://");
        }
        Registry registry = null;
        Exception last = null;
        String[] urls = URL.split(url.getHost());
        for (String value : urls) {
            try {
                // 把父参数带入进去
                URL uri = URL.valueOf(value).addParametersIfAbsent(url.getParameters());
                RegistryFactory factory = PluginUtil.createService(RegistryFactory.class, uri);
                if (factory != null) {
                    registry = factory.getRegistry();
                    if (registry != null) {
                        return registry;
                    }
                }
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
        return null;
    }

    @Override
    public String getType() {
        return "failover";
    }


}
