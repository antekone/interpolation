package com.mds;

public class Log {
    public static void log(String fmt, Object... args) {
        String out = String.format(fmt, args);
        System.out.println(out);
    }
}
