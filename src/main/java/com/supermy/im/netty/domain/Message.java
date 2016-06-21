package com.supermy.im.netty.domain;

import java.io.Serializable;

/**
 * Created by moyong on 16/5/19.
 */
@Deprecated
public class Message implements Serializable{
    private String id;
    private String username;
    private String msg;
}
