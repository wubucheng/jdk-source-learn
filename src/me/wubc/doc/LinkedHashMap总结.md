# LinkedHashMap知识总结

### 特性
> 只能单向访问，主要通过迭代器进行访问

### 成员属性


### put方法分析
LinkedHashMap覆写了Map的put方法中调用的的newNode方法：
```java
  Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }
```
可以看到起调用了linkNodeLast方法，将节点加到链表最后
```java
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }
```

### 删除策略
LRU：最近最少访问策略

如果设置了LRU策略，每次get方法时会将访问的节点放到后面
```java
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            // 如果设置了LRU策略，将当前key已到链表尾部
            afterNodeAccess(e);
        return e.value;
    }
```

利用HashMap的put方法，调用afterNodeInsertion删除队头元素
```java
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        // removeEldestEntry控制删除策略
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            // 删除队头元素
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
```