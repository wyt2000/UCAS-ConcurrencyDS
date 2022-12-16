# 性能评测报告

吴钰同 2022E8013282172

## 设计思路

### 并发数据结构

主要包括以下四个：

```java
private AtomicLong tid;
private ConcurrentHashMap<Long, Ticket> tickets;
private AtomicLong[][] seats;
private BitMap[][] stations;
```

- `tid`: 车票编号，使用原子变量以保证每次购买到的车票编号不同。

- `tickets`: 从车票编号到已购车票的映射，用于保证退票的时候退的是合法的票。为处理退票之间的并发，使用 `ConcurrentHashMap` 。

- `seats`: 大小为 `routenum * coachnum * seatnum` 的原子数组，用于表示哪辆车的哪个座位从哪个站到哪个站的票已经卖出去了，因为老师在群里说：

  >64线程，50列车，20个车厢，每个车厢100个座位，30个站，每个线程100万条操作。买票30%，退票10%，查票60%。如果时间来不及，也可能是每个线程10万条操作。

  故站的数量不超过 64，使用一个 `Long` 就能存得下，如果超过 64 则需要使用 `Long` 数组，详见 `BitMap` 。

- `stations`： 大小为 `routenum * stationnum` 的原子数组，用于表示哪辆车在哪个站的哪些座位的票已经卖出去了，该数组用于加速查询。由于一个车最多有 2000 个座位，所以要用很多 `Long` 拼接起来表示，因此使用自定义的类 `BitMap`，其接口包括置位、清零、计算 1 的个数和找到第一个 0 。

### buyTicket

先计算 `[departure, arrival)` 之间所有 `stations` 对应位向量的按位或，该结果的某一位为 0 表示该座位在这些站都是空的。然后找到第一个 0 对应的下标，尝试购买该位置的票，这一过程不需要加锁，因为后面会用 `seats` 验证该座位是否真是空的。

若该下标超过座位总数，则说明票卖完了，返回 `null`。否则通过将 `seats` 的对应分量和只有 `[departure, arrival)` 对应的位为 1 的 `bitmask` 按位与，结果为 0 则说明该座位确实在这一段是空闲的。为了防止对 `seats` 的并发修改，使用 `CAS` 原语将这些位清零，如果修改失败，则重新判断该座位是否空闲，如果空闲则尝试继续买票，否则重新寻找第一个 0 对应的下标。

购票成功后将 `stations` 的 `[departure, arrival)` 位设为 1，该过程不是原子过程，需要加锁。由于车次之间互不干扰，故只需对每个 `stations[route]` 加细粒度锁。

最后递增 `tid` 并将生成的车票加入 `tickets` 映射。

### refundTicket

首先判断 `tickets` 中是否包含要退的车票，然后尝试删除该车票，如果删除失败，则说明有并发的退票已经删除了，直接返回即可。否则计算座位的下标，分别尝试修改 `seats` 和 `stations`，过程和 `buyTicket` 类似。

### inquiry

同样计算 `[departure, arrival)` 之间所有 `stations` 对应位向量的按位或，然后统计其中 1 的个数，表示卖出去的座位，用总座位数减去 1 的个数就是余票的数量。

### 多线程测试程序

主要参考了 `GenerateHistory.java`，按比例随机发送三种请求。对于延迟和吞吐量的计算，为了减少 cache 的影响，把相同线程数的测试反复运行 10 次，取后 6 次的平均值作为结果。

## 正确性

考察 3 个方法之间的并发：

### buyTicket 和 buyTicket 并发

`buyTicket` 退出循环时有两种情况：

- `stations` 按位或的结果全为 1：此时该区间没有余票。这说明其他 `buyTicket` 对 `stations` 的修改先于它执行，而同一个 `buyTicket` 方法内对 `seats` 的修改又先于对 `stations` 的修改，从而说明本次调用的 `buyTicket` 已经把能占的座位的 `seats` 都修改成 1 了。
- 修改 `seats` 的 `CAS` 原语返回 `true`：由于 `oldValue` 的计算保证了要占的座位在该区间都是 0，如果 `CAS` 成功，则说明在获取 `oldValue` 到置位之间没有修改该座位任意区间的并发 `buyTicket` 操作，而且在进入 `while` 循环到获取 `oldValue` 之间没有修改该区间的并发 `buyTicket` 操作，故修改 `seats` 的操作是原子且合法的。

综上所述，两个对相同座位操作的并发 `buyTicket` 方法可以按照成功执行的 `CAS` 原语定序，如果某个方法返回 `null`，则可以把该方法定序到所有并发执行的 `buyTicket` 方法之后。

### buyTicket 和 refundTicket 并发

由于对 `tickets` 映射的修改是原子的，故可以根据并发的 `buyTicket` 是否执行到 `tickets.put(t.tid, t)` 来确定已经出票的 `buyTicket` 和 `refundTicket` 之间的顺序。

对于还在搜索空闲座位的 `buyTicket`，它也可能受到并发的 `refundTicket` 的影响，注意到：`refundTicket` 先修改 `seats`，再修改 `stations`，而 `buyTicket` 先查找 `stations`，再修改 `seats`，故当 `buyTicket` 发现 `stations` 的第一个 0 的时候，并发的 `refundTicket` 已经完成了，后面的 `CAS` 原语修改 `seats` 一定可以成功。故可以按照修改 `stations` 的顺序（这是一个原子过程，因为加了锁）为 `buyTicket` 和 `refundTicket` 定序。

### buyTicket 和 inquiry 并发

当且仅当 `buyTicket` 对 `stations` 的修改完成后，占下的座位才对 `inquiry` 可见，故可以按照 `buyTicket` 修改 `stations` 和 `inquiry` 访问 `stations` 的顺序为它们定序。

### refundTicket 和 refundTicket 并发

如果两个并发调用退同一张票，则按照 `tickets.remove(ticket.tid)` 定序。

### refundTicket 和 inquery 并发

同上，按照 `refundTicket` 修改 `stations` 和 `inquiry` 访问 `stations` 的顺序为它们定序。

### inquery 和 inquery 并发

不会冲突，因为`inquery` 没有写 `stations` 数组的操作。

## 进展性

因为每个方法在读写  `stations` 时都使用了锁，故它们都不是 starvation-free、lock-free 和 wait-free 的。又因为一个方法不能返回，只有可能是它被别的方法反复获取锁打断，故它们都是 deadlock-free 的。

## 性能

使用 `test.sh` 测试性能。

参数：车次数 5，车厢数 8，座位数 100，车站数 10，60% 查票，30% 购票，10% 退票，每个线程调用 10000 次。

| 线程数 | buyTicket (ns) | inquiry (ns) | refundTicket (ns) | 吞吐量(次/ms) |
| :----: | :------------: | :----------: | :---------------: | :-----------: |
|   4    |      3256      |     701      |       3465        |     10502     |
|   8    |      4237      |     890      |       3479        |     9547      |
|   16   |      6079      |     1294     |       3367        |     9065      |
|   32   |      4778      |     1088     |       4887        |     8333      |
|   64   |      7360      |     2907     |       6485        |     7383      |

## 参考资料

https://github.com/YLonely/concurrency-project 中使用 `BitMap` 优化。

https://github.com/qaqcxh/ticketingsystem 提供的可线性化检查程序。

