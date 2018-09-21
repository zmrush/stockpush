package push.common.pojo;

import push.model.message.AbstractMessage;

/**
 * Created by lizheng on 2017/2/17.
 */
public class NodeBean extends AbstractMessage {
    private int nodeId;//节点id
    private String nodeName;//节点名称
    private String host;//host地址
    private String description;//节点描述
    private String nodeType;

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }
}
