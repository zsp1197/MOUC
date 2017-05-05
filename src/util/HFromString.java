package util;

import java.util.StringTokenizer;
import java.util.logging.Logger;

/*
 * Copyright (c) 2005, Tsinghua University. All Rights Reserved.
 * @author Guangyu HE (gyhe@tsinghua.edu)
 * @version 1.0
 */

public class HFromString {
    private static Logger logger = Logger
            .getLogger("com.googlepages.guo.jiachun.HFromString");

    // --------------------------------functions for array-------------

    /**
     * Take the given string and chop it up into a series of strings on
     * whitespace boundries. This is useful for trying to get an array of
     * strings out of the resource file.
     *
     * @param input string
     * @return string array
     */
    public static String[] toStringArray(String input) {
        return toStringArray(input, " \t\n\r\f");
    }

    public static String[] toStringArray(String input, String delim) {
        if (input == null)
            return new String[]{};
        //
        StringTokenizer t = new StringTokenizer(input, delim);
        int len = t.countTokens();
        if (len <= 0)
            return new String[]{};
        String cmd[] = new String[len];
        for (int i = 0; i < len; i++) {
            try {
                cmd[i] = t.nextToken();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return cmd;
    }

    /**
     * convert String src into a double array
     *
     * @param src   String
     * @param delim String
     * @return double[]
     */
    public static double[] toDoubleArray(String src, String delim) {
        if (src == null)
            return new double[]{};

        StringTokenizer token = new StringTokenizer(src, delim);
        int len = token.countTokens();
        if (len <= 0)
            return null;

        double a[] = new double[len];
        for (int i = 0; i < len; i++) {
            try {
                String temp = token.nextToken();
                a[i] = Double.parseDouble(temp);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return a;
    }

    /**
     * convert string which is splited by delim to double Array a
     *
     * @param src   String
     * @param delim String
     * @param a     double[]
     */
    public static void toDoubleArray(String src, String delim, double a[]) {
        if (src == null)
            return;
        if (a == null)
            return;

        StringTokenizer token = new StringTokenizer(src, delim);

        // compare
        int len = token.countTokens();
        if (len < a.length) {
            // hhh modified, 2004.11.7
            // for (int i = len; i < a.length; i++) a[i] = 0;
            System.err.println("string is too short!");
        } else if (len > a.length) {
            len = a.length;
            System.err.println("string is too long!");
        }

        for (int i = 0; i < len; i++) {
            try {
                String temp = token.nextToken();
                a[i] = Double.parseDouble(temp);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    // --------------------------------functions for reading
    // property---------------

    /**
     * get property's value in String src for example, src is as follows: model =
     * 'Linear Programming' % haha rowNumber = 2 factor = 3.1 we can call "
     * getString(src,"model","=%")" to get its value: 'Linear Programming'
     *
     * @param src      String
     * @param property String
     * @param delim    String
     * @return String
     */
    public static String getString(String src, String property, String delim) {
        if (src == null || property == null)
            return null;

        property = property.toUpperCase();
        StringTokenizer token = new StringTokenizer(src, "\r\n");

        try {
            while (true) {
                String temp = token.nextToken();
                // get the line
                StringTokenizer token2 = new StringTokenizer(temp, delim);
                String temp2 = token2.nextToken();
                if (property.compareTo(temp2.trim().toUpperCase()) == 0) {
                    return token2.nextToken().trim();
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    /**
     * <p/>
     * for one property, get more strings
     * </p>
     * <p/> using Example 1: Assume src is as follows: eolien.Ve(1) = 4;
     * eolien.Pe(1) = 24; eolien.Cp(1) = 0.159; eolien.Ve(2) = 5; eolien.Pe(2) =
     * 86; eolien.Cp(2) = 0.292; we use: getString(src,"eolien.Ve(1)", "=;") get
     * value: 4, eolien.Pe(1),24,eolien.Cp(1),0.159
     * </p>
     * <p/> using Example 2: String src2[] = HFromString.getStrings(src,
     * property, " \t"); if ( src2.length < pointNum ) { logger.severe(" Wrong
     * format in Property : " + property + "\n" ); continue; } for(int j=0;j<pointNum;j++) {
     * price[i*pointNum + j] = Double.parseDouble(src2[j]); }
     * </p>
     *
     * @param src      String
     * @param property String
     * @param delim    String
     * @return String[]
     */
    public static String[] getStrings(String src, String property, String delim) {
        if (src == null || property == null)
            return null;

        property = property.toUpperCase();
        StringTokenizer token = new StringTokenizer(src, "\r\n");

        try {
            while (true) {
                String temp = token.nextToken();
                // get the line
                StringTokenizer token2 = new StringTokenizer(temp, delim);
                String temp2 = token2.nextToken();
                if (property.compareTo(temp2.trim().toUpperCase()) == 0) {
                    int count = token2.countTokens();
                    String t[] = new String[count];
                    for (int i = 0; i < count; i++)
                        t[i] = token2.nextToken().trim();
                    return t;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    /**
     * @param src          String
     * @param property     String
     * @param delim        String
     * @param defaultValue String
     * @return String
     */
    public static String readString(String src, String property, String delim,
                                    String defaultValue) {
        String s = getString(src, property, delim);
        if (s == null)
            return defaultValue;
        return s;
    }

    /**
     * read property's value in String src for example, src is as follows: model =
     * 'Linear Programming' % haha rowNumber = 2 factor = 3.1 we can call "
     * readInteger(src,"rowNumber","=%")" to get its value: 2
     *
     * @param src          String
     * @param property     String
     * @param delim        String
     * @param defaultValue int
     * @return int
     */
    public static int readInteger(String src, String property, String delim,
                                  int defaultValue) {
        String s = getString(src, property, delim);
        if (s == null)
            return defaultValue;

        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            logger.severe("the wrong string is " + s + "\n");
        }
        return defaultValue;
    }

    /**
     * read property's value in String src for example, src is as follows: model =
     * 'Linear Programming' % haha rowNumber = 2 factor = 3.1 we can call "
     * readDouble(src,"factor","=%")" to get its value: 3.1
     *
     * @param src          String
     * @param property     String
     * @param delim        String
     * @param defaultValue int
     * @return double
     */
    public static double readDouble(String src, String property, String delim,
                                    double defaultValue) {
        String s = getString(src, property, delim);
        if (s == null)
            return defaultValue;

        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            logger.severe("the wrong string is " + s + "\n");
        }
        return defaultValue;
    }

    // --------------------------------------coversions-------------------------

    /**
     * Returns the classname without the package. Example: If the input class is
     * java.lang.String than the return value is String.
     *
     * @param cl The class to inspect
     * @return The classname
     */
    public static String getClassNameWithoutPackage(Class cl) {
        String className = cl.getName();
        int pos = className.lastIndexOf('.') + 1;
        if (pos == -1)
            pos = 0;
        return className.substring(pos);
    }

}
