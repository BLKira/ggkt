package com.atguigu.excel;

import com.alibaba.excel.EasyExcel;

public class TestRead {
    public static void main(String[] args) {
        String fileName = "E:\\ggkt\\ggkt.xlsx";
        EasyExcel.read(fileName,User.class,new ExcelListener()).sheet().doRead();
    }



}
