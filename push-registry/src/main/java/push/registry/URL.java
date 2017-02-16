package push.registry;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public final class URL implements Serializable {

    public static final String FILE = "file";
    private static final long serialVersionUID = -1985165475234910535L;
    private final String protocol;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String path;
    private final Map<String, String> parameters;

    protected URL() {
        this.protocol = null;
        this.username = null;
        this.password = null;
        this.host = null;
        this.port = 0;
        this.path = null;
        this.parameters = null;
    }

    public URL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String username, String password, String host, int port, String path) {
        this(protocol, username, password, host, port, path, (Map<String, String>) null);
    }

    public URL(String protocol, String username, String password, String host, int port, String path,
               Map<String, String> parameters) {
        if ((username == null || username.isEmpty()) && password != null && password.length() > 0) {
            throw new IllegalArgumentException("Invalid url, password without username!");
        }
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = (port < 0 ? 0 : port);
        this.path = path;
        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        } else {
            parameters = new HashMap<String, String>(parameters);
        }
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * 用";"分割多个URL串
     *
     * @param url 链接
     * @return
     */
    public static String[] split(String url) {
        return split(url, ';');
    }

    /**
     * 分割多个URL串，保持括号
     *
     * @param url       链接
     * @param delimeter 分割符号
     * @return
     */
    public static String[] split(String url, char delimeter) {
        if (url == null || url.isEmpty()) {
            return new String[0];
        }
        List<String> urls = new ArrayList<String>();
        int braces = 0;
        String value = null;
        char[] chars = url.toCharArray();
        StringBuilder builder = new StringBuilder(url.length());
        for (char ch : chars) {
            if (ch == delimeter && braces == 0) {
                if (builder.length() > 0) {
                    value = builder.toString().trim();
                    if (!value.isEmpty()) {
                        urls.add(value);
                    }
                    builder.delete(0, builder.length());
                }
            } else {
                builder.append(ch);
                if (ch == '(') {
                    braces++;
                } else if (ch == ')') {
                    if (braces > 0) {
                        braces--;
                    }
                }
            }
        }
        if (builder.length() > 0) {
            value = builder.toString().trim();
            if (!value.isEmpty()) {
                urls.add(value);
            }
        }
        return urls.toArray(new String[urls.size()]);

    }

    /**
     * 把字符串转化成URL对象
     *
     * @param url
     * @return
     */
    public static URL valueOf(String url) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (url.isEmpty()) {
            return null;
        }
        String protocol = null;
        String username = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;

        // cloud://user:password@jss.360buy.com/mq?timeout=60000
        // file:/path/to/file.txt
        // zookeeper://10.10.10.10:2181,10.10.10.11:2181/?retryTimes=3
        // failover://(zookeeper://10.10.10.10:2181,10.10.10.11:2181;zookeeper://20.10.10.10:2181,20.10.10.11:2181)?interval=1000
        int j = 0;
        int i = url.indexOf(')');
        if (i >= 0) {
            i = url.indexOf('?', i);
        } else {
            i = url.indexOf("?");
        }
        if (i >= 0) {
            if (i < url.length() - 1) {
                String[] parts = url.substring(i + 1).split("\\&");
                parameters = new HashMap<String, String>();
                for (String part : parts) {
                    part = part.trim();
                    if (part.length() > 0) {
                        j = part.indexOf('=');
                        if (j > 0) {
                            if (j == part.length() - 1) {
                                parameters.put(part.substring(0, j), "");
                            } else {
                                parameters.put(part.substring(0, j), part.substring(j + 1));
                            }
                        } else if (j == -1) {
                            parameters.put(part, part);
                        }
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i > 0) {
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else if (i < 0) {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i > 0) {
                protocol = url.substring(0, i);
                // 保留路径符号“/”
                url = url.substring(i + 1);
            }
        }
        if (protocol == null || protocol.isEmpty()) {
            throw new IllegalStateException("url missing protocol: " + url);
        }
        if (protocol.equals(FILE)) {
            path = url;
            url = "";
        } else {
            i = url.lastIndexOf(')');
            if (i >= 0) {
                i = url.indexOf('/', i);
            } else {
                i = url.indexOf("/");
            }
            if (i >= 0) {
                path = url.substring(i + 1);
                url = url.substring(0, i);
            }
        }
        i = url.indexOf('(');
        if (i >= 0) {
            j = url.lastIndexOf(')');
            if (j >= 0) {
                url = url.substring(i + 1, j);
            } else {
                url = url.substring(i + 1);
            }
        } else {
            i = url.indexOf("@");
            if (i >= 0) {
                username = url.substring(0, i);
                j = username.indexOf(":");
                if (j >= 0) {
                    password = username.substring(j + 1);
                    username = username.substring(0, j);
                }
                url = url.substring(i + 1);
            }
            String[] values = url.split(":");
            if (values.length == 2) {
                // 排除zookeeper://192.168.1.2:2181,192.168.1.3:2181
                port = Integer.parseInt(values[1]);
                url = values[0];
            }
        }
        if (!url.isEmpty()) {
            host = url;
        }
        return new URL(protocol, username, password, host, port, path, parameters);
    }

    public static String encode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public URL setProtocol(String protocol) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getUsername() {
        return username;
    }

    public URL setUsername(String username) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getPassword() {
        return password;
    }

    public URL setPassword(String password) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getHost() {
        return host;
    }

    public URL setHost(String host) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public int getPort() {
        return port;
    }

    public URL setPort(int port) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getAddress() {
        return port <= 0 ? host : host + ":" + port;
    }

    public URL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getPath() {
        return path;
    }

    public URL setPath(String path) {
        return new URL(protocol, username, password, host, port, path, getParameters());
    }

    public String getAbsolutePath() {
        if (path != null && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterAndDecoded(String key) {
        return getParameterAndDecoded(key, null);
    }

    public String getParameterAndDecoded(String key, String defaultValue) {
        return decode(getParameter(key, defaultValue));
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public double getParameter(String key, double defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public long getParameter(String key, long defaultValue) {
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public int getParameter(String key, int defaultValue) {
        String value = getParameter(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public short getParameter(String key, short defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Short.parseShort(value);
    }

    public byte getParameter(String key, byte defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Byte.parseByte(value);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public long getNatrualParameter(String key, long defaultValue) {
        if (defaultValue < 0) {
            throw new IllegalArgumentException("defaultValue < 0");
        }
        long value = getParameter(key, defaultValue);
        if (value < 0) {
            return defaultValue;
        }
        return value;
    }

    public int getNatrualParameter(String key, int defaultValue) {
        if (defaultValue < 0) {
            throw new IllegalArgumentException("defaultValue < 0");
        }
        int value = getParameter(key, defaultValue);
        if (value < 0) {
            return defaultValue;
        }
        return value;
    }

    public short getNatrualParameter(String key, short defaultValue) {
        if (defaultValue < 0) {
            throw new IllegalArgumentException("defaultValue < 0");
        }
        short value = getParameter(key, defaultValue);
        if (value < 0) {
            return defaultValue;
        }
        return value;
    }

    public byte getNatrualParameter(String key, byte defaultValue) {
        if (defaultValue < 0) {
            throw new IllegalArgumentException("defaultValue < 0");
        }
        byte value = getParameter(key, defaultValue);
        if (value < 0) {
            return defaultValue;
        }
        return value;
    }

    public long getPositiveParameter(String key, long defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        long value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public int getPositiveParameter(String key, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        int value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public short getPositiveParameter(String key, short defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        short value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }

    public byte getPositiveParameter(String key, byte defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue <= 0");
        }
        byte value = getParameter(key, defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return value;
    }


    public boolean hasParameter(String key) {
        String value = getParameter(key);
        return value != null && !value.isEmpty();
    }

    public URL addParameterAndEncoded(String key, String value) {
        if (value == null || value.isEmpty()) {
            return this;
        }
        return addParameter(key, encode(value));
    }

    public URL addParameter(String key, boolean value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, char value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, byte value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, short value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, int value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, long value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, float value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, double value) {
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, Number value) {
        if (value == null) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return addParameter(key, String.valueOf(value));
    }

    public URL addParameter(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.put(key, value);
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL addParameterIfAbsent(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return this;
        }
        if (hasParameter(key)) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.put(key, value);
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL addParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        map.putAll(parameters);
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL addParametersIfAbsent(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(parameters);
        map.putAll(getParameters());
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL removeParameter(String key) {
        if (key == null || key.isEmpty()) {
            return this;
        }
        return removeParameters(key);
    }

    public URL removeParameters(Collection<String> keys) {
        if (keys == null || keys.size() == 0) {
            return this;
        }
        return removeParameters(keys.toArray(new String[0]));
    }

    public URL removeParameters(String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        Map<String, String> map = new HashMap<String, String>(getParameters());
        for (String key : keys) {
            map.remove(key);
        }
        if (map.size() == getParameters().size()) {
            return this;
        }
        return new URL(protocol, username, password, host, port, path, map);
    }

    public URL clearParameters() {
        return new URL(protocol, username, password, host, port, path, new HashMap<String, String>());
    }

    public String toString() {
        return buildString(false, true); // no show username and password
    }

    public String toString(String... parameters) {
        return buildString(false, true, parameters); // no show username and
        // password
    }

    public String toFullString() {
        return buildString(true, true);
    }

    public String toFullString(String... parameters) {
        return buildString(true, true, parameters);
    }

    public String toParameterString() {
        return toParameterString(new String[0]);
    }

    public String toParameterString(String... parameters) {
        StringBuilder buf = new StringBuilder();
        buildParameters(buf, false, parameters);
        return buf.toString();
    }

    private void buildParameters(StringBuilder buf, boolean concat, String[] parameters) {
        if (getParameters() != null && getParameters().size() > 0) {
            List<String> includes = (parameters == null || parameters.length == 0 ? null : Arrays.asList(parameters));
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(getParameters()).entrySet()) {
                if (entry.getKey() != null && entry.getKey().length() > 0 && (includes == null || includes
                        .contains(entry.getKey()))) {
                    if (first) {
                        if (concat) {
                            buf.append("?");
                        }
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
    }

    private String buildString(boolean u, boolean p, String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (protocol != null && !protocol.isEmpty()) {
            buf.append(protocol);
            buf.append("://");
        }
        if (u && username != null && !username.isEmpty()) {
            buf.append(username);
            if (password != null && !password.isEmpty()) {
                buf.append(":");
                buf.append(password);
            }
            buf.append("@");
        }
        if (host != null && !host.isEmpty()) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }
        if (path != null && !path.isEmpty()) {
            if (path.charAt(0) != '/') {
                buf.append('/');
            }
            buf.append(path);
        }
        if (p) {
            buildParameters(buf, true, parameters);
        }
        return buf.toString();
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        URL other = (URL) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        return true;
    }
}
