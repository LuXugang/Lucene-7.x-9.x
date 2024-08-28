package org.example;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.simulator.Simulator;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        // 创建一个带有最大大小和过期策略的缓存
        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(1000) // 最大缓存条目数
                .expireAfterWrite(10, TimeUnit.MINUTES) // 写入后10分钟过期
                .refreshAfterWrite(1, TimeUnit.MINUTES) // 写入后1分钟自动刷新
                .removalListener((String key, String value, RemovalCause cause) ->
                        System.out.println("Removed: " + key + " due to " + cause)) // 移除监听器
                .build(key -> loadValueForKey(key)); // 异步加载策略

        // 使用缓存
        String key1 = "key1";
        String value1 = cache.get(key1); // 加载值
        System.out.println("Loaded value: " + value1);

        // 手动放入值
        cache.put("key2", "value2");
        System.out.println("Manually put key2 with value: value2");

        // 获取并打印值
        String value2 = cache.getIfPresent("key2");
        System.out.println("Cached value for key2: " + value2);

        // 模拟等待以触发过期
        try {
            Thread.sleep(60000); // 等待1分钟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 再次获取值，触发自动刷新
        String refreshedValue1 = cache.get(key1);
        System.out.println("Refreshed value for key1: " + refreshedValue1);
    }

    // 模拟加载方法
    private static String loadValueForKey(String key) {
        System.out.println("Loading value for key: " + key);
        return "value_for_" + key;
    }
}