package push;

import push.listener.*;
import push.URL;

import java.util.List;

/**
 * 注册中心接口
 * Created by Eric on 2015/8/1.
 */
public interface Registry{
    /**
     * 获取URL
     *
     * @return URL
     */
    URL getUrl();

    /**
     * 创建实例
     *
     * @throws RegistryException
     */
    void open() throws RegistryException;

    /**
     * 是否连接上
     *
     * @return 是否连接上
     */
    boolean isConnected();

    /**
     * 关闭open()的资源
     *
     * @throws RegistryException
     */
    void close() throws RegistryException;

    /**
     * 用指定的路径、值生成节点
     *
     * @param path 全路径
     * @param data 数据
     * @throws RegistryException
     */
    void create(String path, byte[] data) throws RegistryException;

    /**
     * 批量生成节点(无值)
     *
     * @param paths 全路径
     * @throws RegistryException
     */
    void create(List<String> paths) throws RegistryException;

    /**
     * 创建存活节点
     *
     * @param path 全路径
     * @param data 数据
     */
    void createLive(String path, byte[] data);

    /**
     * 删除存活节点
     *
     * @param path 全路径
     */
    void deleteLive(String path);

    /**
     * 更新节点的值
     *
     * @param path 全路径
     * @param data 数据
     * @throws RegistryException
     */
    void update(String path, byte[] data) throws RegistryException;

    /**
     * 更新节点。更新后会通知关心此值变化的客户端
     *
     * @param path   全路径
     * @param data   子节点数据
     * @param parent 父节点数据
     * @throws RegistryException
     */
    void update(String path, byte[] data, byte[] parent) throws RegistryException;

    /**
     * 删除节点
     *
     * @param path 全路径
     * @throws RegistryException
     */
    void delete(String path) throws RegistryException;

    /**
     * 批量删除
     *
     * @param paths 全路径
     * @throws RegistryException
     */
    void delete(List<String> paths) throws RegistryException;

    /**
     * 制定路径的节点是否存在
     *
     * @param path 全路径
     * @return 节点是否存在
     * @throws RegistryException
     */
    boolean exists(String path) throws RegistryException;

    /**
     * 是否存在leader
     *
     * @param path 全路径
     * @return 是否是leader
     * @throws RegistryException
     */
    boolean isLeader(String path) throws RegistryException;

    /**
     * 获取当前path数据
     *
     * @param path 全路径
     * @return 当前path数据
     * @throws RegistryException
     */
    PathData getData(String path) throws RegistryException;

    /**
     * 获取指定路径孩子节点的值
     *
     * @param path 全路径
     * @return 孩子节点的值，PathData中的路径不包括父节点
     * @throws RegistryException
     */
    List<PathData> getChildData(String path) throws RegistryException;

    /**
     * 获取指定节点下孩子节点的path
     *
     * @param path 全路径
     * @return 孩子节点的path，不包括父节点
     * @throws RegistryException
     */
    List<String> getChildren(String path) throws RegistryException;

    /**
     * 监视该节点下子节点的变化。增加、删除
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void addListener(String path, ChildrenListener listener);

    /**
     * 监视该节点下子节点data的变化，以及子节点增加、删除
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void addListener(String path, ChildrenDataListener listener);

    /**
     * 监视该节点data的变化
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void addListener(String path, PathListener listener);

    /**
     * 监视leader选举
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void addListener(String path, LeaderListener listener);

    /**
     * 监视leader选举
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void addListener(String path, ClusterListener listener);

    /**
     * 监视与server的连接
     */
    void addListener(ConnectionListener listener);

    /**
     * 删除节点listener
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void removeListener(String path, PathListener listener);

    /**
     * 删除节点listener
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void removeListener(String path, ChildrenListener listener);

    /**
     * 删除节点listener
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void removeListener(String path, ChildrenDataListener listener);

    /**
     * 删除节点listener
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void removeListener(String path, LeaderListener listener);

    /**
     * 删除节点listener
     *
     * @param path     全路径
     * @param listener 监听器
     */
    void removeListener(String path, ClusterListener listener);

    /**
     * 删除节点listener
     */
    void removeListener(ConnectionListener listener);
}
