package push.factory;


import com.creditease.toumi.dte.util.Closeables;

import java.io.*;

/**
 * 文件注册中心实现
 */
public class FileRegistryFactory extends AbstractRegistryFactory {
    public static final String DEFAULT_REGISTRY = "registry";
    public static final String USER_HOME = "user.home";

    @Override
    protected String getRegistryUrl() throws Exception {
        InputStream in = null;
        File file = null;
        //设定的文件
        if (url.getPath() != null && !url.getPath().isEmpty()) {
            file = new File(url.getPath());
        }
        if (file == null || !file.exists()) {
            //获取user.home下的文件
            String home = System.getProperty(USER_HOME);
            if (home != null) {
                file = new File(home, DEFAULT_REGISTRY);
            }
        }
        if (file != null && file.exists()) {
            //文件存在
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
            }
        }
        if (in == null) {
            // 没有取到，则从当前Classpath环境获取该文件
            in = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_REGISTRY);
        }
        BufferedReader reader = null;
        if (in != null) {
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                return reader.readLine();
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
        return null;
    }

    @Override
    public String getType() {
        return "file";
    }
}
