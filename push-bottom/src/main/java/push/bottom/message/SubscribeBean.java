package push.bottom.message;

import push.model.message.AbstractMessage;

/**
 * Created by lizheng on 2017/2/17.
 */
public class SubscribeBean extends AbstractMessage{
    private int  nodeId;//节点id
    private String uid;//用户id，包括userid和设备id

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
