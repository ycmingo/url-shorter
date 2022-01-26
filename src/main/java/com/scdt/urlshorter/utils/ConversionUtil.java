package com.scdt.urlshorter.utils;

import org.apache.commons.lang.StringUtils;

/**
 * @author mingo
 */
public class ConversionUtil {
    /**
     * 初始化 62 进制数据，索引位置代表字符的数值，比如 A代表10，z代表61等
     */
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SCALE = 62;

    /**
     * 将数字转为62进制
     *
     * @param num    Long 型数字
     * @param length 转换后的字符串长度，不足则左侧补0
     * @return 62进制字符串
     */
    public static String encode(long num, int length) {
        StringBuilder sb = new StringBuilder();
        int remainder = 0;

        while (num > SCALE - 1) {
            /**
             * 对 scale 进行求余，然后将余数追加至 sb 中，由于是从末位开始追加的，因此最后需要反转（reverse）字符串
             */
            remainder = Long.valueOf(num % SCALE).intValue();
            sb.append(CHARS.charAt(remainder));

            num = num / SCALE;
        }

        sb.append(CHARS.charAt(Long.valueOf(num).intValue()));
        String value = sb.reverse().toString();
        return StringUtils.leftPad(value, length, '0');
    }

    /**
     * 62进制字符串转为数字
     *
     * @param str 编码后的62进制字符串
     * @return 解码后的 10 进制字符串
     */
    public static long decode(String str) {
        /**
         * 将 0 开头的字符串进行替换
         */
        str = str.replace("^0*", "");
        long num = 0;
        int index = 0;
        for (int i = 0; i < str.length(); i++) {
            /**
             * 查找字符的索引位置
             */
            index = CHARS.indexOf(str.charAt(i));
            /**
             * 索引位置代表字符的数值
             */
            num += (long) (index * (Math.pow(SCALE, str.length() - i - 1)));
        }

        return num;
    }
}
