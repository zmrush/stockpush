package push.io;

import push.message.Entity;

/**
 * Created by mingzhu7 on 2017/1/5.
 */
public class MessageEvent {
    public static enum MessageEventType{
        MESSAGE_RECEIVE
    }
    private MessageEventType messageEventType;
    private Entity.BaseEntity message;

    public MessageEventType getMessageEventType() {
        return messageEventType;
    }

    public void setMessageEventType(MessageEventType messageEventType) {
        this.messageEventType = messageEventType;
    }

    public Entity.BaseEntity getMessage() {
        return message;
    }

    public void setMessage(Entity.BaseEntity message) {
        this.message = message;
    }
}
