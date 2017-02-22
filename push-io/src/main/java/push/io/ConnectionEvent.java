package push.io;

import io.netty.channel.ChannelHandlerContext;
import push.message.Entity;

/**
 * Created by mingzhu7 on 2017/1/5.
 */
public class ConnectionEvent {
    public static enum ConnectionEventType{
        CONNECTION_TRANSIENT,
        CONNECTION_ADD,
        CONNECTION_REMOVE,
        MESSAGE_TRANSFER
    }
    private ChannelHandlerContext chc;
    private String uid;
    private ConnectionEventType cet;
    private Entity.BaseEntity message;

    public ChannelHandlerContext getChc() {
        return chc;
    }

    public void setChc(ChannelHandlerContext chc) {
        this.chc = chc;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public ConnectionEventType getCet() {
        return cet;
    }

    public void setCet(ConnectionEventType cet) {
        this.cet = cet;
    }

    public Entity.BaseEntity getMessage() {
        return message;
    }

    public void setMessage(Entity.BaseEntity message) {
        this.message = message;
    }
}
