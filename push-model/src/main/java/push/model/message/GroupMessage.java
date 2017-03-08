package push.model.message;

/**
 * Created by lizheng on 2017/2/21.
 */
public class GroupMessage extends AbstractMessage{

    private int nodeid;
    private String message;
    private String messageType;//0:非股票类节点(登录即推送最后一条消息，需要入库保存)。1:股票类节点(只推送即时消息，不用入库保存)

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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
