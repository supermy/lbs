## 2016-05-30
        完成内存是否泄露测试;
        完成乘客取消订单逻辑,增加取消订单指令;
        
        首先我们先建立一下索引，数据会在30秒后删除
        db.ttl_collection.ensureIndex( { "time": 1 }, { expireAfterSeconds: 5} )
        db.ttl_collection.insert({"name":123, "time" : new Date()})
        
        db.location.drop()
        db.location.ensureIndex( {time:1}, {expireAfterSeconds: 5})
        db.location.ensureIndex( {position: "2d"} )
        db.location.getIndexKeys()
        db.location.save( {_id: "18610588588","time" : new Date(), position: [0.1, -0.1]} )
        db.location.find()
        
        
        redis-spring 调用优化完成;
        mongodb-spring 索引优化完成;
        

## 2016-05-28
        抢单代码重构ok 增加允许抢单范围的ID 过滤,增加keys 有效期过滤; 消费订单key 长期有效,可是通过第三方获取数据.
       
        订单id OK
        
        后续需要继续梳理抢单逻辑 ......
        
        flume 获取订单数据,存入到数据库,或者关系数据库.
        
        
## 2016-05-27
        乘客发起约车,司机抢单,逻辑完成;
        todo:乘客多次发起情况;数据失效情况;订单同步到DB;
        todo:订单的有效情况;
        todo:取消订单 
        todo:付款流程;接客流程;送客流程;


## 2016-05-26
    按指令集进行代码重构
    
        准备数据
        首先定义一个位置集合,给定a,b,c,d节点.
        > db.createCollection("location")
        { "ok" : 1 }
        > db.location.save( {_id: "18610588588", position: [0.1, -0.1]} )
        > db.location.save( {_id: "18610588587", position: [1.0, 1.0]} )
        > db.location.save( {_id: "18610588586", position: [0.5, 0.5]} )
        > db.location.save( {_id: "18610588589", position: [-0.5, -0.5]} )
         
        接着指定location索引
        db.location.ensureIndex( {position: "2d"} )
        现在我们可以进行简单的GEO查询
        
        查询point(0,0),半径0.7附近的点
        > db.location.find( {position: { $near: [0,0], $maxDistance: 0.7  } } )
        { "_id" : "A", "position" : [ 0.1, -0.1 ] }
        
        需要脚本设置数据的有效期,根据字段的时间;
        

## 2016-05-20
    接收LBS-Server 推送消息完成;
    测试通过;
    
    将乘客信息发送给司机;
    * 乘客的约车信息{司机电话|乘客姓名|乘客电话|乘客位置[经度|纬度]}: [p::18610586586;张三;18610586586;1462870826752;106.72;26.57]


## 2016-05-19
    
    接收乘客的约车信息;
    * 乘客的约车信息{司机电话|乘客姓名|乘客电话|乘客位置[经度|纬度]}: [p::18610586586;张三;18610586586;1462870826752;106.72;26.57]

    涉及到乘客信息的传递,信息需要加密;
    服务器端使用ssl 通道加密;
    
    id 与 channel 进行绑定,便于检索发送信息.
    
    todo: 是否使用pojo 替换 string 
    
## 2016-05-17
    mongodb 加上用户认证;

未启用验证之前,创建管理员账户;
 Built-In Roles（内置角色）：
    1. 数据库用户角色：read、readWrite;
    2. 数据库管理角色：dbAdmin、dbOwner、userAdmin；
    3. 集群管理角色：clusterAdmin、clusterManager、clusterMonitor、hostManager；
    4. 备份恢复角色：backup、restore；
    5. 所有数据库角色：readAnyDatabase、readWriteAnyDatabase、userAdminAnyDatabase、dbAdminAnyDatabase
    6. 超级用户角色：root  
    // 这里还有几个角色间接或直接提供了系统超级用户的访问（dbOwner 、userAdmin、userAdminAnyDatabase）
    7. 内部角色：__system
    PS：关于每个角色所拥有的操作权限可以点击上面的内置角色链接查看详情。


use admin
db.createUser(
  {
    user:"root",
    pwd:"123456",
    roles:["root"]
  }
)

use mydb
db.createUser(
   {
     user: "mydb",
     pwd: "123456",
     roles: [ "readWrite", "dbAdmin" ]
   }
)



创建一个超级用户
use admin
db.createUser(
  {
    user: "root",
    pwd: "123456",
    roles:
    [
      {
        roles: "userAdminAnyDatabase",
        db: "admin"
      }
    ]
  }
)

vim /usr/local/etc/mongod.conf
auth = true

db.auth( <username>, <password> )
> use admin
> db.auth("root","123456");


db.createUser(
  {
    user:"mydb",
    pwd:"123456",
    roles:[
      {role:"readwrite",db:"mydb"},
    ]
  }
)



## 2016-05-12
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
    
    todo 1m 测试
    
    认证,采用md5+salt+天 加密 id 进行认证
    

## 2016-05-11
#more /etc/sysctl.conf
#sysctl -p
fs.file-max = 1048576 
net.ipv4.ip_local_port_range = 1024 65535 
net.ipv4.tcp_mem = 786432 2097152 3145728 
net.ipv4.tcp_rmem = 4096 4096 16777216 
net.ipv4.tcp_wmem = 4096 4096 16777216 
net.ipv4.tcp_tw_reuse = 1 
net.ipv4.tcp_tw_recycle = 1

#/etc/security/limits.conf
* soft nofile 1048576
* hard nofile 1048576

#/etc/profile
#ulimit -u1048576
在Linux平台下，无论是64位或者32位的MongoDB默认最大连接数都是819

#db.serverStatus().connections

update-alternatives --config java
update-alternatives --config javac
  

nohup java  -server -Xmx3G -Xms3G -Xmn600M \
-XX:PermSize=50M -XX:MaxPermSize=50M -Xss256K -XX:+DisableExplicitGC -XX:SurvivorRatio=1 -XX:+UseConcMarkSweepGC \
-XX:+UseParNewGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=0 \
-XX:+CMSClassUnloadingEnabled -XX:LargePageSizeInBytes=128M -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly \
-XX:CMSInitiatingOccupancyFraction=80 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+PrintClassHistogram -XX:+PrintGCDetails \
-XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -Xloggc:gc.log  \
-Djava.rmi.server.hostname=192.168.6.61 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8091 \
-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false \
-jar netty-spring-im-1.0-SNAPSHOT.jar  >server.out 2>&1 &
 

 
mongostat -h 192.168.6.53

java -jar client.jar >client.log 2>&1 &

ss -s ;free 
迅速 telnet 192.168.6.61 8090
ss -s ;free
一个新的链接维护大概8k.

内存使用:
1118004
连接上:1118136=+132
发送心跳:1118640


buffer:
822404
连接上:822520=+116
心跳:822684

ab -n 20000 -c 20000 -k -t 999999999 -r http://192.168.6.61:8090/


## 2016-05-10
* 完成im netty-springboot-im基本框架;
* 完成异步 mongodb 调用,插入数据;
* 更新位置指令[id|时间|经度|纬度]: [g::18610586586;1462870826752;106.72;26.57]

    fixme 内存不足 netty-mongodb 掉线.


## 2016-05-09晚
* 完成im netty-springboot-im基本框架;
* 完成异步 mongodb 调用;
todo 日志模式 mongodb  100万条;  ???

# Netty spring IM
TCP communication server with Netty And SpringBoot

This TCP Communication Service is a simple example for developer who want to make tcp service with Spring-Boot and Netty.


## Feature
* Telnet Client can send message to other telnet client.

## How to use
* Run com.zbum.example.socket.server.netty.Application with IDE or Maven
```
    $ mvn spring-boot:run
```
* Connect to this server by telnet command.
```
    $ telnet localhost 8090
    Trying ::1...
    Connected to localhost.
    Escape character is '^]'.
    Your channel key is /0:0:0:0:0:0:0:1:57220
```
* Your channel key (ID) is 04e9b346-50ec-4810-bd59-6daba2cc6f54
* Connect to this server by telnet command on annother terminal.
```
    $ telnet localhost 8090
    Trying ::1...
    Connected to localhost.
    Escape character is '^]'.
    Your channel key is /0:0:0:0:0:0:0:1:57221
```
* From now, you can send message to 04e9b346-50ec-4810-bd59-6daba2cc6f54 channel by below
```
    /0:0:0:0:0:0:0:1:57220::I Love You!!!
```
* Then, you can receive Message like below
```bash
    $ telnet localhost 8090
    Trying ::1...
    Connected to localhost.
    Escape character is '^]'.
    Your channel key is /0:0:0:0:0:0:0:1:57220
    I Love You!!!
```


