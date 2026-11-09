package com.citytravel.citytravel.util;

/**
 * 分页参数收敛工具。
 * 防止调用方传入超大 pageSize 导致一次拉全表（DoS / 内存打爆）。
 */
public final class PageUtil {

    /** 单页最大条数 */
    public static final int MAX_PAGE_SIZE = 100;
    /** 默认单页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private PageUtil() {
    }

    /** 页码最小为 1 */
    public static int pageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    /** 每页条数收敛到 [1, MAX_PAGE_SIZE] */
    public static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
