/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supermy.im.mongo;

import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.*;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * netty+mongo 异步调用;
 * 配置文件设置mongo;
 * <p/>
 * 只负责完成数据插入-司机信息:< geo:id|精度|维度|时间 >;
 */
@Component
@Qualifier("mymongo")
@PropertySource(value = "classpath:/properties/local/nettyserver.properties")
public class MongoRepository {
    @Configuration
    @Profile("production")
    @PropertySource("classpath:/properties/production/nettyserver.properties")
    static class Production {
    }

    @Configuration
    @Profile("local")
    @PropertySource({"classpath:/properties/local/nettyserver.properties"})
    static class Local {
    }

    @Value("${mongo.address}")
    private String mongoAddress;

    @Value("${mongo.user}")
    private String mongoUser;

    @Value("${mongo.passwd}")
    private String mongoPasswd;

    @Value("${mongo.db}")
    private String mydb;

    @Value("${mongo.coll}")
    private String mycoll;

    @Value("${mongo.mytasks}")
    private String mytasks;

    @Value("${driver.geo}")
    public String upgeo;

//    @Value("${driver.msg}")
//    public String driverMsg;

    @Value("${im.msg}")
    public String iMmsg;




    @Bean(name = "mongoClient")
    public MongoClient mongoClient() {
        System.out.println("*******************" + mongoAddress);

        ClusterSettings clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(new ServerAddress(mongoAddress))).build();
        MongoCredential credential = MongoCredential.createCredential(mongoUser, mydb, mongoPasswd.toCharArray());

        MongoClientSettings settings = MongoClientSettings.builder().streamFactoryFactory(new NettyStreamFactoryFactory()).
                clusterSettings(clusterSettings).credentialList(Arrays.asList(credential)).build();


//        MongoClient mongoClient = new MongoClient(new ServerAddress(), Arrays.asList(credential));

        MongoClient mongoClient = MongoClients.create(settings);
        return mongoClient;
    }

    @Bean(name = "mongoDatabase")
    public MongoDatabase mongoDatabase() {
        System.out.println("*******************" + mydb);

        MongoDatabase db = mongoClient().getDatabase(mydb);

        return mongoClient().getDatabase(mydb);
    }

    /**
     *
     * @param collectionName
     * @return
     */
    public boolean collectionExists(final String collectionName) {


        MongoIterable colls=mongoDatabase().listCollectionNames();

         List<String> collectionNames = new ArrayList<String>();

        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);//数据不存在情况测试

            colls.into(collectionNames, new SingleResultCallback<Void>() {
                @Override
                public void onResult(final Void result, final Throwable t) {
                   // logger.debug("执行完成");
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(collectionNames);
        System.out.println(collectionNames.size());

        for (String name : collectionNames) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    @Bean(name = "mongoCollection")
    public MongoCollection mongoCollection() {
        System.out.println("*******************" + mycoll);

        final MongoDatabase mydb = mongoDatabase();

        /**
         * 如果集合不存在,创建集合;设定集合索引的失效时间;
         * 实际如果超过1分钟不更新位置数据,可能失效,不再进行附近的推荐查询;
         */
        if (!collectionExists(mycoll)){
            final CountDownLatch countDownLatch = new CountDownLatch(1);//数据不存在情况测试

            mydb.createCollection(mycoll, new SingleResultCallback<Void>() {
                @Override
                public void onResult(final Void result, final Throwable t) {
                    // logger.debug("执行完成");

                    MongoCollection coll = mydb.getCollection(mycoll);



                    coll.createIndex(new Document("position","2d"), new SingleResultCallback<Void>() {
                        @Override
                        public void onResult(final Void result, final Throwable t) {
                            // logger.debug("创建索引不用等待完成");
                        }
                    });

                    //设置数据的有效期
                    Document expire = Document.parse("{time:1},{expireAfterSeconds:10*60}");
                    coll.createIndex(expire, new SingleResultCallback<Void>() {
                        @Override
                        public void onResult(final Void result, final Throwable t) {
                            // logger.debug("创建索引不用等待完成");
                        }
                    });


                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


        MongoCollection coll = mydb.getCollection(mycoll);
        // TODO: 16/5/10  手动创建索引;设定数据的有效期
        //> db.location.ensureIndex( {position: "2d"} )


        return coll;
    }


    @Bean(name = "mongoCollectionTask")
    public MongoCollection mongoCollectionTask() {
        System.out.println("*******************" + mytasks);

        MongoDatabase mydb = mongoDatabase();
        MongoCollection coll = mydb.getCollection(mytasks);
        // TODO: 16/5/10  手动创建索引;设定数据的有效期
        //> db.location.ensureIndex( {position: "2d"} )


        return coll;
    }


//    public static void main(String[] args) throws Exception {
////        ClusterSettings clusterSettings = ClusterSettings.builder().hosts(Arrays.asList(new ServerAddress("127.0.0.1"))).build();
////        MongoCredential credential = MongoCredential.createCredential("mydb", "mydb", "123456");
////
////        MongoClientSettings settings = MongoClientSettings.builder().streamFactoryFactory(new NettyStreamFactoryFactory()).
////                clusterSettings(clusterSettings).credentialList(Arrays.asList(credential)).build();
////
//////        MongoClient mongoClient = new MongoClient(new ServerAddress(), Arrays.asList(credential));
////
////        MongoClient mongoClient = MongoClients.create(settings);
////        MongoDatabase mydb = mongoClient().getDatabase("mydb");
////        MongoCollection coll = mydb.getCollection("location");
////
////        String query =  "{position: { $near: [0.1,0.2], $maxDistance: 0.7  } }";
////        Document find = Document.parse(query);
////
////
////
////        FindIterable drivers = coll.find(find);
////
////        System.out.println(drivers);
//    }


}
