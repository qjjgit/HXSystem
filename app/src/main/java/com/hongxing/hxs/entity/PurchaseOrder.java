package com.hongxing.hxs.entity;

import java.util.Arrays;
import java.util.Date;

public class PurchaseOrder {
    private Integer id;
    private Date date;
    private byte[] data;

    public PurchaseOrder() {
    }

    public PurchaseOrder(Date date, byte[] data) {
        this.date = date;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "id=" + id +
                ", date=" + date +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
