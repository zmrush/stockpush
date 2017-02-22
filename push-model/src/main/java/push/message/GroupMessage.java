package push.message;

import push.message.AbstractMessage;

/**
 * Created by lizheng on 2017/2/21.
 */
public class GroupMessage extends AbstractMessage{
    private int nodeid;
    private String message;

    public int getNodeid() {
        return nodeid;
    }

    public void setNodeid(int nodeid) {
        this.nodeid = nodeid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
