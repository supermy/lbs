# MyLBS

[![Build Status](https://travis-ci.org/supermy/lbs.svg?branch=master)](https://github.com/supermy/lbs)

## 简介 
* MyLBS 司机端实时跟踪司机GPS 位置信息；乘客发送订单信息，携带位置与目的地信息推送到起始位置直径1KM 范围的司机；司机抢单，
抢单成功的司机信息推送给司机与乘客。


## 特点
* 采用 netty 支持百万并发；
* 位置计算采用原生的 mongodb 的算法；
* 采用 mongodb 进行数据的存储；
* 完成一套独立的指令集；
* 支持司机抢单；

## 适用场景

    高并发的消息引擎




## 消息机制
    单消息长度,放入配置文件;
    
    backlog指定了内核为此套接口排队的最大连接个数，对于给定的监听套接口，内核要维护两个队列，未链接队列和已连接队列，
    根据TCP三路握手过程中三个分节来分隔这两个队列。服务器处于listen状态时收到客户端syn 分节(connect)时在未完成队列
    中创建一个新的条目，然后用三路握手的第二个分节即服务器的syn 响应及对客户端syn的ack,此条目在第三个分节到达前(客户
    端对服务器syn的ack)一直保留在未完成连接队列中，如果三路握手完成，该条目将从未完成连接队列搬到已完成连接队列尾部。
    当进程调用accept时，从已完成队列中的头部取出一个条目给进程，当已完成队列为空时进程将睡眠，直到有条目在已完成连接队
    列中才唤醒。backlog被规定为两个队列总和的最大值，大多数实现默认值为5，但在高并发web服务器中此值显然不够，lighttpd
    中此值达到128*8 。需要设置此值更大一些的原因是未完成连接队列的长度可能因为客户端SYN的到达及等待三路握手第三个分节的
    到达延时而增大。Netty默认的backlog为100，当然，用户可以修改默认值，用户需要根据实际场景和网络状况进行灵活设置。
    
    多线程netty->mongodb 压力测试ok,10个线程每个线程产生8000条数据,间隔3毫秒.
    
## 启动命令

nohup java  -server -Xmx3G -Xms3G -Xmn600M \
-XX:PermSize=50M -XX:MaxPermSize=50M -Xss256K -XX:+DisableExplicitGC -XX:SurvivorRatio=1 -XX:+UseConcMarkSweepGC \
-XX:+UseParNewGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=0 \
-XX:+CMSClassUnloadingEnabled -XX:LargePageSizeInBytes=128M -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly \
-XX:CMSInitiatingOccupancyFraction=80 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+PrintClassHistogram -XX:+PrintGCDetails \
-XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -Xloggc:gc.log  \
-Djava.rmi.server.hostname=192.168.6.61 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8091 \
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false \
-jar netty-spring-im-1.0-SNAPSHOT.jar  >server.out 2>&1 &
 
## 压力测试
ab -n 20000 -c 20000 -k -t 999999999 -r http://192.168.6.61:8090/
