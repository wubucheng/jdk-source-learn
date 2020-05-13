## LinkedList源码分析

### 成员属性
- first：首节点
- last：尾节点

他们都是内部类Node的对象，LinkedList是使用双向链表实现的。
```java
   private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```

### 从首节点新增节点的方法addFirst
```java
   public void addFirst(E e) {
        linkFirst(e);
    }
```
其内部使用了linkFirst这个方法：
```java
    /**
     * Links e as first element.
     * 从头部增加元素
     */
    private void linkFirst(E e) {
        final Node<E> f = first;
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null)
            // 原LinkedList为空，则新节点同为首节点和尾节点
            last = newNode;
        else
            f.prev = newNode;
        size++;
        modCount++;
    }
```
根据新增的元素构造一个新的节点newNode，并把first指针指向新节点，如果原链表没有
元素，那么首节点指针和尾节点指针都指向该新节点，否则将原首节点的前驱节点指向
新节点newNode。

### 从尾节点新增节点的方法add.
其方法实际调用了linkLast方法
```java
    /**
     * Links e as last element.
     * 从尾部增加元素
     */
    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null)
            // 如果是一个空的LinkedList,那么插入的节点即为头节点
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }
```
方法比较简单，如果LinedList没有节点，那么首尾节点都指向该newNode，否则将
原来的尾节点的后驱节点指向该新节点。

### 删除删除首节点方法remove
其内部调用了unlinkFirst方法
```java
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }
```




