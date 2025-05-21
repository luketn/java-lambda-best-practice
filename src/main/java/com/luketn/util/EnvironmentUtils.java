package com.luketn.util;

public class EnvironmentUtils {
    public static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        } else {
            return value;
        }
    }
}
