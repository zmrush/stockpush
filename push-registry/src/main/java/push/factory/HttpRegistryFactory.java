package push.factory;


import com.creditease.toumi.dte.util.URL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * HTTP远程请求注册中心
 *
 */
public class HttpRegistryFactory extends AbstractRegistryFactory {

    public static final String TIMEOUT = "timeout";
    public static final String RETRY_TIMES = "retryTimes";
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int DEFAULT_RETRY_TIMES = 1;
    public static final String UTF_8 = "UTF-8";
    public static final String CHARSET = "charset";

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    protected String getRegistryUrl() throws Exception {
        if (url == null) {
            throw new IllegalStateException("url is null");
        }
        if (url.getHost() == null) {
            throw new IllegalStateException("url is invalid");
        }
        String registry = getHtml(url);
        if (registry.contains("</html>") || registry.contains("<html>")) {
            return null;
        }

        return registry;
    }

    @Override
    public String getType() {
        return "http";
    }

    /**
     * 获取网页数据
     *
     * @param url
     * @return
     * @throws java.io.IOException
     */
    protected String getHtml(URL url) throws IOException {

        if (url == null) {
            return null;
        }

        int retryTimes = url.getPositiveParameter(RETRY_TIMES, DEFAULT_RETRY_TIMES);
        int timeout = url.getPositiveParameter(TIMEOUT, DEFAULT_TIMEOUT);
        String charset = url.getParameter(CHARSET, UTF_8);
        url = url.removeParameters(RETRY_TIMES, TIMEOUT, CHARSET);

        return getHtml(url, retryTimes, timeout, charset);

    }

    /**
     * 获取网页数据
     *
     * @param url
     * @param retryTimes
     * @param timeout
     * @param charset
     * @return
     * @throws java.io.IOException
     */
    protected String getHtml(URL url, int retryTimes, int timeout, String charset) throws IOException {
        if (url == null) {
            return null;
        }
        if (retryTimes < 0) {
            retryTimes = 0;
        }
        if (timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (charset == null || charset.isEmpty()) {
            charset = UTF_8;
        }

        HttpURLConnection cnn = null;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        for (int i = 0; i < retryTimes + 1; i++) {
            try {
                java.net.URL ul = new java.net.URL(url.toFullString());
                cnn = (HttpURLConnection) ul.openConnection();
                cnn.setConnectTimeout(timeout);
                cnn.setReadTimeout(timeout);
                cnn.setRequestProperty(CHARSET, charset);
                cnn.setUseCaches(false);

                //连接
                cnn.connect();
                if (cnn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    if (cnn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        //页面不存在
                        i = retryTimes;
                    }
                    throw new IOException("http error,code=" + cnn.getResponseCode());
                }

                //获取数据
                reader = new BufferedReader(new InputStreamReader(cnn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                return builder.toString();
            } catch (IOException e) {
                if (i >= retryTimes) {
                    throw new IOException(url.toString(), e);
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {

                    }
                }
                if (cnn != null) {
                    cnn.disconnect();
                }
            }
        }
        return null;
    }

}
