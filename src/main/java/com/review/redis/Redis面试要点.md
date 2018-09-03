#### 1、Redis支持的数据结构
 - string：String是最常用的一种数据类型，普通的key/value存储都可以归为此类。
 - hash：Redis的Hash实际是内部存储的Value为一个HashMap，并提供了直接存取这个Map成员的接口。
 - list：list的实现为一个双向链表，即可以支持反向查找和遍历，更方便操作，不过带来了部分额外的内存开销，Redis内部的很多实现，包括发送缓冲队列等也都是用的这个数据结构。
 - set：set 的内部实现是一个 value永远为null的HashMap，实际就是通过计算hash的方式来快速排重的。
 - sorted set：与set类似，区别是set不是自动有序的，而sorted set可以通过用户额外提供一个优先级(score)的参数来为成员排序，并且是插入有序的，即自动排序。
 - HyperLogLog（2.8.9 版本新增）： HyperLogLog 是用来做基数统计的算法（即计算某个数据集中不重复元素的个数），其优点是在输入元素的数量或者体积非常非常大时，计算基数所需的空间总是固定的、并且是很小的。在 Redis 里面，
        每个 HyperLogLog 键只需要花费 12 KB 内存，就可以计算接近 2^64 个不同元素的基数。但 HyperLogLog 只会根据输入元素来计算基数，而不会储存输入元素本身，所以 HyperLogLog 不能像集合那样，返回输入的各个元素。
 
        `什么是基数：比如数据集 {1, 3, 5, 7, 5, 7, 8}， 那么这个数据集的基数集为 {1, 3, 5 ,7, 8}, 基数(不重复元素)为5。 `
 
#### 2、Redis删除策略
 - 定时删除：创建一个定时器，定时器在过期时间来临时，立即删除
 - 惰性删除：放任键的过期时间不管，但每次获取元素时都会检查是否过期。过期则删
    - 配置：服务器内置策略
 - 定期删除：每隔一段时间，程序就对数据库进行一次检查，删除过期键。
    - 配置：redis.conf  hz 刷新频率  maxmemory：当内存超过限定，主动触发清除策略

#### 3、主从复制及哨兵机制
redis的复制功能是支持多个数据库之间的数据同步。一类是主数据库（master）一类是从数据库（slave），主数据库可以进行读写操作，当发生写操作的时候自动将数据同步到从数据库，而从数据库一般是只读的，并接收主数据库同步过来的数据。
通过redis的复制功能可以很好的实现数据库的读写分离，提高服务器的负载能力。主数据库主要进行写操作，而从数据库负责读操作。

哨兵机制：[Redis Sentinel 介绍与部署（哨兵机制）](http://207.246.80.156/?p=423)

#### 4、常见问题
- 穿透
   - 原理：频繁查询一个不存在的数据，由于缓存不命中，导致每次都要查库
   - 解决：持久层查不到数据就缓存空结果，并设置适当的过期时间
   
- 雪崩
   - 原理：缓存雪崩是由于原有缓存失效(过期)，新缓存未到期间。所有请求都去查询数据库，而对数据库CPU和内存造成巨大压力，严重的会造成数据库宕机。从而形成一系列连锁反应，造成整个系统崩溃。
   - 解决：
      - 1.碰到这种情况，一般并发量不是特别多的时候，使用最多的解决方案是加锁排队。加锁排队只是为了减轻数据库的压力，并没有提高系统吞吐量。假设在高并发下，缓存重建期间key是锁着的，这是过来1000个请求999个都在阻塞的。
      同样会导致用户等待超时，这是个治标不治本的方法。
      - 2.给每一个缓存数据增加相应的缓存标记，记录缓存的是否失效，如果缓存标记失效，则更新数据缓存。缓存标记用来记录缓存数据是否过期，如果过期会触发通知另外的线程在后台去更新实际key的缓存。缓存数据：它的过期时间
      比缓存标记的时间延长1倍，例：标记缓存时间30分钟，数据缓存设置为60分钟。 这样，当缓存标记key过期后，实际缓存还能把旧数据返回给调用端，直到另外的线程在后台更新完成后，才会返回新缓存。
      
      
  #### 5、Redis和MenCached比较
  - 网络I/O模型
    - Memcached是非阻塞IO复用模型
    - Redis也是非阻塞IO复用模型，但由于redis还提供一些非KV存储之外的排序，聚合功能，在执行这些功能时，复杂的CPU计算，会阻塞整个IO调度。
  - 线程模型
    - Memcached是多线程的，分为监听主线程和worker子线程，监听线程监听网络连接，接受请求后将连接描述字pipe 传递给worker线程进行读写I/O。执行读写过程中，可能存在锁冲突。
    - Redis是单线程的,虽无锁冲突，但难以利用多核的特性提升整体吞吐量。
  - 内存管理
    - Memcached使用预分配内存池的方式，使用slab和大小不同的chunk来管理内存，Item根据大小选择合适的chunk存储。
    - Redis使用临时申请内存的方式来存储数据，并且很少使用free-list等方式来优化内存分配，会在一定程度上存在内存碎片。
  - 数据一致性问题
    - Memcached提供了cas命令，可以保证多个并发访问操作同一份数据的一致性问题。
    - Redis是单线程的，提供了事务的功能，可以保证一串命令的原子性，中间不会被任何操作打断。
  - 数据容灾性
    - Redis实现了持久化（性能损耗）和主从同步，并提供哨兵机制保证高可用
    - Memcached只是存放在内存中，服务器故障关机后数据就会消失
  - 存储方式及其它方面
    - Memcached基本只支持简单的key-value存储
    - Redis除key/value之外，还支持list,set,sorted set,hash等众多数据结构，提供了KEYS进行枚举操作，但不能在线上使用。
    
    
#### 6、Redis如何保证缓存的都是热点数据（6中内存淘汰机制）
- volatile-lru：从已设置过期时间的数据集（server.db[i].expires）中挑选最近最少使用的数据淘汰
- volatile-ttl：从已设置过期时间的数据集（server.db[i].expires）中挑选将要过期的数据淘汰
- volatile-random：从已设置过期时间的数据集（server.db[i].expires）中任意选择数据淘汰
- allkeys-lru：从数据集（server.db[i].dict）中挑选最近最少使用的数据淘汰
- allkeys-random：从数据集（server.db[i].dict）中任意选择数据淘汰
- no-enviction（驱逐）：禁止驱逐数据

#### 7、 Redis实现分布式锁
- Redis 2.8之前版本，使用setNx方法设置分布式锁，并使用expire方法设置过期时间，但是setNx与expire不是一个原子操作，存在风险
- Redis 2.8扩展了set方法，使得setNx与expire可以原子地执行，命令为 SET key value NX EX max-lock-time 或 SET key value NX PX max-lock-time
- 可重入锁：基于ThreadLocal和引用计数器，对Redis进行简单地封装，从而实现可重入锁。但它加重了客户端的复杂性，在编写业务方法时注意在逻辑结构上进行调整完全可以不使用可重入锁。
```java
public class RedisWithReentrantLock {

  private ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

  private Jedis jedis;

  public RedisWithReentrantLock(Jedis jedis) {
    this.jedis = jedis;
  }

  private boolean _lock(String key) {
    return jedis.set(key, "", "nx", "ex", 5L) != null;
  }

  private void _unlock(String key) {
    jedis.del(key);
  }

  private Map<String, Integer> currentLockers() {
    Map<String, Integer> refs = lockers.get();
    if (refs != null) {
      return refs;
    }
    lockers.set(new HashMap<>());
    return lockers.get();
  }

  public boolean lock(String key) {
    Map<String, Integer> refs = currentLockers();
    Integer refCnt = refs.get(key);
    if (refCnt != null) {
      refs.put(key, refCnt + 1);
      return true;
    }
    boolean ok = this._lock(key);
    if (!ok) {
      return false;
    }
    refs.put(key, 1);
    return true;
  }

  public boolean unlock(String key) {
    Map<String, Integer> refs = currentLockers();
    Integer refCnt = refs.get(key);
    if (refCnt == null) {
      return false;
    }
    refCnt -= 1;
    if (refCnt > 0) {
      refs.put(key, refCnt);
    } else {
      refs.remove(key);
      this._unlock(key);
    }
    return true;
  }

  public static void main(String[] args) {
    Jedis jedis = new Jedis();
    RedisWithReentrantLock redis = new RedisWithReentrantLock(jedis);
    System.out.println(redis.lock("codehole"));
    System.out.println(redis.lock("codehole"));
    System.out.println(redis.unlock("codehole"));
    System.out.println(redis.unlock("codehole"));
  }

}
```

#### 8、 Redis实现延时队列
延时队列可以通过 Redis 的 zset(有序列表) 来实现。我们将消息序列化成一个字符串作为 zset 的value，这个消息的到期处理时间作为score，然后用多个线程轮询 zset 获取到期的任务进行处理，
多个线程是为了保障可用性，万一挂了一个线程还有其它线程可以继续处理。因为有多个线程，所以需要考虑并发争抢任务，确保任务不能被多次执行。

Redis 的 zrem 方法是多线程多进程争抢任务的关键，它的返回值决定了当前实例有没有抢到任务，因为 loop 方法可能会被多个线程、多个进程调用，同一个任务可能会被多个进程线程抢到，通过 zrem 来决定唯一的属主。
同时，我们要注意一定要对 handle_msg 进行异常捕获，避免因为个别任务处理问题导致循环异常退出。
```java
import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import redis.clients.jedis.Jedis;

public class RedisDelayingQueue<T> {

  static class TaskItem<T> {
    public String id;
    public T msg;
  }

  // fastjson 序列化对象中存在 generic 类型时，需要使用 TypeReference
  private Type TaskType = new TypeReference<TaskItem<T>>() {
  }.getType();

  private Jedis jedis;
  private String queueKey;

  public RedisDelayingQueue(Jedis jedis, String queueKey) {
    this.jedis = jedis;
    this.queueKey = queueKey;
  }

  public void delay(T msg) {
    TaskItem<T> task = new TaskItem<T>();
    task.id = UUID.randomUUID().toString(); // 分配唯一的 uuid
    task.msg = msg;
    String s = JSON.toJSONString(task); // fastjson 序列化
    jedis.zadd(queueKey, System.currentTimeMillis() + 5000, s); // 塞入延时队列 ,5s 后再试
  }

  public void loop() {
    while (!Thread.interrupted()) {
      // 只取一条
      Set<String> values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
      if (values.isEmpty()) {
        try {
          Thread.sleep(500); // 歇会继续
        } catch (InterruptedException e) {
          break;
        }
        continue;
      }
      String s = values.iterator().next();
      if (jedis.zrem(queueKey, s) > 0) { // 抢到了
        TaskItem<T> task = JSON.parseObject(s, TaskType); // fastjson 反序列化
        this.handleMsg(task.msg);
      }
    }
  }

  public void handleMsg(T msg) {
    System.out.println(msg);
  }

  public static void main(String[] args) {
    Jedis jedis = new Jedis();
    RedisDelayingQueue<String> queue = new RedisDelayingQueue<>(jedis, "q-demo");
    Thread producer = new Thread() {

      public void run() {
        for (int i = 0; i < 10; i++) {
          queue.delay("codehole" + i);
        }
      }

    };
    Thread consumer = new Thread() {
    
      public void run() {
        queue.loop();
      }

    };
    producer.start();
    consumer.start();
    try {
      producer.join();
      Thread.sleep(6000);
      consumer.interrupt();
      consumer.join();
    } catch (InterruptedException e) {
    }
  }
}
```

#### 9、Redis实现简单限流
使用Redis限流，一般采用string类型存储一个数字并设置过期时间，每次请求递增，在达到阈值时进行错误提示。比如：每分钟最多访问10次，一个用户在第1秒时访问1次，在第59秒时访问9次，如果按照这种方式衡量，
则用户在第60秒时访问会提示“频繁操作”，但是在第61秒时访问时会重新计算，显然不是很合理。

上述方式无法很好地处理“滑动时间窗口”问题，可以通过 zset 数据结构的 score 值来圈出这个时间窗口。我们只需要保留这个时间窗口，窗口之外的数据都可以砍掉，然后统计 zset 数据结构的元素个数（阈值）即可。
若要保证统计的元素个数与用户请求个数不会出现偏差，需要保证 zset 的value唯一性，比如uuid（比较浪费空间）、毫秒时间戳等。

缺点：因为要记录时间窗口内所有的行为记录，如果这个量很大，比如限定 60s 内操作不得超过 100w 次这样的参数，它是不适合做这样的限流的，因为会消耗大量的存储空间。
```java
public class SimpleRateLimiter {

  private Jedis jedis;

  public SimpleRateLimiter(Jedis jedis) {
    this.jedis = jedis;
  }

  public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
    // 用户 + 操作，确保这两个维度下key唯一性
    String key = String.format("hist:%s:%s", userId, actionKey);
    long nowTs = System.currentTimeMillis();
    // 使用Redis管道命令，提高操作效率
    Pipeline pipe = jedis.pipelined();
    // 标记一个事务块的开始
    pipe.multi();
    // 添加元素
    pipe.zadd(key, nowTs, "" + nowTs);
    // 批量移除时间范围外的元素（比方说1分钟）
    pipe.zremrangeByScore(key, 0, nowTs - period * 1000);
    // 获取zset中的元素个数
    Response<Long> count = pipe.zcard(key);
    pipe.expire(key, period + 1);
    pipe.exec();
    pipe.close();
    // 判断是否超过阈值
    return count.get() <= maxCount;
  }

  public static void main(String[] args) {
    Jedis jedis = new Jedis();
    SimpleRateLimiter limiter = new SimpleRateLimiter(jedis);
    for(int i=0;i<20;i++) {
      System.out.println(limiter.isActionAllowed("laoqian", "reply", 60, 5));
    }
  }

}
```

