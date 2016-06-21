package com.supermy.im.netty.handler;

import com.supermy.im.netty.domain.Cmd;
import org.bson.Document;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by moyong on 16/5/10.
 */
public class JsonTest {
    public static void main(String[] args) {


        String subjsondata = "{\"passenger\":\"%s\",\"phone\":\"%s\", \"time\":Date(\"%s\"), \"position\": [%s,%s]}";
        String subjson = String.format(subjsondata,"张山","1816033388",new Date().getTime(),"0.1","0.2");

        Document submsg = Document.parse(subjson);
        System.out.println(submsg);
        System.out.println(submsg.get("time").getClass());
        System.out.println(submsg.get("position").getClass());
//        System.out.println(submsg.get("position",String[].class));
        System.out.println(submsg.get("position",ArrayList.class).get(0));
        System.out.println(submsg.get("position",ArrayList.class).get(1));


        String jsondata = "{\"cmd\":\"%s\",\"msg\":\"%s\", \"time\": \"%s\",\"status\": \"%s\", \"data\": %s}";

        String json = String.format(jsondata,"01","已经推送给给2位司机...",new Date().getTime(), "success",subjson);
//        String json = String.format(jsondata,"01","已经推送给给2位司机...",new Date().getTime(), "success","{}");
        System.out.println(json);

        Document msg = Document.parse(json);
        System.out.println(msg);
        System.out.println(msg.getString("cmd"));
        System.out.println(msg.get("data",Document.class).get("phone"));

        Document m = Document.parse("{\"_id\":\"18610588588\",\"time\":\"1464230327946\", \"type\": \"Point\", \"position\": [1.2,1.1]}");

        System.out.println(m);



    }

}
