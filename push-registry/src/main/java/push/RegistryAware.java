package push;

/**
 * 感知注册中心
 */
public interface RegistryAware {

    /**
     * 设置注册中心
     *
     * @param registry
     */
    void setRegistry(Registry registry);
}
