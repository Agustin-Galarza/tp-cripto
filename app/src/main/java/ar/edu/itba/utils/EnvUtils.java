package ar.edu.itba.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.function.Function;

public final class EnvUtils {
    private static Dotenv dotenv;
    private static boolean initialized = false;

    static {
        try {
            dotenv = Dotenv.load();
            initialized = true;
        } catch (Exception e) {
            System.err.println(".env file not found. Default values will be used");
            dotenv = null;
        }
    }

    public static String get(String key, String defaultValue) {
        if(!initialized) {
            return defaultValue;
        }
        return dotenv.get(key);
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static Integer getInt(String key, Function<String, Integer> converter, Integer defaultValue) {
        if(!initialized) {
            return defaultValue;
        }
        var value = dotenv.get(key);
        if(value == null) {
            return defaultValue;
        }
        return converter.apply(value);
    }

    public static Integer getInt(String key, Integer defaultValue) {
        return getInt(key, Integer::parseInt, defaultValue);
    }

    public static Integer getInt(String key) {
        return getInt(key, Integer::parseInt, null);
    }

    public static Long getLong(String key, Function<String, Long> converter, Long defaultValue) {
        if(!initialized) {
            return defaultValue;
        }
        var value = dotenv.get(key);
        if(value == null) {
            return defaultValue;
        }
        return converter.apply(value);
    }

    public static Long getLong(String key, Long defaultValue) {
        return getLong(key, Long::parseLong, defaultValue);
    }

    public static Long getLong(String key) {
        return getLong(key, Long::parseLong, null);
    }
}
