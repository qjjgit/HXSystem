package com.hongxing.hxs.entity;

public class Goods {
    private Integer id;
    private String name;
    private String barcode;
    private String unit;
    private Float price;
    private Float orig;

    public Goods() {
    }

    public Goods(String name, String barcode, String unit, Float price, Float orig) {
        this.name = name;
        this.barcode = barcode;
        this.unit = unit;
        this.price = price;
        this.orig = orig;
    }

    public Goods(Integer id, String name, String barcode, String unit, Float price, Float orig) {
        this.id = id;
        this.name = name;
        this.barcode = barcode;
        this.unit = unit;
        this.price = price;
        this.orig = orig;
    }

    @Override
    public String toString() {
        return "Goods{" +
                "name='" + name + '\'' +
                ", barcode='" + barcode + '\'' +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                ", orig=" + orig +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getOrig() {
        return orig;
    }

    public void setOrig(Float orig) {
        this.orig = orig;
    }
}
