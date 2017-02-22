package push.bottom.message;

import push.message.AbstractMessage;

/**
 * Created by lizheng on 2017/2/17.
 */
public class SubscribeBean extends AbstractMessage{
    private int  nodeid;//节点id
    private String uid;//用户id，包括userid和设备id

    public int getNodeid() {
        return nodeid;
    }

    public void setNodeid(int nodeid) {
        this.nodeid = nodeid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
