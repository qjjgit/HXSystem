package com.hongxing.hxs.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static boolean isValidDate(String str){
        if (str == null || str.length() == 0) {
            return false;
        }
        String s = str.replaceAll("[/\\- ]", "");
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date = format.parse(s);
            if (!format.format(date).equals(s)) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
