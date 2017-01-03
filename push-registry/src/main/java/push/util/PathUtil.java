package push.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建节点的工具类
 * Created by Eric on 2015/8/1.
 */
public class PathUtil {

    /**
     * 创建路径
     *
     * @param parent   父节点
     * @param children 孩子节点
     * @return 全路径
     */
    public static String makePath(String parent, String... children) {
        StringBuilder path = new StringBuilder();

        if (parent == null || parent.isEmpty()) {
            parent = "/";
        }
        parent = parent.trim();
        char[] data = parent.toCharArray();
        if (data.length == 0 || data[0] != '/') {
            path.append('/');
        }
        path.append(parent);
        if ((children == null) || (children.length == 0)) {
            return path.toString();
        }
        if (data.length > 0 && data[data.length - 1] != '/') {
            path.append('/');
        }
        String child;
        for (int i = 0; i < children.length; i++) {
            child = children[i];
            if (child == null) {
                continue;
            }
            data = child.trim().toCharArray();
            if (data.length == 0 || (data.length == 1 && data[0] == '/')) {
                continue;
            }
            if (data[0] == '/') {
                path.append(data, 1, data.length - 1);
            } else {
                path.append(data);
            }
            if (data[data.length - 1] != '/') {
                path.append('/');
            }
        }
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1);
        }

        return path.toString();
    }

    public static List<String> getNodes(String path) {
        List<String> result = new ArrayList<String>();
        if (path != null) {
            String[] values = path.split("/");
            for (String value : values) {
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    /**
     * Given a full path, return the node name. i.e. "/one/two/three" will return "three"
     *
     * @param path the path
     * @return the node
     */

    public static String getNodeFromPath(String path) {
        int i = path.lastIndexOf('/');
        if (i < 0) {
            return path;
        }
        if ((i + 1) >= path.length()) {
            return "";
        }
        return path.substring(i + 1);
    }

    /**
     * Validate the provided znode path string
     *
     * @param path znode path string
     * @throws IllegalArgumentException if the path is invalid
     */
    public static void validatePath(String path) throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (path.length() == 0) {
            throw new IllegalArgumentException("Path length must be > 0");
        }
        if (path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with / character");
        }
        if (path.length() == 1) { // done checking - it's the root
            return;
        }
        if (path.charAt(path.length() - 1) == '/') {
            throw new IllegalArgumentException("Path must not end with / character");
        }

        String reason = null;
        char lastc = '/';
        char chars[] = path.toCharArray();
        char c;
        for (int i = 1; i < chars.length; lastc = chars[i], i++) {
            c = chars[i];

            if (c == 0) {
                reason = "null character not allowed @" + i;
                break;
            } else if (c == '/' && lastc == '/') {
                reason = "empty node name specified @" + i;
                break;
            } else if (c == '.' && lastc == '.') {
                if (chars[i - 2] == '/' && ((i + 1 == chars.length) || chars[i + 1] == '/')) {
                    reason = "relative paths not allowed @" + i;
                    break;
                }
            } else if (c == '.') {
                if (chars[i - 1] == '/' && ((i + 1 == chars.length) || chars[i + 1] == '/')) {
                    reason = "relative paths not allowed @" + i;
                    break;
                }
            } else if (c > '\u0000' && c < '\u001f' || c > '\u007f' && c < '\u009F' || c > '\ud800' && c < '\uf8ff' || c > '\ufff0' && c < '\uffff') {
                reason = "invalid charater @" + i;
                break;
            }
        }

        if (reason != null) {
            throw new IllegalArgumentException("Invalid path string \"" + path + "\" caused by " + reason);
        }
    }

    /**
     * 获取父节点路径
     *
     * @param path 路径
     * @return 父节点路径
     */
    public static String getParentFromPath(String path) {
        if (path == null) {
            return null;
        }
        int pos = path.lastIndexOf('/');
        if (pos < 0) {
            return path;
        }
        return path.substring(0, pos);
    }
}
