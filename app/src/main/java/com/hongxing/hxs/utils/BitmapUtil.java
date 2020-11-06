package com.hongxing.hxs.utils;

import android.graphics.Bitmap;

import com.hongxing.hxs.MainActivity;

public class BitmapUtil {
    private static float scale;
    static {
        scale=ScreenUtil.getDeviceDensity(MainActivity.getMainContext());
    }
    public static class CheckZoom{
        boolean isZoom;
        public CheckZoom(boolean isZoom) {
            this.isZoom = isZoom;
        }
        public boolean isZoom() {
            return isZoom;
        }
        public void setZoom(boolean zoom) {
            isZoom = zoom;
        }
    }
    /**
     * 缩放图片裁剪指定大小的正方形
     * @param bitmap 原图
     * @param edgeLength 正方形的边长
     * @return 裁剪后的图片
     */
    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength){
        if(null == bitmap || edgeLength <= 0){
            return null;
        }
        Bitmap result = bitmap;
        edgeLength=(int)(edgeLength * scale + 0.5f);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if(width > edgeLength && height > edgeLength){
            int longerEdge = (int)(edgeLength * Math.max(width, height) / Math.min(width, height));
            int scaleWidth = width > height ? longerEdge : edgeLength;
            int scaleHeight = width > height ? edgeLength : longerEdge;
            Bitmap scaleBitmap;
            try{
                scaleBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true);
            }catch(Exception e){
                return null;
            }
            //从图片的正中间裁剪
            int xTopLeft = (scaleWidth - edgeLength)/2;
            int yTopLeft = (scaleHeight - edgeLength)/2;
            try{
                result = Bitmap.createBitmap(scaleBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
                scaleBitmap.recycle();
            }catch(Exception e){
                return null;
            }
        }
        return result;
    }

    /**
     * 按比例缩放图片 指定短边长
     * @param bitmap 原图
     * @param shorterEdgeLength 较短边的边长
     * @return 裁剪后的图片
     */
    public static Bitmap proportionalScaleBitmap(Bitmap bitmap, int shorterEdgeLength){
        if(null == bitmap || shorterEdgeLength <= 0){
            return null;
        }
        System.out.println("shorterEdgeLength: "+shorterEdgeLength);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap scaleBitmap = null;
        if(width > shorterEdgeLength && height > shorterEdgeLength){
            int longerEdge = (int)(shorterEdgeLength * Math.max(width, height) / Math.min(width, height));
            int scaleWidth = width > height ? longerEdge : shorterEdgeLength;
            System.out.println("scaleWidth: "+scaleWidth);
            int scaleHeight = width > height ? shorterEdgeLength : longerEdge;
            System.out.println("scaleHeight: "+scaleHeight);
            try{
                scaleBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true);
            }catch(Exception e){
                return null;
            }
        }
        return scaleBitmap;
    }
}
