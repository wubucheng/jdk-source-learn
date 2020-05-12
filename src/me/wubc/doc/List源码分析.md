## List源码分析

### 成员变量
- DEFAULT_CAPACITY：初始化数组大小
- EMPTY_ELEMENTDATA：初始化使用的空数组
- DEFAULTCAPACITY_EMPTY_ELEMENTDATA：空数组示例
- elementData：存放数组元素
- size：当前数组大小，非线程安全
- MAX_ARRAY_SIZE：数组长度最大值，值为 **Integer.MAX_VALUE - 8 **，减8是为了防止内容溢出，有部分空间需要
记录数组长度


### 构造方法
无参构造：使用空数组进行复制
```java
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

```

指定容量大小：
```java
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }
```
- 参数大于0:elementData初始化为initialCapacity大小的数组
- 参数小于0:elementData初始化为空数组
- 参数小于0:抛出异常

参数为Collection类型的构造器：
```java
   public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                // 判断数组元素类型
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }
```

### add方法
添加指定元素：
```java
    public boolean add(E e) {
        // 添加数据时先进行扩容
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;
        return true;
    }
```
这里使用到了ensureCapacityInternal方法：深入分析可以知道其先调用了calculateCapacity方法，
计算出最小需要的容量大小
```java
    private static int calculateCapacity(Object[] elementData, int minCapacity) {
        /*
         *   如果是使用无参构造函数进行初始化，那么elementData与DEFAULTCAPACITY_EMPTY_ELEMENTDATA相等，此时容量为10
         *
         */
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity;
    }
```
然后使用ensureExplicitCapacity方法，判断是否需要进行扩容
```java
  private void ensureExplicitCapacity(int minCapacity) {
        // 记录修改次数
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            // 需要的容量比当前数组长度大才进行扩容
            grow(minCapacity);
    }
```
扩容方法grow：
```java
   private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;
        // 扩容后大小是原来容量的1.5倍
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        // 下面这几行是为了防止容量大小越界
        // 新容量大小比最小容量要小，那么新容量即为最小容量，场景是使用无参构造函数进行初始化
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
```

```java
   private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
```
通过分析可以得出：每次扩容都是原容量的1.5倍，新容量大小比最小容量要小，那么新容量即为最小容量。


### 指定位置插入元素
```java
  public void add(int index, E element) {
        // 校验index合法性
        rangeCheckForAdd(index);
        // 判断是否扩容
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        System.arraycopy(elementData, index, elementData, index + 1,
                         size - index);
        elementData[index] = element;
        size++;
    }
```
基本逻辑跟add(E e)相同，这里多了rangeCheckForAdd进行index合法性判断。数组未满插入元素，index后的元素
都需要向后移动一位。

### 指定下标删除元素
```java
    public E remove(int index) {
        rangeCheck(index);

        modCount++;
        // 从数组中查询出要删除的数据
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        // 如果这个值大于0 说明后续有元素需要左移
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // clear to let GC do its work

        return oldValue;
    }
```
数组往前移一位，将最后面的元素置为空，并返回删除的值。

### 指定删除元素
```java
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }
```
删除指定元素，list是允许存储null值的，因此也可以删除null值