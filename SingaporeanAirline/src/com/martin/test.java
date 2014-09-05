package com.martin;

import java.math.BigDecimal;

public class test {
    public static void main(String args[]){
        double d1 = 1234.5678;
        double d2 = 2456.6232;
        BigDecimal tmpD1 = new BigDecimal(Double.valueOf(d1));
        BigDecimal tmpD2 = new BigDecimal(Double.valueOf(d2));
        double real1 = tmpD1.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        double real2 = tmpD2.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        System.out.println(real1+real2);
    }
}
