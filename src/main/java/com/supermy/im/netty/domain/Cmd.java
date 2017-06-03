package com.supermy.im.netty.domain;


/**
 * 指令集
 */
public class Cmd {

    public static String ORDER_LIST = "orderList"; //订单列表
    public static String ORDER_CONSUMED_LIST = "orderConsumedList"; //订单消费列表
    public static String ORDER_CONSUMED_MAP = "orderConsumedMap"; //已有订单的司机列表
    public static String ORDER_ALLOW_MAP = "orderAllowMap"; //允许抢单的司机列表
    public static String ORDER_CAR_PASSENGER = "orderPassengers"; //允许抢单的司机列表   乘客


//	-- 函数：尝试获得订单，如果成功，则返回json字符串，如果不成功，则返回空
//	-- 参数：1.订单队列名{KEYS[1],List}，2.已消费的队列名{KEYS[2],List}，3.去重的Map名{KEYS[3],Hash},4.司机ID{KEYS[4]}，5.允许抢单的司机ID {Hash KEYS[5] }
//	-- 返回值：nil 或者 json字符串，包含司机ID：driverId，订单ID：id，订单信息：money
   public static String tryGetOrderScript =

                      "if  redis.call('hexists', KEYS[5], KEYS[4]) == 0 then\n"
                    + "   return nil\n"
                    + "end\n"

            +"if  redis.call('hexists', KEYS[3], KEYS[4]) ~= 0 then\n"
                    + "   return nil\n"
                    + "end\n"

            + "local  orderForm = redis.call('rpop', KEYS[1]);\n"
                    + "if orderForm then\n"
                    + "   local x = cjson.decode(orderForm);\n"
                    + "   x['driverId'] = KEYS[4];\n"
                    + "   local re = cjson.encode(x);\n"
                    + "   redis.call('hset', KEYS[3], KEYS[4], KEYS[4]);\n"
                    + "   redis.call('expire', KEYS[3], 10*60);\n"
                    + "   redis.call('lpush', KEYS[2], re);\n"
                    + "   return re;\n"
                    + "end\n"

                    + "return nil";


    public static final String SEND_GEO="01";//geo信息
    public static final String SEND_MSG="02";//发送消息
    public static final String BIND_DRIVER="03";//绑定司机
    public static final String BIND_PASSENGER="04";//绑定乘客
    public static final String POST_APPOINTMENT="05";//提交预约单
    public static final String RECEIVE_APPOINTMENT="06";//接收预约单
    public static final String CANCEL_APPOINTMENT="07";//接收预约单
    public static final String WAIT_APPOINTMENT="08";//接收预约单
    public static final String SEND_ORDER="09";//抢单
    public static final String SEARCH_ORDER="10";//查询是否有未被抢的订单

    public static final String CODE_NONE="-1";//错误指令 无法处理
    public static final String DATA_EMPTY="{}";//错误指令 无法处理


}
