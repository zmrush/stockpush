package push.factory;

/**
 * Created with IntelliJ IDEA.
 */
public class FixedRegistryFactory extends AbstractRegistryFactory {

    @Override
    public String getType() {
        return "fix";
    }

    /**
     * 获取注册中心地址
     *
     * @return
     */
    protected String getRegistryUrl() throws Exception {
        return url.getHost();
    }
}
