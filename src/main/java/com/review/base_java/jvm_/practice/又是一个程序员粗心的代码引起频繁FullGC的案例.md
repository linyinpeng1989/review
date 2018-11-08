- 案例来源：[又是一个程序员粗心的代码引起频繁FullGC的案例](https://mp.weixin.qq.com/s?__biz=MzI5NTYwNDQxNA==&mid=2247484418&idx=1&sn=883b43106a4af5ce63772423834fb457&chksm=ec505dd3db27d4c5e479a686b4bf303c2e2302ec335db3ff5145d4b29d4b245982fbd2be0a7c&mpshare=1&scene=1&srcid=1108rAZdmaaLO6OW2vaQJCaA#rd)
- 问题现象：CPU飙高并且频繁FullGC

#### 1. 问题重现
模拟并简化业务场景，代码如下：[FullGCDemo.java](demo/FullGCDemo.java)

说明：为了更好地还原问题，FutureContract.java 的定义建议尽量与问题代码保持一致：
- 16个BigDecimal类型属性
- 3个Long类型属性
- 3个String类型属性
- 4个Integer类型属性
- 2个Date类型属性

运行参数：
```
[linyp@localhost demo]$ javac -cp ./ FullGCDemo.java
[linyp@localhost demo]$ java -cp ./ -Xmx256m -Xms256m -Xmn64m FullGCDemo
```

#### 2. CPU飙高
使用top命令，找到相应的Java应用的进程ID。
```
[linyp@localhost ~]$ top

top - 15:42:23 up 7 min,  2 users,  load average: 1.25, 0.96, 0.46
Tasks:  98 total,   1 running,  97 sleeping,   0 stopped,   0 zombie
%Cpu(s): 99.7 us,  0.2 sy,  0.0 ni,  0.0 id,  0.0 wa,  0.0 hi,  0.1 si,  0.0 st
KiB Mem :   997956 total,   352024 free,   441088 used,   204844 buff/cache
KiB Swap:  2097148 total,  2097148 free,        0 used.   376240 avail Mem

   PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
  1298 linyp     20   0 2302056 309800  12208 S 99.6 31.0   4:26.63 java
  1387 root      20   0       0      0      0 S  0.1  0.0   0:00.23 kworker/0:1
  ......
```

可以发现，对应的Java应用的进程ID为 1298。使用top -p 1298 查看某个进程的状态。
```
[linyp@localhost ~]$ top -p 1298

top - 15:44:56 up 10 min,  2 users,  load average: 1.30, 1.12, 0.60
Tasks:   1 total,   0 running,   1 sleeping,   0 stopped,   0 zombie
%Cpu(s): 99.8 us,  0.1 sy,  0.0 ni,  0.0 id,  0.0 wa,  0.0 hi,  0.1 si,  0.0 st
KiB Mem :   997956 total,   351256 free,   441856 used,   204844 buff/cache
KiB Swap:  2097148 total,  2097148 free,        0 used.   375472 avail Mem

   PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
  1298 linyp     20   0 2302056 310312  12208 S 99.8 31.1   6:58.84 java
```

然后执行命令 top -H -p1298 查看进程里的线程情况：
```
[linyp@localhost ~]$ top -H -p1298

top - 15:49:55 up 15 min,  2 users,  load average: 1.14, 1.11, 0.73
Threads:  60 total,   1 running,  59 sleeping,   0 stopped,   0 zombie
%Cpu(s): 99.8 us,  0.1 sy,  0.0 ni,  0.0 id,  0.0 wa,  0.0 hi,  0.1 si,  0.0 st
KiB Mem :   997956 total,   351512 free,   441460 used,   204984 buff/cache
KiB Swap:  2097148 total,  2097148 free,        0 used.   375776 avail Mem

   PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
  1300 linyp     20   0 2302056 310312  12208 R 99.6 31.1  11:37.69 java
  1298 linyp     20   0 2302056 310312  12208 S  0.0 31.1   0:00.00 java
  1299 linyp     20   0 2302056 310312  12208 S  0.0 31.1   0:00.93 java
  ......
```
上述结果可知线程 1300 很消耗CPI。将 1300 转为16进制，得到 514 。接下来通过执行命令 jstack -l 1298 > ./demo/p1298.log 导出线程栈信息（命令中是进程ID），并在线程dump文件中寻找16进制数 514，得到如下信息：
```
[linyp@localhost ~]$ jstack -l 1298 > ./demo/p1298.log
[linyp@localhost demo]$ less p1298.log | grep 514
"VM Thread" os_prio=0 tid=0x00007f737406e000 nid=0x514 runnable
```
可以看到占用CPU资源最大的是“VM Thread”，而VM Thread是使用C++定义的类，与用户创建的线程无关，它是JVM本身用来进行虚拟机操作的线程，比如GC。

因此可以确定，CPU飙高是由JVM频繁进行GC操作造成的，使用命令 jstat -gc <pid> <period> <times> 命令查看GC的信息，结果如下：
```
[linyp@localhost demo]$ jstat -gc 1298 1000 5
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
6528.0 6528.0 6528.0  0.0   52480.0  52480.0   196608.0   196608.0  5248.0 4690.1 640.0  478.3      47    1.330 4158  2336.775 2338.104
6528.0 6528.0 6528.0  0.0   52480.0  52480.0   196608.0   196608.0  5248.0 4690.1 640.0  478.3      47    1.330 4160  2338.247 2339.576
6528.0 6528.0 6528.0  0.0   52480.0  52480.0   196608.0   196608.0  5248.0 4690.1 640.0  478.3      47    1.330 4161  2338.835 2340.164
6528.0 6528.0 6528.0  0.0   52480.0  52480.0   196608.0   196608.0  5248.0 4690.1 640.0  478.3      47    1.330 4163  2339.898 2341.227
6528.0 6528.0 6528.0  0.0   52480.0  52480.0   196608.0   196608.0  5248.0 4690.1 640.0  478.3      47    1.330 4165  2340.958 2342.287
```
- S0C: Current survivor space 0 capacity (kB).
- S1C: Current survivor space 1 capacity (kB).
- S0U: Survivor space 0 utilization (kB).
- S1U: Survivor space 1 utilization (kB).
- EC: Current eden space capacity (kB).
- EU: Eden space utilization (kB).
- OC: Current old space capacity (kB).
- OU: Old space utilization (kB).
- MC: Metaspace capacity (kB).
- MU: Metacspace utilization (kB).
- CCSC: Compressed class space capacity (kB).
- CCSU: Compressed class space used (kB).
- YGC: Number of young generation garbage collection events.
- YGCT: Young generation garbage collection time.
- FGC: Number of full GC events.
- FGCT: Full garbage collection time.
- GCT: Total garbage collection time.

查看GC信息可知，Eden和Old都占满了，且不再发生YGC，但是却在频繁FGC，此时的应用已经不能处理任务，相当于假死了。

#### 3. 揪出真凶
现在基本可以确认是有对象没有释放导致堆内存溢出，即使发生FullGC也回收不了。准备dump进行分析看看Old区都是些什么妖魔鬼怪，执行如下命令dump进程信息。
```
[linyp@localhost ~]$ jmap -dump:format=b,file=./demo/heap1298.hprof 1298
```
可以使用jhat(Java heap Analyzes Tool)进行分析，但是更推荐 jprofiler进行分析。JProfiler是由ej-technologies GmbH公司开发的一款性能瓶颈分析工具。基于JProfiler发现，ScheduledThreadPoolExecutor的任务队列中包含大量RunnableScheduledFuture对象，占满了整个堆空间。
因此可以确定，问题代码为：
```
private static void buildBar() {
    List<FutureContract> futureContracts = getAllFutureConstract();
    futureContracts.forEach(futureContract -> {
        executor.scheduleWithFixedDelay(() -> {
            try {
                doFutureContract(futureContract);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2, 3, TimeUnit.SECONDS);
    });
}
```
为什么这块代码无法释放内存呢？这里用到了ScheduledThreadPoolExecutor定时调度，且每3秒执行一次。然而定时器中需要的参数来自外面的List<FutureContract>，这就会导致List<FutureContract>这个对象一直被一个定时任务引用，永远无法回收，
从而导致FutureContract对象不断晋升到Old区，直到占满Old区然后频繁进行FullGC。

#### 4. 解决问题
- 不使用ScheduledThreadPoolExecutor，而直接使用ThreadPoolExecutor即可
```
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 50, 0L , TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(128));
    ......
    private static void buildBar() {
        List<FutureContract> futureContracts = getAllFutureConstract();
        futureContracts.forEach(futureContract -> {
            executor.execute(() -> {
                try {
                    doFutureContract(futureContract);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
```
- 直接改成同步处理，不使用线程池
```
private static void buildBar() {
    List<FutureContract> futureContracts = getAllFutureConstract();
    futureContracts.forEach(futureContract -> {
        try {
            doFutureContract(futureContract);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
```


