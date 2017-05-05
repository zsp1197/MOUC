package util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

/*
 * Copyright (c) 2005, Tsinghua University. All Rights Reserved.
 * @author Guangyu HE (gyhe@tsinghua.edu)
 * @version 1.0
 */

public class HToString {
    // --------------------------------functions for array-------------

    /**
     * convert int array to a string
     *
     * @param x                    double[], double array
     * @param elementNumInEachLine int
     * @return String
     */
    public static String convert(double x[], int elementNumInEachLine) {
        StringBuffer sb = new StringBuffer();
        int len = x.length;
        for (int i = 0; i < len; i++) {
            sb.append(x[i]).append("\t");
            if ((i != len - 1 && (i + 1) % elementNumInEachLine == 0))
                sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * convert 2-dimension double array to a string
     *
     * @param x double[][], double array
     * @return String
     */
    public static String convert(double x[][]) {
        StringBuffer sb = new StringBuffer();
        int len = x.length;
        for (int i = 0; i < len; i++)
            sb.append(Arrays.toString(x[i]));
        return sb.toString();
    }

    // --------------------------------functions for date && time-------------

    /**
     * get the current time, and return it as string format.
     *
     * @return String
     */
    public static String nowTime() {
        Calendar rightNow = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss SSS");
        return sdf.format(rightNow.getTime());
    }

    /**
     * convert date,time,calendar to the appropriate format The format is as
     * follows: G Era designator Text AD y Year Year 1996; 96 M Month in year
     * Month July; Jul; 07 w Week in year Number 27 W Week in month Number 2 D
     * Day in year Number 189 d Day in month Number 10 F Day of week in month
     * Number 2 E Day in week Text Tuesday; Tue a Am/pm marker Text PM H Hour in
     * day (0-23) Number 0 k Hour in day (1-24) Number 24 K Hour in am/pm (0-11)
     * Number 0 h Hour in am/pm (1-12) Number 12 m Minute in hour Number 30 s
     * Second in minute Number 55 S Millisecond Number 978 z Time zone General
     * time zone Pacific Standard Time; PST; GMT-08:00 Z Time zone RFC 822 time
     * zone -0800 <p/> usage: dd/MM/yyyy HH:mm:ss SSS
     *
     * @param rightNow calendar
     * @param format   string
     * @return String
     */
    public static String convert(Calendar rightNow, String format) {
        if (rightNow == null)
            return "";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(rightNow.getTime());
    }

    // --------------------------------------------------------------------
    public static String convert(Rectangle2D r) {
        return r.getX() + "," + r.getY() + "," + r.getWidth() + ","
                + r.getHeight();
    }

    public static String convert(Point2D r) {
        return r.getX() + "," + r.getY();
    }
}
