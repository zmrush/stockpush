package push.common.pojo;

import push.model.message.AbstractMessage;

/**
 * Created by mingzhu7 on 2017/2/15.
 */
public class Registration extends AbstractMessage {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
