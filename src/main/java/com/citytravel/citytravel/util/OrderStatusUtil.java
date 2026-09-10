package com.citytravel.citytravel.util;

public class OrderStatusUtil {

    public static final String STATUS_UNPAID = "unpaid";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_USED = "used";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_EVALUATED = "evaluated";

    public static String getStatusText(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_UNPAID:
                return "待支付";
            case STATUS_PAID:
                return "已支付";
            case STATUS_USED:
                return "已使用";
            case STATUS_EXPIRED:
                return "已过期";
            case STATUS_EVALUATED:
                return "已评价";
            default:
                return "未知";
        }
    }
}