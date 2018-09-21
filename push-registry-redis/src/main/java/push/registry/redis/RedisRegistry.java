package push.registry.redis;

import push.registry.AbstractRegistry;
import push.registry.RegistryException;
import push.registry.URL;

public class RedisRegistry extends AbstractRegistry {
    protected URL url;
    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
    @Override
    public void open() throws RegistryException {
        url.getHost()
    }
}
