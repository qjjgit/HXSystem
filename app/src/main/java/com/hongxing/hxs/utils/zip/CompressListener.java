package com.hongxing.hxs.utils.zip;

public interface CompressListener {
    /** 开始压缩 */
    void zipStart();
    /** 压缩成功 */
    void zipSuccess();
    /** 压缩失败 */
    void zipFail();
}
