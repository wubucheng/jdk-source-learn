# TreeMap知识总结

### 底层实现
> 使用红黑树实现。利用了红黑树左节点小，右节点大的性质，根据 key 进行排序，使每个元
素能够插入到红黑树大小适当的位置，维护了 key 的大小关系。

### 成员属性
- comparator：比较器：如果有外部比较器过来，则使用外部比价器，否则使用自己实现compareTo 方法
- root：红黑树根节点
- size：红黑树节点个数

### put方法分析
```java
  public V put(K key, V value) {
        Entry<K,V> t = root;
        if (t == null) {
            compare(key, key); // type (and possibly null) check

            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K,V> parent;
        // split comparator and comparable paths
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                // 寻找合适的插入位置
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        else {
            if (key == null)
                throw new NullPointerException();
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        // 如果前没有找到相等的节点，则下面逻辑会执行
        // 最后判断插入的节点位置，以及插入新节点
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }
```
利用红黑树的特性，新增的值与当前红黑树节点的值进行比较，如果
中途中找到的节点的值与新增的值相等，那么将会进行覆盖，并返回，否则一直找到叶子节点为止。
最后判断插入的节点位置，以及插入新节点。

