package push.message;

/**
 * Created by mingzhu7 on 2017/2/15.
 */
public class AbstractMessage {
    //1:注册用户。2:群发消息。3:创建节点。4:删除节点。5：订阅节点。6：反订阅节点
    protected String type;

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
