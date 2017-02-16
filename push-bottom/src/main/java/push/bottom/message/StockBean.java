package push.bottom.message;

import push.message.AbstractMessage;

/**
 * Created by mingzhu7 on 2017/1/20.
 */
public class StockBean extends AbstractMessage {
    private String code;
    private float price;
    private Long timestamp;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
