# ReentrantLock

## 概念

> ReentrantLock是一个可重入的独占锁

![ReentrantLock.png](https://i.loli.net/2020/07/02/1Y6tSikGXEyIT7g.png)



内部类Sync有两个子类NonfairSync和FairSync，分别表示获取锁的非公平策略和公平策略



## 获取锁

### lock方法

获取到锁将AQS的state值修改为1，如果该线程之前已经获取锁了，state会进行加一，获取锁失败会将该线程存放到AQS阻塞队列。

ReentrantLock的lock方法实际上调用器内部类Sync的lock方法，而在Sync内部根据实现策略的不同，公平锁则使用的是FairSync的lock方法，非公平锁则调用的是NonfairSync的lock方法。

```java
public void lock() {
    sync.lock();
}
```

```java
final void lock() {
    // 通过CAS设置状态值
    if (compareAndSetState(0, 1))
        // 设置成功则设置持有锁线程为当前线程
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);
}
```

如果获取锁失败则会调用acquire方法：

```java
public final void acquire(int arg) {
    // tryAcquire失败则把当前线程放入到AQS阻塞队列
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
    
}
```

**在非公平锁中**，tryAcquire方法实现如下:

```java
protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}
```

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 设置状态值
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }// 如果当前线程是该锁持有者
    else if (current == getExclusiveOwnerThread()) {
        // 则直接将状态值加一
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

**在公平锁中**，tryAcquire方法实现如下：

```java
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            // 如果当前状态值为0，并且前面没有等待的队列，则设置持有锁的线程为当前线程
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
}
```

与非公平锁相比，多了一步hasQueuedPredecessors的判断：

```java
public final boolean hasQueuedPredecessors() {
    Node t = tail; 
    Node h = head;
    Node s;
    // 头结点与为节点不相等，并且第一个元素不是当前元素
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

### lockInterruptibly方法

与lock比较，增加了对中断响应的功能。

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
}
```

```java
public final void acquireInterruptibly(int arg)
        throws InterruptedException {
    // 如果线程被中断，则直接抛出异常
    if (Thread.interrupted())
        throw new InterruptedException();
    // 尝试获取资源
    if (!tryAcquire(arg))
        // 调用AQS的可被中断的方法
        doAcquireInterruptibly(arg);
}
```

```java
private void doAcquireInterruptibly(int arg)
    throws InterruptedException {
    // 在AQS阻塞队列中添加一个节点
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        // 自璇
        for (;;) {
            // 获取前驱节点
            final Node p = node.predecessor();
            // 如果前驱节点是头结点并且获取锁成功，则将新添加的节点作为头节点，原来的头节点移除掉
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return;
            }
            // 如果失败后需要挂起
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



### tryLock方法

尝试获取锁，获取成功返回true，否则false，不会进行阻塞。

```java
public boolean tryLock() {
    return sync.nonfairTryAcquire(1);
}
```

它调用的前面提交的非公平锁的nonfairTryAcquire方法。



### tryLock(long timeout, TimeUnit unit)方法

与tryLock方法比较，增加超时返回机制，在指定时间内没有获取到锁将不会阻塞下去，会返回执行下一步操作。



## 释放锁

### unlock方法

如果该线程持有锁，则将状态值减一，减后的状态值为0则释放锁。

```java
public void unlock() {
    sync.release(1);
}
```

他调用java.util.concurrent.locks.AbstractQueuedSynchronizer#release的方法，而该方法里面调用了ReetrantLock的tryAcquire方法：

```java
protected final boolean tryRelease(int releases) {
    // 状态值减一
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    // 释放锁的标记
    boolean free = false;
    if (c == 0) {
        free = true;
        // 清空持有锁的线程信息
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```