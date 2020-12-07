package io.codecs;

import java.util.Properties;

/**
 * @author Lu Xugang
 * @date 2020/9/25 1:59 下午
 */
public class ReadProperties {
    public static void main(String[] args) {
        Properties properties = System.getProperties();
        System.out.println("start");
        System.out.println(properties.getProperty("file.encoding"));
        System.out.println("end");
    }
}
