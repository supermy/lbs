package com.supermy.im.netty.domain;


import java.io.Serializable;
import java.util.Date;

/**
 * Created by moyong on 16/5/10.
 * 实际的经纬度信息
 */
@Deprecated
public class Location implements Serializable {

    //@Id
    private String id;

    private double[] position;

    private Date utime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double[] getPosition() {
        return position;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public Date getUtime() {
        return utime;
    }

    public void setUtime(Date utime) {
        this.utime = utime;
    }

    @Override
    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append(id).append("|");
        sb.append(position[0]).append("|");
        sb.append(position[1]).append("|");
        sb.append(utime);
        return super.toString();
    }

}

