/**
 *
 */
package push.registry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class NetUtil {

    public static String EXCLUDE_IP = "10.";
    public static String NET_INTERFACE;
    public static Pattern LINUX = Pattern.compile("icmp_seq=\\d+ ttl=\\d+ time=(.*?) ms");
    public static Pattern WINDOWS = Pattern.compile("字节=\\d+ 时间=(.*?)ms TTL=\\d+");

    static {
        NET_INTERFACE = System.getProperty("nic");
        EXCLUDE_IP = System.getProperty("excludeIP", EXCLUDE_IP);
    }

    /**
     * 得到本机所有的IPV4地址
     *
     * @return
     * @throws Exception
     */
    public static List<String> getAllLocalIP() throws Exception {
        List<String> result = new ArrayList<String>();
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> ias = ni.getInetAddresses();
            while (ias.hasMoreElements()) {
                InetAddress ip = ias.nextElement();
                if (!ip.isLoopbackAddress() && (ip instanceof Inet4Address)) {
                    result.add(ip.getHostAddress());
                }
            }
        }
        return result;
    }


    /**
     * 得到指定网卡上的地址
     *
     * @param nic     网卡
     * @param exclude 排除的地址
     * @return 地址列表
     * @throws Exception
     */
    public static List<String> getLocalIP(String nic, String exclude) throws Exception {
        List<String> result = new ArrayList<String>();
        NetworkInterface ni;
        Enumeration<InetAddress> ias;
        InetAddress address;
        String ip;
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            ni = netInterfaces.nextElement();
            if (nic != null && !nic.isEmpty() && !ni.getName().equals(nic)) {
                continue;
            }
            ias = ni.getInetAddresses();
            while (ias.hasMoreElements()) {
                address = ias.nextElement();
                if (!address.isLoopbackAddress() && (address instanceof Inet4Address)) {
                    ip = address.getHostAddress();
                    result.add(ip);
                }
            }
        }

        int count = result.size();
        if (count <= 1) {
            return result;
        }
        if (exclude != null && !exclude.isEmpty()) {
            for (int i = count - 1; i >= 0; i--) {
                ip = result.get(i);
                if (ip.startsWith(exclude)) {
                    result.remove(i);
                    count--;
                    if (count == 1) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 得到本机IPV4地址
     *
     * @param exclude 排除的IP段
     * @return 本机IPV4地址
     * @throws Exception
     */
    public static String getLocalIP(final String exclude) throws Exception {
        String ip=InetAddress.getLocalHost().getHostAddress().toString();
        //如果获取的是回环地址，就采用eth0地址
        if(ip.equals("127.0.0.1")) {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            for (; n.hasMoreElements(); ) {
                NetworkInterface e = n.nextElement();
                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements(); ) {
                    InetAddress addr = a.nextElement();
                    if (addr instanceof Inet4Address)
                        ip = addr.getHostAddress();
                }
                if ("eth0".equals(e.getDisplayName()) || "en4".equals(e.getDisplayName()))
                    break;
            }
        }
        return ip;
//        List<String> ips = getLocalIP(NET_INTERFACE, exclude);
//        if (ips != null && !ips.isEmpty()) {
//            if (ips.size() == 1) {
//                return ips.get(0);
//            }
//            for (String ip : ips) {
//                if (ip.startsWith("172.")) {
//                    return ip;
//                } else if (ip.startsWith("192.")) {
//                    return ip;
//                }
//            }
//            return ips.get(0);
//        }
//        return null;
    }

    /**
     * 得到本机IPV4地址
     *
     * @return 本机IPV4地址
     * @throws Exception
     */
    public static String getLocalIP() throws Exception {
        return getLocalIP(EXCLUDE_IP);
    }

    /**
     * 获取地址字符串
     *
     * @param address 地址
     * @return 地址字符串
     */
    public static String getAddress(SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) address;
            return isa.getAddress().getHostAddress() + ":" + isa.getPort();
        } else {
            return address.toString();
        }
    }

    /**
     * 获取IP
     *
     * @param address 地址数组
     * @return IP
     */
    public static String getIp(byte[] address) {
        if (address == null || address.length < 4) {
            return null;
        }
        int pos = 0;
        if (address.length >= 6) {
            pos += 2;
        }
        StringBuilder builder = new StringBuilder(20);
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF);
        return builder.toString();
    }

    /**
     * 地址转化成字节数组
     *
     * @param socketAddress 地址对象
     * @return 字节数组
     */
    public static byte[] toByte(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            throw new IllegalArgumentException("socketAddress is null");
        }
        InetAddress inetAddress = socketAddress.getAddress();
        if (inetAddress == null) {
            throw new IllegalArgumentException("socketAddress is invalid");
        }
        byte[] address = inetAddress.getAddress();
        byte[] result = new byte[address.length + 2];
        System.arraycopy(address, 0, result, 2, address.length);
        int port = socketAddress.getPort();
        result[1] = (byte) (port >> 8 & 0xFF);
        result[0] = (byte) (port & 0xFF);
        return result;
    }

    /**
     * 转换成地址
     *
     * @param address 地址字节数组
     * @return 地址对象
     */
    public static InetSocketAddress toAddress(byte[] address) throws UnknownHostException {
        if (address == null || address.length < 6) {
            throw new IllegalArgumentException("address is invalid");
        }
        int port = address[0] & 0xFF;
        port |= (address[1] << 8 & 0xFF00);
        byte[] inetAddress = new byte[address.length - 2];
        System.arraycopy(address, 2, inetAddress, 0, inetAddress.length);
        return new InetSocketAddress(InetAddress.getByAddress(inetAddress), port);
    }

    /**
     * 转换成可阅读模式
     *
     * @param address 字节数组
     * @param builder 字符串构造器
     */
    public static void toAddress(byte[] address, StringBuilder builder) {
        if (builder == null) {
            return;
        }
        if (address == null) {
            throw new IllegalArgumentException("address is invalid");
        }
        if (address.length < 4) {
            throw new IllegalArgumentException("address is invalid");
        }
        int pos = 0;
        int port = 0;
        if (address.length >= 6) {
            port = address[pos++] & 0xFF;
            port |= (address[pos++] << 8 & 0xFF00);
        }
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF).append('.');
        builder.append(address[pos++] & 0xFF);
        if (address.length >= 6) {
            builder.append(':').append(port);
        }
    }

    /**
     * 把16进制地址字符串转化成字节数组
     *
     * @param address 地址
     * @return 字节数组
     */
    public static byte[] toByteByHex(String address) {
        if (address == null) {
            throw new IllegalArgumentException("address is invalid");
        }
        int len = address.length();
        if (len < 8) {
            throw new IllegalArgumentException("address is invalid");
        }
        byte[] buf;
        if (len >= 12) {
            buf = new byte[6];
        } else {
            buf = new byte[4];
        }
        int pos = 0;
        if (len >= 12) {
            buf[pos++] = (byte) Integer.parseInt(address.substring(10, 12), 16);
            buf[pos++] = (byte) Integer.parseInt(address.substring(8, 10), 16);
        }
        buf[pos++] = (byte) Integer.parseInt(address.substring(0, 2), 16);
        buf[pos++] = (byte) Integer.parseInt(address.substring(2, 4), 16);
        buf[pos++] = (byte) Integer.parseInt(address.substring(4, 6), 16);
        buf[pos++] = (byte) Integer.parseInt(address.substring(6, 8), 16);
        return buf;
    }

    /**
     * 把10进制地址字符串转化成字节数组
     *
     * @param address 地址
     * @return 字节数组
     */
    public static byte[] toByteByAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("address is invalid");
        }
        String[] ips = address.split("[.:_]");
        if (ips.length < 4) {
            throw new IllegalArgumentException("broker is invalid");
        }
        int pos = 0;
        byte[] buf;
        if (ips.length > 4) {
            buf = new byte[6];
            int port = Integer.parseInt(ips[4]);
            buf[pos++] = (byte) (port & 0xFF);
            buf[pos++] = (byte) (port >> 8 & 0xFF);
        } else {
            buf = new byte[4];
        }
        buf[pos++] = (byte) Integer.parseInt(ips[0]);
        buf[pos++] = (byte) Integer.parseInt(ips[1]);
        buf[pos++] = (byte) Integer.parseInt(ips[2]);
        buf[pos++] = (byte) Integer.parseInt(ips[3]);
        return buf;
    }

    /**
     * 把字节数组转化成16进制字符串
     *
     * @param address 字节数组
     * @return 16进制
     */
    public static String toHex(byte[] address) {
        StringBuilder builder = new StringBuilder();
        toHex(address, builder);
        return builder.toString();
    }

    /**
     * 把字节数组转化成16进制字符串
     *
     * @param address 字节数组
     * @param builder 字符串构造器
     */
    public static void toHex(byte[] address, StringBuilder builder) {
        if (address == null || address.length < 4) {
            throw new IllegalArgumentException("address is invalid");
        }
        if (builder == null) {
            throw new IllegalArgumentException("builder is invalid");
        }
        String hex;
        int pos = 0;
        int port = 0;
        if (address.length >= 6) {
            port = address[pos++] & 0xFF;
            port |= (address[pos++] << 8 & 0xFF00);
        }

        for (int i = 0; i < 4; i++) {
            hex = Integer.toHexString(address[pos++] & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                builder.append('0').append(hex);
            } else {
                builder.append(hex);
            }
        }
        if (address.length >= 6) {
            hex = Integer.toHexString(port).toUpperCase();
            int len = hex.length();
            if (len == 1) {
                builder.append("000").append(hex);
            } else if (len == 2) {
                builder.append("00").append(hex);
            } else if (len == 3) {
                builder.append("0").append(hex);
            } else {
                builder.append(hex);
            }
        }
    }

    /**
     * Ping IP
     *
     * @param ip    IP
     * @param count 次数
     * @return 平均响应时间
     * @throws java.io.IOException
     */
    public static double ping(final String ip, final int count) throws IOException {
        return ping(ip, count, -1);
    }

    /**
     * Ping IP,Linux普通用户时间间隔不能小于200毫秒
     *
     * @param ip       IP
     * @param count    次数
     * @param interval 间隔(毫秒)
     * @return 平均响应时间
     * @throws java.io.IOException
     */
    public static double ping(final String ip, final int count, final long interval) throws IOException {
        LineNumberReader input = null;
        try {
            // 根据操作系统构造ping命令
            String osName = System.getProperties().getProperty("os.name");
            String charset = System.getProperties().getProperty("sun.jnu.encoding");
            String pingCmd = null;
            boolean windows = osName.toUpperCase().startsWith("WINDOWS");
            if (windows) {
                pingCmd = "cmd /c ping -n {0} {1}";
                pingCmd = MessageFormat.format(pingCmd, count, ip);
            } else if (interval > 0) {
                pingCmd = "ping -c {0} -i {1} {2}";
                pingCmd = MessageFormat.format(pingCmd, count, interval * 1.0 / 1000, ip);
            } else {
                pingCmd = "ping -c {0} {1}";
                pingCmd = MessageFormat.format(pingCmd, count, ip);
            }
            // 执行ping命令
            Process process = Runtime.getRuntime().exec(pingCmd);
            // 读取输出
            input = new LineNumberReader(new InputStreamReader(process.getInputStream(), charset));
            String line;
            List<String> outputs = new ArrayList<String>();
            while ((line = input.readLine()) != null) {
                if (!line.isEmpty()) {
                    outputs.add(line);
                }
            }
            double result = 0;
            int success = 0;
            if (windows) {
                for (String str : outputs) {
                    Matcher matcher = WINDOWS.matcher(str);
                    if (matcher.find()) {
                        success++;
                        result += Double.valueOf(matcher.group(1));
                    }
                }
            } else {
                for (String str : outputs) {
                    Matcher matcher = LINUX.matcher(str);
                    if (matcher.find()) {
                        success++;
                        result += Double.valueOf(matcher.group(1));
                    }
                }
            }
            if (success > 0) {
                return result / success;
            }
            return -1;
        } finally {
            Closeables.close(input);
        }
    }

}
