package com.supermy.im.netty.handler;

import org.bson.Document;
import org.springframework.boot.SpringApplication;

import java.util.Date;

/**
 * Created by moyong on 16/5/10.
 */
public class DateUtilTest {
    public static void main(String[] args) {
        System.out.print(new Date().getTime());
        System.out.print(new Date());
        System.out.print(new Date().toGMTString());
        System.out.print(new Date().toLocaleString());
        System.out.print(new Date().getTimezoneOffset());

        String[] abc="18610586586|1462861314465|106.72|26.57".split("/|");

        System.out.println(abc[0]);

        String sb="{\"_id\":\"1861033348199\",\"time\":Date(\"1462870826752\"),\"loc\" : { \"type\": \"Point\", \"position\": [9.0,9.0]}}";
        System.out.println(Document.parse(sb));

    }

}
