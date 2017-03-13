package push.model.message;


/**
 * Created by mingzhu7 on 2017/3/13.
 */
public class PrivateMessage extends AbstractMessage {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
