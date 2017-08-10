package com.javastar920905.spider.config;

/**
 * Created by ouzhx on 2017/8/10.
 * java 源代码阅读调试
 */
public class ReadCode {
    public static void main(String[] args) {
        int[] src = {1, 2, 3};
        int[] dest = {10, 20, 30};
        System.arraycopy(src, 3, dest, 1, 2);

        //把 src复制到dest , 从srcPos 开始,复制length个src的元素, 放置到将dest的destPos位置
        //一不小心就数组越界异常了 1:srcPos+length<=src数组长度; 2:destPos+length<=dest数组长度
        for (int i : dest) {
            System.out.print(i + "\t");
        }
    }
}
