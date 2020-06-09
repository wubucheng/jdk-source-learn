---
title: CountDownLatch源码分析

---

## 概念与作用
> CountDownLatch是JUC包下提供的一个工具类，它的作用是让一个或者一组线程等待其他线程执行完成后，自己再接着执行，count数不为0则线程进行等待。

## API
- await：执行等待，计数不为0则进入等待
- await(long timeout, TimeUnit unit)：超时则自动唤醒，继续往下走
- coutDown：计数减一
- getCount：获取当前计数的值

## 源码分析
### 内部实现
> 底层使用一个Sync内部类实现，改类继承了AbstractQueuedSynchronizer

### 

- Sync构造方法
```java
  /**
         * 在构造方法中设置同步变量state的值
         *
         * @param count
         */
        Sync(int count) {
            setState(count);
        }

```
- 获取共享锁
```java
   /**
         * 尝试在获取共享锁
         *
         * @param acquires
         * @return
         */
        protected int tryAcquireShared(int acquires) {
            // state为0返回1，否则返回-1
            return (getState() == 0) ? 1 : -1;
        }
```
- 释放共享锁
```java
        /**
         * 尝试释放锁
         * 该方法的调用实际是CountDownLatch调用countDonwn后使用AQS中的releaseShared
         * @param releases
         * @return
         */
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0)
                    // state已为0则返回失败
                    return false;
                int nextc = c - 1;
                // 通过CAS设置状态
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
```
### 核心方法：await方法
```java
  public void await() throws InterruptedException {
        // 使用的是AQS的acquireSharedInterruptibly方法
        sync.acquireSharedInterruptibly(1);
    }
```
该方法内部调用了AQS的acquireSharedInterruptibly方法：
```java
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }
```
acquireSharedInterruptibly方法分两步进行分析
- tryAcquireShared：实际调用的是java.util.concurrent.CountDownLatch.Sync#tryAcquireShared，该方法上面有提及到
```java
   protected int tryAcquireShared(int acquires) {
            // state为0返回1，否则返回-1
            return (getState() == 0) ? 1 : -1;
        }
```

- doAcquireSharedInterruptibly
```java
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
		// 新建并将共享节点加入到等待队列
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
				// 或取前驱节点
                final Node p = node.predecessor();
                if (p == head) {
				   // 当前节点是队列中等待的第一个节点则尝试获取锁
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
					   // r>=0了获取到锁，设置当前节点为头结点
                        setHeadAndPropagate(node, r);
						// 在队列中删除原头结点
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
				// 判断获取失败后是否需要等待并进行中断检查
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
接着来看一下setHeadAndPropagate方法：
```java
  private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
		// 设置头结点
        setHead(node);
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())
				// 释放锁
                doReleaseShared();
        }
    }
```
propagate > 0 说明state已经为0了；如果propagate > 0或者头结点为空，或头结点的等待状态小于0，则获取该节点的后继节点，并判断如果没有后继节点或者后继节点为共享模式，则调用doReleaseShared方法，doReleaseShared方法如下：
```java
   private void doReleaseShared() {
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
			  // 获取头节点的等待状态
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
				    // 如果状态为SIGNAL则进行CAS更新
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
						// 释放后继节点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
						 //  如果等待状态为0，且节点状态为PROPAGATE，则继续循环
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
```
这里顺便提一下Node的状态：
-   CANCELLED =  1: 被取消，当线程等待超时或被中断
-   SIGNAL    = -1：通知，当前线程释放了，通知后继节点
-   CONDITION = -2：节点处于等待队列中，调用signal方法后，节点转移到同步队列中，加入到同步状态的获取中
 -  PROPAGATE = -3; 下一次共享状态将会被无条件传播下去

### 核心方法：countDown方法
```java
   public void countDown() {
        sync.releaseShared(1);
    }
```
该方法调用的AQS的releaseShared方法：
```java
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
```
同样，这里使用到tryReleaseShared方法和doReleaseShared方法，下面做分析：
- tryReleaseShared 尝试获取共享锁
```java
    protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0)
                    // state已为0则返回失败
                    return false;
                int nextc = c - 1;
                // 通过CAS设置状态
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
```
- doReleaseShared：执行释放锁
```java
 private void doReleaseShared() {
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
				  // 如果头结点的状态为SIGNAL，进行CAS更新
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
						// 释放后继节点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
```

前面都有提到的unparkSuccessor方法如下：
```java
   private void unparkSuccessor(Node node) {
     	// 获取节点状态
        int ws = node.waitStatus;
        if (ws < 0)
		  // 节点状态小于0，则该节点的状态可能为：SIGNAL、CONDITION、PROPAGATE
            compareAndSetWaitStatus(node, ws, 0);
		
		
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
		   // 下一个节点的等待状态为CANCELLED或为空，则先置空
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
				   // 后循环释放节点的状态为：SIGNAL、CONDITION、PROPAGATE的后继节点
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }
```	