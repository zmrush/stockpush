package push.message;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Login extends BaseEntity {
    @Indexed(unique = true)
    private long uin;
    private String authToken;
    private long activeTime;

    public long getUin() {
        return uin;
    }

    public void setUin(long uin) {
        this.uin = uin;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    @Override
    public String toString() {
        return "Login{" +
                "uin=" + uin +
                ", authToken='" + authToken + '\'' +
                ", activeTime=" + activeTime +
                '}';
    }
}
