package push.bottom.message;

import push.message.AbstractMessage;

/**
 * Created by lizheng on 2017/2/17.
 */
public class NodeBean extends AbstractMessage{
    private int nodeid;//节点id
    private String nodename;//节点名称
    private String host;//host地址
    private String description;//节点描述
    private String nodetype;

    public int getNodeid() {
        return nodeid;
    }

    public void setNodeid(int nodeid) {
        this.nodeid = nodeid;
    }

    public String getNodename() {
        return nodename;
    }

    public void setNodename(String nodename) {
        this.nodename = nodename;
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

    public String getNodetype() {
        return nodetype;
    }

    public void setNodetype(String nodetype) {
        this.nodetype = nodetype;
    }
}
