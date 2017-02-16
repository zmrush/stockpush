package push.registry.zookeeper;


import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.PathUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ZKUtil {
    private static final Logger logger = Logger.getLogger(ZKUtil.class);

    /**
     * 递归删除目录
     *
     * @param zk   the zookeeper handle
     * @param path the path to be deleted
     * @throws IllegalArgumentException if an invalid path is specified
     * @throws InterruptedException
     */
    public static void deleteRecursive(ZooKeeper zk, final String path) throws InterruptedException, KeeperException {
        PathUtils.validatePath(path);

        List<String> tree = listSubTreeBFS(zk, path);
        for (int i = tree.size() - 1; i >= 0; --i) {
            //Delete the leaves first and eventually get rid of the root
            zk.delete(tree.get(i), -1); //Delete all versions of the node with -1.
        }
    }

    /**
     * 递归删除目录
     *
     * @param zk   the zookeeper handle
     * @param path the path to be deleted
     * @param cb   call back method
     * @param ctx  the context the callback method is called with
     * @throws IllegalArgumentException if an invalid path is specified
     */
    public static void deleteRecursive(ZooKeeper zk, final String path, AsyncCallback.VoidCallback cb,
                                       Object ctx) throws InterruptedException, KeeperException {
        PathUtils.validatePath(path);

        List<String> tree = listSubTreeBFS(zk, path);
        for (int i = tree.size() - 1; i >= 0; --i) {
            //Delete the leaves first and eventually get rid of the root
            zk.delete(tree.get(i), -1, cb, ctx); //Delete all versions of the node with -1.
        }
    }

    /**
     * 宽度优先遍历
     *
     * @param zk   the zookeeper handle
     * @param path The znode path, for which the entire subtree needs to be listed.
     * @throws InterruptedException
     * @throws KeeperException
     */
    public static List<String> listSubTreeBFS(ZooKeeper zk, final String path) throws KeeperException,
            InterruptedException {
        Deque<String> queue = new LinkedList<String>();
        List<String> tree = new ArrayList<String>();
        queue.add(path);
        tree.add(path);
        while (true) {
            String node = queue.pollFirst();
            if (node == null) {
                break;
            }
            List<String> children = zk.getChildren(node, false);
            for (final String child : children) {
                final String childPath = node + "/" + child;
                queue.add(childPath);
                tree.add(childPath);
            }
        }
        return tree;
    }

}
