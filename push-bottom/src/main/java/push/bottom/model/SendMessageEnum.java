package push.bottom.model;

/**
 * Created by lizheng on 2017/3/1.
 */
public enum SendMessageEnum {
    //1:注册用户。2:群发消息。3:创建节点。4:删除节点。5：订阅节点。6：反订阅节点
    REGIST_TYPE("1"),
    CREATENODE_TYPE("3"),
    DELATENODE_TYPE("4"),
    SUBSCRIBE_TYPE("5"),
    UNSUBSCRIBE_TYPE("6");

    private String type;
    SendMessageEnum(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static void main(String[] args){
        System.out.println(SendMessageEnum.CREATENODE_TYPE.getType());
    }
}
