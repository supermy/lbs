package com.supermy.im.netty.handler;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoCollection;
import com.supermy.im.utils.EncrUtil;
import com.supermy.im.mongo.MongoRepository;
import com.supermy.im.netty.domain.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.supermy.im.netty.domain.Cmd.tryGetOrderScript;


/**
 * 服务端 channel
 *
 * @author moyong
 */
@Component
@Qualifier("simpleChatServerHandler")
@ChannelHandler.Sharable
public class ImChatServerHandler extends SimpleChannelInboundHandler<String> { // (1)

    private static Logger logger = Logger.getLogger(ImChatServerHandler.class.getName());

    /**
     * A thread-safe Set  Using ChannelGroup, you can categorize Channels into a meaningful group.
     * A closed Channel is automatically removed from the collection,
     */
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

//    //用于存储设备和channel的绑定关系
//    private Map<String, Channel> deviceAndChannel= new HashMap<String, Channel>();
//    //用于存储Channel和Device的绑定关系，方便channel断开连接时，能快速地找到对应的设备和用户，并解除绑定关系
//    private Map<Channel, String> channelAndDevice= new HashMap<Channel, String>();

    /**
     * 绑定chanel 与 用户 id
     */
    private BiMap<String, Channel> driverAndDevice = HashBiMap.create(new HashMap<String, Channel>());
    private BiMap<String, Channel> passengerAndDevice = HashBiMap.create(new HashMap<String, Channel>());
    private BiMap<String, Document> idAndName = HashBiMap.create(new HashMap<String, Document>());


    //    private ExecutorService executorService = Executors.newFixedThreadPool(20);
    @Autowired
    org.springframework.core.env.Environment env;

    @Autowired
    @Qualifier("bipool")
    private ExecutorService executorService;

    @Autowired
    @Qualifier("mongoClient")
    private MongoClient mongoClient;

    @Autowired
    @Qualifier("mongoCollection")
    private MongoCollection mongoCollection;

    @Autowired
    @Qualifier("mongoCollectionTask")
    private MongoCollection mongoCollectionTask;

    @Autowired
    @Qualifier("mymongo")
    private MongoRepository mr;


    @Autowired
    StringRedisTemplate redisClient;

    @Autowired
    Jedis jedis;


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {  // (2)
        Channel incoming = ctx.channel();

        logger.debug("*******" + ctx.name());


        // Broadcast a message to multiple Channels
        channels.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 加入,总人数:" + channels.size() + "\n");

        channels.add(ctx.channel());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {  // (3)
        Channel incoming = ctx.channel();


        idAndName.remove(driverAndDevice.inverse().get(incoming));
        idAndName.remove(passengerAndDevice.inverse().get(incoming));

        //解除用户与频道的绑定
        driverAndDevice.inverse().remove(incoming);
        passengerAndDevice.inverse().remove(incoming);


        logger.debug("司机数量:" + driverAndDevice.size());
        logger.debug("乘客数量:" + passengerAndDevice.size());


        // Broadcast a message to multiple Channels
        channels.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 离开,总人数:" + channels.size() + "\n");

        // A closed Channel is automatically removed from ChannelGroup,
        // so there is no need to do "channels.remove(ctx.channel());"
    }


    /**
     * @param ctx
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
//        logger.debug("do channelRead0:"+s);

        //json 处理使用mongo 的document
        Channel incoming = ctx.channel();
        doRule(incoming, s);

//		for (Channel channel : channels) {
//            if (channel != incoming){
//                //channel.writeAndFlush("[" + incoming.remoteAddress() + "]" + s + "\n");
//            } else {
//            	channel.writeAndFlush("[you]" + s + "\n");
//            }
//        }


    }

    /**
     * 按规则处理指令
     *
     * @param s
     * @param incoming
     */
    private void doRule(Channel incoming, String s) {
        logger.debug("do rule:" + s);

        Document msg = Document.parse(s);
        String cmd = msg.getString("cmd");
        Document data = msg.get("data", Document.class);

        //异步处理各指令;
        switch (cmd) {

            case Cmd.CODE_NONE:
                //无法处理的指令
                incoming.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "空指令,ping 使用",
                        new Date().getTime(), "success", s) + "\n");
                break;

            case Cmd.BIND_DRIVER:
                bindDriver(incoming, data);
                break;

            case Cmd.BIND_PASSENGER:
                bindPassenger(incoming, data);
                break;

            case Cmd.SEND_GEO:
                sendGeo(incoming, data);
                break;

            case Cmd.POST_APPOINTMENT:
                postAppointment(incoming, data);
                break;

            case Cmd.SEND_ORDER:
                sendOrder(incoming, data);
                break;

            case Cmd.CANCEL_APPOINTMENT:

                cancelOrder(incoming, data);
                break;

            case Cmd.SEARCH_ORDER:

                searchOrder(incoming, data);
                break;


            case Cmd.SEND_MSG:
                break;

            default:
                //无法处理的消息,回复给消息发送者
                //im.msg={"cmd":"%s","msg":"%s", "time": "%s","status": "%s", "data": {"%s"}}
                incoming.writeAndFlush(String.format(mr.iMmsg, Cmd.SEND_MSG, "无法识别的指令格式...",
                        new Date().getTime(), "fail", s) + "\n");
                break;

        }
    }

    /**
     * 查询是否有未被抢的订单
     *
     * @param incoming
     * @param data
     */
    private void searchOrder(Channel incoming, Document data) {

    }

    /**
     * 取消订单
     *
     * @param incoming
     * @param data
     */
    private void cancelOrder(Channel passenger, Document data) {
        String orderId = data.getString("orderId");

        Long size = redisClient.opsForList().size(Cmd.ORDER_LIST + orderId);
        if (size <= 0) {
            passenger.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "订单已经被抢,不能取消",
                    new Date().getTime(), "failure", data.toJson()) + "\n");
        } else {

            try {
                logger.debug(Cmd.ORDER_LIST + orderId);
                redisClient.opsForList().rightPop(Cmd.ORDER_LIST + orderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            passenger.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "订单已经取消",
                    new Date().getTime(), "success", data.toJson()) + "\n");
        }
    }


    /**
     * 司机抢单
     *
     * @param driver
     * @param data
     */
    private void sendOrder(final Channel driver, Document data) {
        //司机抢单
        final String driverId = driverAndDevice.inverse().get(driver);
        logger.debug(driverId);

        final String orderId = data.getString("orderId");
        //解密,获取订单乘客id
        String orderIdSource = EncrUtil.decrypt(orderId);
        final String passengerId = orderIdSource.split(":")[0];
        logger.debug(passengerId);


        //判断红包是否已经被抢  异步线程抢单
        StopWatch watch = new StopWatch();
        final CountDownLatch latch = new CountDownLatch(1);
        System.err.println("start:" + System.currentTimeMillis() / 1000);
        watch.start(driverId+"抢单......");

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //原子粒度抢单方案
                Collections.singletonList("key");

                String[] param = new String[] { Cmd.ORDER_LIST + orderId,
                        Cmd.ORDER_CONSUMED_LIST, Cmd.ORDER_CONSUMED_MAP + orderId, driverId, Cmd.ORDER_ALLOW_MAP + orderId};
                List<String> params = Arrays.asList(param);

                RedisScript<String> script = new DefaultRedisScript<String>(tryGetOrderScript, String.class);
                Object object = redisClient.execute(script, params,new Object[] {});
                logger.debug("++++++++++spring redis"+object);


//                Object object = jedis.eval(tryGetOrderScript, 5, Cmd.ORDER_LIST + orderId,
//                        Cmd.ORDER_CONSUMED_LIST, Cmd.ORDER_CONSUMED_MAP + orderId, driverId, Cmd.ORDER_ALLOW_MAP + orderId);
//                logger.debug("获得订单 :" + object);

                if (object != null) {

                    //告知乘客抢单成功
                    Channel passengerOrder = passengerAndDevice.get(passengerId);

                    Document orderData = Document.parse(object.toString());

                    passengerOrder.writeAndFlush(String.format(mr.iMmsg, Cmd.SEND_MSG, "预约车辆成功,请等候司机联系...",
                            new Date().getTime(), "success", idAndName.get(orderData.getString("driverId"))) + "\n");

                    //通知司机抢单成功,将乘客信息告知司机, 有电话信息;
                    driver.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "抢单成功......乘客信息详见数据",
                            new Date().getTime(), "success", orderData.toJson()) + "\n");

                    //乘客group 联系人加入 car 乘客列表

                    redisClient.opsForSet().add(Cmd.ORDER_CAR_PASSENGER+driverId,passengerId);
                    Long expire = redisClient.getExpire(Cmd.ORDER_CAR_PASSENGER + driverId);
                    if (expire==-1) {
                        redisClient.expire(Cmd.ORDER_CAR_PASSENGER + driverId, 12, TimeUnit.HOURS);
                    }

                } else {
                    //没抢到订单司机的通知
                    if (redisClient.opsForList().size(Cmd.ORDER_LIST + orderId) == 0) {
//                    if (jedis.llen(Cmd.ORDER_LIST + orderId) == 0) {
                        logger.debug("订单已经被抢,不给司机乘客的信息,防止破解作弊!");

                        driver.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "订单已经被抢,或者订单被取消",
                                new Date().getTime(), "failure", Cmd.DATA_EMPTY) + "\n");
                    } else {
                        logger.debug("已经获得过订单;或者不在附近司机范围之内");

                        driver.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "已将获得订单,不要重复抢单;或者不属于附近的司机",
                                new Date().getTime(), "failure", Cmd.DATA_EMPTY) + "\n");
                    }
                }
                latch.countDown();
            }
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.err.println("end:" + System.currentTimeMillis() / 1000);
        watch.stop();
        logger.debug("time:" + watch.getTotalTimeSeconds());
        watch.prettyPrint();

    }

    /**
     * 乘客约车 //// TODO: 16/5/28  可优化为异步线程
     *
     * @param passenger
     * @param data
     */
    private void postAppointment(Channel passenger, Document data) {
        String json="";//1.接收用户的预约单,给每个司机发送信息  //生成订单号,简化后续逻辑(乘客多次提交订单;司机抢单也要依据订单号.....)
        //订单ID加密,隐藏用户号码信息,订单信息存在内存的订单中,不发给司机,防止软件破解作弊......

        //搜索附近的司机 根据经纬度
        final List<Document> list = searchNearDriver(data);

//        logger.debug(data.getString("_id"));
//        Channel passenger = passengerAndDevice.get(data.getString("_id"));
//        logger.debug(passenger);
//        logger.debug(incoming);

        //5.给用户发送信息,附近没有司机,请重新发起
        if (list.size() <= 0) {
            passenger.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "附近没有司机,请稍后进行约车...",
                    new Date().getTime(), "failure", Cmd.DATA_EMPTY)+ "\n");
        } else {

            startOrder(passenger, data, list);

        }
    }

    /**
     * 开始抢单
     * @param passenger
     * @param orderData
     * @param driverList
     */
    private void startOrder(Channel passenger, Document orderData, List<Document> driverList) {
        String passengerPhone = orderData.getString("_id");
        String orderId = passengerPhone + ":" + new Date().getTime(); //订单号
        orderId = EncrUtil.encrypt(orderId);

        ArrayList position = orderData.get("position", ArrayList.class);

        String appointment = String.format(env.getProperty("passenger.order"), orderId, passengerPhone,
                orderData.getString("time"), position.get(0), position.get(1));

        String driverMsg = String.format(env.getProperty("driver.msg"), orderId,
                orderData.getString("time"), position.get(0), position.get(1));


        //3.生成订单
        //3.1生成红包,保存预约单到redis   给红包设定有效期10分钟,到期失效
        try {
            logger.debug(Cmd.ORDER_LIST + orderId);
            redisClient.opsForList().rightPush(Cmd.ORDER_LIST + orderId, appointment);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean sendDriver = false; //有的司机没有绑定的情况
        //4.发送给每个司机
        for (Document doc : driverList) {
            Channel driverChannel = driverAndDevice.get(doc.get("_id"));
            if (driverChannel == null) {

                logger.debug("司机没有绑定,不能推送:" + doc.get("_id"));

            } else {
                //3.2保存预约单对应的司机到 redis , 过滤,不允许范围外的司机抢单;
                redisClient.opsForHash().put(Cmd.ORDER_ALLOW_MAP + orderId, doc.get("_id"), doc.get("_id"));

                driverChannel.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.RECEIVE_APPOINTMENT, "订单信息,可以抢单...",
                        new Date().getTime(), "success", driverMsg) + "\n");

                sendDriver = true;
            }
        }
        //3.3 key 存在之后,设置订单的有效期......
        redisClient.expire(Cmd.ORDER_ALLOW_MAP + orderId, 10, TimeUnit.MINUTES);
        Long expire = redisClient.getExpire(Cmd.ORDER_ALLOW_MAP + orderId);
        if (expire==-1) {
            redisClient.expire(Cmd.ORDER_ALLOW_MAP + orderId, 10, TimeUnit.MINUTES);
        }


        //4.给用户发送信息,已经推送到司机,等待
        if (sendDriver) {
            passenger.writeAndFlush(String.format(env.getProperty("im.msg"),
                    Cmd.WAIT_APPOINTMENT, "已经推送给给" + driverList.size() + "位司机,请稍后...",
                    new Date().getTime(), "success", appointment) + "\n");

            // TODO: 16/5/28   订单入库  消息队列入库作为备份库可以对账;
        } else {
            passenger.writeAndFlush(String.format(env.getProperty("im.msg"),
                    Cmd.SEND_MSG, "附近没有司机,请稍后进行约车...",
                    new Date().getTime(), "failure", Cmd.DATA_EMPTY) + "\n");

        }

    }

    /**
     * 搜索附近的司机
     *
     * @param data
     * @param position
     * @return
     */
    private List<Document> searchNearDriver(Document data) {
        ArrayList position = data.get("position", ArrayList.class);

        //2.搜索附近的司机
//                final List<Location> drivers = locationRepository.findByPositionWithin(circle);
//                > db.location.find( {position: { $near: [0,0], $maxDistance: 0.7  } } )

        Document find = Document.parse(String.format(env.getProperty("passenger.nearDriver"),
                position.get(0), position.get(1), data.getString("radius")));

        logger.debug("im near driver:" + find);

        FindIterable drivers = mongoCollection.find(find);
        final List<Document> list = new ArrayList<Document>();

        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);//数据不存在情况测试

            drivers.into(list, new SingleResultCallback<Void>() {
                @Override
                public void onResult(final Void result, final Throwable t) {
                    logger.debug("执行完成");
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("near driver count:" + list.size());

        return list;
    }

    /**
     * 获取司机的位置信息
     *
     * @param incoming
     * @param driverGeo
     */
    private void sendGeo(final Channel driver, final Document driverGeo) {
        //直接保存json 数据,验证哪些字段是必须的,以及层级,数据格式验证,安全问题??? //// FIXME: 16/5/25
        //driver.geo={"_id":"%s","time":Date("%s"), "type": "Point", "position": [%s,%s]}

        logger.debug(env.getProperty("driver.geo"));

        //final CountDownLatch countWait = new CountDownLatch(1);
        //final AtomicReference<Throwable> th = new AtomicReference<>();
        //logger.debug("insert driverGeo:"+json.toString());

        final String driverId = driverGeo.getString("_id");
        final ArrayList position = driverGeo.get("position", ArrayList.class);

        mongoCollection.insertOne(Document.parse(String.format(env.getProperty("driver.geo"),
                driverId, new Date(driverGeo.getString("time")).getTime(), position.get(0), position.get(1))),
                new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) {
                //logger.debug("Inserted!");
                if (t != null) {
                    //th.set(t);
                }
                //countWait.countDown();
                //// TODO: 16/5/25 geo 信息不返回,节约带宽,调试期间先放开
                driver.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG,
                        driverId + " 数据插入完成...", new Date().getTime(), "success", Cmd.DATA_EMPTY) + "\n");
            }
        });
        //countWait.await(10, TimeUnit.SECONDS);

        //给乘客联系人发送geo 信息 ,异步
        //                    jedis.sadd(Cmd.ORDER_CAR_PASSENGER+driverId,passengerId);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Set<String> passengers =redisClient.opsForSet().members(Cmd.ORDER_CAR_PASSENGER + driverId);

//                Set<String> passengers = jedis.smembers(Cmd.ORDER_CAR_PASSENGER + driverId);
                for (String passenger:passengers
                        ) {

                    Channel passengerChannel = passengerAndDevice.get(passenger);
                    if (passengerChannel!=null) {//乘客可能掉线
                        passengerChannel.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG,
                                driverId + "车辆geo 信息...", new Date().getTime(), "success",

                                String.format(env.getProperty("driver.geo"),
                                        driverId, new Date(driverGeo.getString("time")).getTime(), position.get(0), position.get(1))

                        ) + "\n");
                    }

                }

            }

        });

    }

    /**
     * 绑定乘客
     *
     * @param passenger
     * @param data
     */
    private void bindPassenger(Channel passenger, Document data) {

        passengerAndDevice.put(data.getString("_id"), passenger);
        idAndName.put(data.getString("_id"), data);

        passenger.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "手机号[" + data.getString("_id") +
                "]绑定完成,可以约车...", new Date().getTime(), "success", Cmd.DATA_EMPTY) + "\n");

    }

    /**
     * 绑定司机,司机出车
     *
     * @param driver
     * @param data
     */
    private void bindDriver(Channel driver, Document data) {

        driverAndDevice.put(data.getString("_id"), driver);
        idAndName.put(data.getString("_id"), data);

        driver.writeAndFlush(String.format(env.getProperty("im.msg"), Cmd.SEND_MSG, "手机号[" + data.getString("_id") +
                "]绑定完成,可以接单...", new Date().getTime(), "success", Cmd.DATA_EMPTY) + "\n");
        //建立乘客列表的队列,一个乘客手机号可能有多位乘客
        //队列的有效期为一天



    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception { // (5)
        final Channel incoming = ctx.channel();

        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(Future<Channel> future) throws Exception {
                        incoming.writeAndFlush(
                                "Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");
                        incoming.writeAndFlush(
                                "Your session is protected by " +
                                        ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                                        " cipher suite.\n");

                        channels.add(incoming);
                    }
                });

        logger.debug("SimpleChatClient:" + incoming.remoteAddress() + "在线,总人数:" + channels.size() + "\n");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
        Channel incoming = ctx.channel();
        logger.debug("SimpleChatClient:" + incoming.remoteAddress() + "掉线,总人数:" + channels.size() + "\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel incoming = ctx.channel();
        logger.debug("SimpleChatClient:" + incoming.remoteAddress() + "异常,总人数:" + channels.size() + "\n");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}