package push;


import com.creditease.toumi.dte.plugin.ServicePlugin;

/**
 * 注册中心工厂类
 */
public interface RegistryFactory extends ServicePlugin {

    /**
     * 获取创建好的注册中心
     *
     * @return
     */
    Registry getRegistry() throws RegistryException;

}
