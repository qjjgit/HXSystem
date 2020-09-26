package com.hongxing.hxs.entity;

import java.util.Arrays;

public class PurchaseOrder {
    private String id;
    private String supplier;
    private String date;
    private byte[] data;

    public PurchaseOrder() {
    }

    public PurchaseOrder(String id, String supplier, String date, byte[] data) {
        this.id = id;
        this.supplier = supplier;
        this.date = date;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "id='" + id + '\'' +
                ", supplier='" + supplier + '\'' +
                ", date='" + date + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
