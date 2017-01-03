package push;


import com.creditease.toumi.dte.registry.listener.*;
import com.creditease.toumi.dte.registry.util.PathUtil;
import com.creditease.toumi.dte.util.EventManager;
import com.creditease.toumi.dte.util.URL;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于内存的注册中心
 */
public class RegistryMock implements Registry {
    public static final String MEMBER = "member-";
    //URL
    protected URL url;
    //根节点
    protected Node root = new Node();
    //节点事件
    protected EventManager<NodeEvent> nodeEventManager = new EventManager<NodeEvent>(new NodeEventListener());
    //通知事件
    protected EventManager<PathEvent> pathEventManager = new EventManager<PathEvent>();
    protected EventManager<ChildrenEvent> childrenEventManager = new EventManager<ChildrenEvent>();
    protected EventManager<LeaderEvent> leaderEventManager = new EventManager<LeaderEvent>();
    protected EventManager<ClusterEvent> clusterEventManager = new EventManager<ClusterEvent>();
    protected Status status = Status.CLOSED;
    protected Map<String, String> leaders = new HashMap<String, String>();
    protected Map<String, Map<ChildrenListener, Node>> childrenListeners =
            new HashMap<String, Map<ChildrenListener, Node>>();
    protected Map<String, Map<ChildrenDataListener, Node>> childrenDataListeners =
            new HashMap<String, Map<ChildrenDataListener, Node>>();
    protected Map<String, Map<PathListener, Node>> pathListeners = new HashMap<String, Map<PathListener, Node>>();
    protected Map<String, Map<LeaderListener, Node>> leaderListeners = new HashMap<String, Map<LeaderListener, Node>>();
    protected Map<String, Map<ClusterListener, Node>> clusterListeners =
            new HashMap<String, Map<ClusterListener, Node>>();
    protected TreeSet<ConnectionListener> connectionListeners = new TreeSet<ConnectionListener>();
    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected ReentrantReadWriteLock listenerLock = new ReentrantReadWriteLock();

    public RegistryMock() {

    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void open() throws RegistryException {
        lock.writeLock().lock();
        try {
            if (status == Status.OPENED) {
                return;
            }
            status = Status.OPENED;
            nodeEventManager.start();
            pathEventManager.start();
            childrenEventManager.start();
            leaderEventManager.start();
            clusterEventManager.start();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isConnected() {
        lock.readLock().lock();
        try {
            return status == Status.OPENED;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws RegistryException {
        lock.writeLock().lock();
        try {
            status = Status.CLOSING;
            nodeEventManager.stop();
            pathEventManager.stop();
            childrenEventManager.stop();
            leaderEventManager.stop();
            clusterEventManager.stop();
            //不再产生通知事件
            root.removeDescendants();
            status = Status.CLOSED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查状态
     */
    protected void checkState() {
        if (status != Status.OPENED) {
            throw new IllegalStateException("registry has not been opened yet!");
        }
    }

    @Override
    public void create(String path, byte[] data) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            root.createDescendant(path, data);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void create(List<String> paths) throws RegistryException {
        if (paths != null) {
            lock.readLock().lock();
            try {
                checkState();
                for (String path : paths) {
                    root.createDescendant(path, null);
                }
            } finally {
                lock.readLock().unlock();
            }

        }
    }

    @Override
    public void createLive(String path, byte[] data) {
        lock.readLock().lock();
        try {
            checkState();
            root.createDescendant(path, data);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void deleteLive(String path) {
        lock.readLock().lock();
        try {
            checkState();
            root.removeDescendant(path);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void update(String path, byte[] data) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            root.createDescendant(path, data);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void update(String path, byte[] data, byte[] parent) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            Node node = root.createDescendant(path, data);
            if (node.getParent() != root) {
                node.setData(parent);
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void delete(String path) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            root.removeDescendant(path);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void delete(List<String> paths) throws RegistryException {
        if (paths != null) {
            lock.readLock().lock();
            try {
                checkState();
                for (String path : paths) {
                    root.removeDescendant(path);
                }
            } finally {
                lock.readLock().unlock();
            }

        }
    }

    @Override
    public boolean exists(String path) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            Node node = root.getDescendant(path);
            return node != null;
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public boolean isLeader(String path) throws RegistryException {
        return leaders.containsKey(path);
    }

    @Override
    public PathData getData(String path) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            Node node = root.getDescendant(path);
            if (node != null) {
                return new PathData(node.getName(), node.getData());
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public List<PathData> getChildData(String path) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            List<PathData> result = new ArrayList<PathData>();
            Node node = root.getDescendant(path);
            if (node != null) {
                for (Node child : node.getChildren()) {
                    result.add(new PathData(child.getName(), child.getData()));
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public List<String> getChildren(String path) throws RegistryException {
        lock.readLock().lock();
        try {
            checkState();
            List<String> result = new ArrayList<String>();
            Node node = root.getDescendant(path);
            if (node != null) {
                for (Node child : node.getChildren()) {
                    result.add(child.getName());
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }

    }

    protected <T> Node removeEventListener(Map<String, Map<T, Node>> listeners, String path, T listener) {
        listenerLock.writeLock().lock();
        try {
            Map<T, Node> map = listeners.get(path);
            if (map != null) {
                return map.remove(listener);
            }
            return null;
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    protected <T> boolean addEventListener(Map<String, Map<T, Node>> listeners, String path, T listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return false;
        }
        listenerLock.writeLock().lock();
        try {
            Map<T, Node> map = listeners.get(path);
            if (map == null) {
                map = new HashMap<T, Node>();
                listeners.put(path, map);
            }
            if (!map.containsKey(listener)) {
                map.put(listener, null);
                return true;
            }
            return false;
        } finally {
            listenerLock.writeLock().unlock();
        }

    }

    @Override
    public void addListener(String path, ChildrenListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        PathUtil.validatePath(path);
        lock.readLock().lock();
        try {
            Node node = root.createDescendant(path, null);
            if (addEventListener(childrenListeners, path, listener)) {
                //初始化事件
                for (Node child : node.getChildren()) {
                    childrenEventManager
                            .add(new ChildrenEvent(ChildrenEvent.ChildrenEventType.CHILD_CREATED, child.getName(),
                                    null), listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, ChildrenDataListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        PathUtil.validatePath(path);
        lock.readLock().lock();
        try {
            Node node = root.createDescendant(path, null);
            if (addEventListener(childrenDataListeners, path, listener)) {
                //初始化事件
                for (Node child : node.getChildren()) {
                    childrenEventManager
                            .add(new ChildrenEvent(ChildrenEvent.ChildrenEventType.CHILD_CREATED, child.getName(),
                                    child.getData()), listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, PathListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        PathUtil.validatePath(path);
        lock.readLock().lock();
        try {
            if (addEventListener(pathListeners, path, listener)) {
                Node node = root.getDescendant(path);
                if (node != null) {
                    pathEventManager.add(new PathEvent(PathEvent.PathEventType.CREATED, node.getName(), node.getData()),
                            listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, LeaderListener listener) {
        //创建选举临时节点
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        PathUtil.validatePath(path);
        lock.readLock().lock();
        try {
            if (addEventListener(leaderListeners, path, listener)) {
                Node node = root.createDescendant(path, null);
                synchronized (node) {
                    // 创建了选举节点
                    Node child = createLeaderNode(node, null);
                    if (child != null) {
                        leaderListeners.get(path).put(listener, child);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addListener(String path, ClusterListener listener) {
        //创建选举临时节点
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        PathUtil.validatePath(path);
        lock.readLock().lock();
        try {
            if (addEventListener(clusterListeners, path, listener)) {
                Node node = root.createDescendant(path, null);
                synchronized (node) {
                    Node child = createLeaderNode(node, listener.getNodeName().getBytes());
                    if (child != null) {
                        clusterListeners.get(path).put(listener, child);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 在当前节点下创建选举节点
     *
     * @param node 节点
     */
    protected Node createLeaderNode(Node node, byte[] data) {
        if (node == null) {
            return null;
        }
        Node child;
        String name;
        long seq = 0;
        long maxSeq = 0;
        //得到孩子节点
        List<Node> children = node.getChildren();
        //遍历获取最大的顺序
        for (int i = children.size() - 1; i >= 0; i--) {
            child = children.get(i);
            name = child.getName();
            if (name.startsWith(MEMBER)) {
                try {
                    seq = Long.valueOf(name.substring(MEMBER.length()));
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        //添加选举节点
        maxSeq++;
        name = String.format("%s%09d", MEMBER, maxSeq);
        child = new Node(name, data);
        node.addChild(child);
        if (maxSeq == 1) {
            leaders.put(node.getPath(), name);
        }
        return child;

    }

    @Override
    public void addListener(ConnectionListener listener) {
        if (listener == null) {
            return;
        }
        listenerLock.writeLock().lock();
        try {
            connectionListeners.add(listener);
        } finally {
            listenerLock.writeLock().unlock();
        }

    }

    @Override
    public void removeListener(String path, PathListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            removeEventListener(pathListeners, path, listener);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void removeListener(String path, ChildrenListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            removeEventListener(childrenListeners, path, listener);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void removeListener(String path, ChildrenDataListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            removeEventListener(childrenDataListeners, path, listener);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void removeListener(String path, LeaderListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            listenerLock.writeLock().lock();
            try {
                Map<LeaderListener, Node> listeners = leaderListeners.get(path);
                Node node = null;
                if (listeners != null) {
                    node = listeners.remove(listener);
                }
                // 删除该监听器节点
                if (node != null) {
                    Node parent = node.getParent();
                    if (parent != null) {
                        parent.removeChild(node);
                    }
                }
            } finally {
                listenerLock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void removeListener(String path, ClusterListener listener) {
        if (listener == null || path == null || path.isEmpty()) {
            return;
        }
        lock.readLock().lock();
        try {
            listenerLock.writeLock().lock();
            try {
                Map<ClusterListener, Node> listeners = clusterListeners.get(path);
                Node node = null;
                if (listeners != null) {
                    node = listeners.remove(listener);
                }
                if (node != null) {
                    Node parent = node.getParent();
                    if (parent != null) {
                        parent.removeChild(node);
                    }
                }
            } finally {
                listenerLock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void removeListener(ConnectionListener listener) {
        if (listener == null) {
            return;
        }
        listenerLock.writeLock().lock();
        try {
            connectionListeners.remove(listener);
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    @Override
    public String getType() {
        return "mock";
    }

    /**
     * 创建节点通知事件
     *
     * @param parent 父节点
     * @param node   节点
     * @param type   事件类型
     */
    protected void createPathEvent(Node parent, Node node, PathEvent.PathEventType type) {
        if (node == null || parent == null) {
            return;
        }
        Map<PathListener, Node> listeners = pathListeners.get(node.getPath());
        if (listeners != null && !listeners.isEmpty()) {
            for (PathListener listener : listeners.keySet()) {
                pathEventManager.add(new PathEvent(type, node.getName(), node.getData()), listener);
            }
        }
    }

    /**
     * 创建子节点通知事件
     *
     * @param parent 父节点
     * @param node   节点
     * @param type   事件类型
     */
    protected void createChildrenEvent(Node parent, Node node, ChildrenEvent.ChildrenEventType type) {
        if (node == null || parent == null) {
            return;
        }
        Map<ChildrenListener, Node> listeners = childrenListeners.get(parent.getPath());
        if (listeners != null && !listeners.isEmpty()) {
            for (ChildrenListener listener : listeners.keySet()) {
                childrenEventManager.add(new ChildrenEvent(type, node.getName(), null), listener);
            }
        }
    }

    /**
     * 创建子节点数据通知事件
     *
     * @param parent 父节点
     * @param node   节点
     * @param type   事件类型
     */
    protected void createChildrenDataEvent(Node parent, Node node, ChildrenEvent.ChildrenEventType type) {
        if (node == null || parent == null) {
            return;
        }
        Map<ChildrenDataListener, Node> listeners = childrenDataListeners.get(parent.getPath());
        if (listeners != null) {
            for (ChildrenDataListener listener : listeners.keySet()) {
                childrenEventManager.add(new ChildrenEvent(type, node.getName(), node.getData()), listener);
            }
        }
    }

    /**
     * 创建选举数据通知事件
     *
     * @param parent 父节点
     * @param node   节点
     */
    protected void createLeaderEvent(Node parent, Node node) {
        if (node == null || parent == null) {
            return;
        }
        Map<LeaderListener, Node> listeners = leaderListeners.get(parent.getPath());
        if (listeners != null && !listeners.isEmpty()) {
            LeaderEvent.LeaderEventType type =
                    parent.size() == 0 ? LeaderEvent.LeaderEventType.LOST : LeaderEvent.LeaderEventType.TAKE;
            for (LeaderListener listener : listeners.keySet()) {
                leaderEventManager.add(new LeaderEvent(type, parent.getName()), listener);
            }
        }
    }

    /**
     * 创建集群数据通知事件
     *
     * @param parent 父节点
     * @param node   节点
     */
    protected void createClusterEvent(Node parent, Node node) {
        if (node == null || parent == null) {
            return;
        }
        Map<ClusterListener, Node> listeners = clusterListeners.get(parent.getPath());
        if (listeners != null && !listeners.isEmpty()) {
            //集群监听器
            List<PathData> states = new ArrayList<PathData>();
            for (Node child : parent.getChildren()) {
                states.add(new PathData(child.getName(), child.getData()));
            }
            for (ClusterListener listener : listeners.keySet()) {
                clusterEventManager.add(new ClusterEvent(parent.getName(), states), listener);
            }
        }
    }

    /**
     * 状态
     */
    protected enum Status {
        OPENED, CLOSING, CLOSED
    }

    /**
     * 节点事件类型
     */
    protected enum NodeEventType {
        UPDATE,
        ADD,
        DELETE
    }

    /**
     * 树节点
     */
    public class Node {
        //名称
        private String name;
        //完整路径
        private String path;
        //数据
        private byte[] data;
        //父节点
        private Node parent;
        //孩子节点
        private Map<String, Node> children;

        public Node() {
            this(null, null, null);
        }

        public Node(String name, byte[] data) {
            this(name, data, null);
        }

        public Node(String name, byte[] data, Node parent) {
            this.name = name;
            this.data = data;
            if (parent != null) {
                this.parent = parent;
                this.parent.addChild(this);
            } else {
                this.path = PathUtil.makePath("/", name);
            }
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }

        protected void setData(byte[] data) {
            this.data = data;
            nodeEventManager.add(new NodeEvent(NodeEventType.UPDATE, parent, this));
        }

        public String getPath() {
            return path;
        }

        public Node getParent() {
            return parent;
        }

        /**
         * 获取孩子节点列表
         *
         * @return 孩子节点列表
         */
        public List<Node> getChildren() {
            //复制一份
            List<Node> result = new ArrayList<Node>();
            synchronized (this) {
                if (children != null && !children.isEmpty()) {
                    result.addAll(children.values());
                }
            }
            return result;
        }

        /**
         * 孩子节点的数量
         *
         * @return 孩子节点的数量
         */
        public int size() {
            int result = 0;
            synchronized (this) {
                if (children != null) {
                    result = children.size();
                }
            }
            return result;
        }

        protected Map<String, Node> getChildrenMap() {
            if (children == null) {
                children = new TreeMap<String, Node>();
            }
            return children;
        }

        public Node getChild(String child) {
            if (child == null || child.isEmpty()) {
                return null;
            }
            synchronized (this) {
                return getChildrenMap().get(child);
            }
        }

        /**
         * 添加子节点
         *
         * @param child 子节点
         */
        protected void addChild(Node child) {
            if (child == null || child.getName() == null || child.getName().isEmpty()) {
                throw new IllegalArgumentException("child is null or it's name is empty");
            }
            if (child.parent != this) {
                child.parent = this;
                child.path = PathUtil.makePath(this.path, child.name);
            }
            synchronized (this) {
                Map<String, Node> children = getChildrenMap();
                if (!children.containsKey(child.getName())) {
                    children.put(child.getName(), child);
                    nodeEventManager.add(new NodeEvent(NodeEventType.ADD, this, child));
                }
            }
        }

        /**
         * 删除子节点
         *
         * @param child 子节点
         */
        protected Node removeChild(String child) {
            if (child == null || child.isEmpty()) {
                return null;
            }
            synchronized (this) {
                Node node = getChildrenMap().remove(child);
                if (node != null) {
                    node.parent = null;
                    nodeEventManager.add(new NodeEvent(NodeEventType.DELETE, this, node));
                }
                return node;
            }
        }

        /**
         * 删除子节点
         *
         * @param child 子节点
         */
        protected Node removeChild(Node child) {
            if (child == null) {
                return null;
            }
            return removeChild(child.getName());
        }

        /**
         * 递归获取子节点
         *
         * @param path 路径
         * @return 子节点
         */
        public Node getDescendant(String path) {
            PathUtil.validatePath(path);
            List<String> nodes = PathUtil.getNodes(path);
            Node parent = this;
            Node child = parent;
            for (String node : nodes) {
                child = parent.getChild(node);
                if (child == null) {
                    break;
                } else {
                    parent = child;
                }
            }
            return child;
        }

        /**
         * 递归创建子节点
         *
         * @param path 路径
         * @param data 数据
         * @return 子节点
         */
        protected Node createDescendant(String path, byte[] data) {
            PathUtil.validatePath(path);
            List<String> nodes = PathUtil.getNodes(path);
            boolean created = false;
            Node parent = this;
            Node child = parent;
            for (String node : nodes) {
                child = parent.getChild(node);
                if (child == null) {
                    created = true;
                    child = new Node(node, null);
                    parent.addChild(child);
                }
                parent = child;
            }
            if (!created) {
                child.setData(data);
            } else {
                child.data = data;
            }
            return child;
        }

        /**
         * 递归删除子节点
         *
         * @param path 路径
         * @return 子节点
         */
        protected Node removeDescendant(String path) {
            PathUtil.validatePath(path);
            List<String> nodes = PathUtil.getNodes(path);
            Node parent = this;
            Node child = parent;
            String node;
            for (int i = 0; i < nodes.size(); i++) {
                node = nodes.get(i);
                if (i == nodes.size() - 1) {
                    child = parent.removeChild(node);
                } else {
                    child = parent.getChild(node);
                }
                if (child != null) {
                    parent = child;
                } else {
                    break;
                }
            }
            return child;
        }

        /**
         * 递归删除所有子节点
         */
        protected void removeDescendants() {
            Queue<Node> queue = new LinkedList<Node>();
            queue.add(this);
            Node node;
            while (!queue.isEmpty()) {
                node = queue.poll();
                synchronized (node) {
                    if (node.children != null && !node.children.isEmpty()) {
                        for (Node child : node.children.values()) {
                            queue.add(child);
                        }
                        node.children.clear();
                        nodeEventManager.add(new NodeEvent(NodeEventType.DELETE, node.parent, node));
                    }
                    node.children = null;
                    node.parent = null;
                }
            }
        }

        /**
         * 返回所有的路径集合
         *
         * @return 路径集合
         */
        public List<String> getPaths() {
            List<String> result = new ArrayList<String>();
            Stack<Node> stack = new Stack<Node>();
            stack.add(this);
            Node node;
            while (!stack.isEmpty()) {
                node = stack.pop();
                result.add(node.getPath());
                synchronized (node) {
                    if (node.children != null && !node.children.isEmpty()) {
                        Collection<Node> nodes = node.children.values();
                        Node[] childs = nodes.toArray(new Node[nodes.size()]);
                        for (int i = childs.length - 1; i >= 0; i--) {
                            stack.push(childs[i]);
                        }
                    }
                }
            }
            return result;
        }
    }

    /**
     * 节点事件
     */
    protected class NodeEvent {
        private NodeEventType type;
        private Node parent;
        private Node node;

        public NodeEvent(NodeEventType type, Node parent, Node node) {
            this.type = type;
            this.parent = parent;
            this.node = node;
        }

        public NodeEventType getType() {
            return type;
        }

        public Node getParent() {
            return parent;
        }

        public Node getNode() {
            return node;
        }
    }

    protected class NodeEventListener implements EventListener<NodeEvent> {
        @Override
        public void onEvent(NodeEvent event) {
            Node current = event.getNode();
            //得到父节点
            Node parent = event.getParent();
            if (parent == root) {
                parent = null;
            }
            listenerLock.readLock().lock();
            try {
                if (event.type == NodeEventType.ADD) {
                    createPathEvent(parent, current, PathEvent.PathEventType.CREATED);
                    createChildrenEvent(parent, current, ChildrenEvent.ChildrenEventType.CHILD_CREATED);
                    createChildrenDataEvent(parent, current, ChildrenEvent.ChildrenEventType.CHILD_CREATED);
                    createClusterEvent(parent, current);
                    createLeaderEvent(parent, current);
                } else if (event.type == NodeEventType.UPDATE) {
                    createPathEvent(parent, current, PathEvent.PathEventType.UPDATED);
                    createChildrenDataEvent(parent, current, ChildrenEvent.ChildrenEventType.CHILD_UPDATED);
                } else if (event.type == NodeEventType.DELETE) {
                    createPathEvent(parent, current, PathEvent.PathEventType.REMOVED);
                    createChildrenEvent(parent, current, ChildrenEvent.ChildrenEventType.CHILD_REMOVED);
                    createChildrenDataEvent(parent, current, ChildrenEvent.ChildrenEventType.CHILD_REMOVED);
                    createClusterEvent(parent, current);
                    createLeaderEvent(parent, current);
                }
            } finally {
                listenerLock.readLock().unlock();
            }
        }
    }

}
