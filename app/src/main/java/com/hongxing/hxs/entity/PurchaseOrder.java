package com.hongxing.hxs.entity;


public class PurchaseOrder {
    private String id;
    private String supplier;
    private String date;
    private String data_uri;

    public PurchaseOrder() {
    }

    public PurchaseOrder(String id, String supplier, String date, String data_uri) {
        this.id = id;
        this.supplier = supplier;
        this.date = date;
        this.data_uri = data_uri;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "id='" + id + '\'' +
                ", supplier='" + supplier + '\'' +
                ", date='" + date + '\'' +
                ", data_uri='" + data_uri + '\'' +
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

    public void setDate(String data_uri) {
        this.data_uri = data_uri;
    }

    public String getDataUri() {
        return data_uri;
    }

    public void setDataUri(String data_uri) {
        this.data_uri = data_uri;
    }
}
