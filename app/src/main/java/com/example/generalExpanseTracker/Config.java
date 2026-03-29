package com.example.generalExpanseTracker;

import android.content.Context;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Config {

    private static Map<String, Object> config;

    public static void load(Context context) {

        try {
            InputStream input = context.getAssets().open("config.yml");
            Yaml yaml = new Yaml();
            config = yaml.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String path) {

        String[] keys = path.split("\\.");
        Object value = config;

        for (String key : keys) {
            value = ((Map) value).get(key);
        }

        return value.toString();
    }
}