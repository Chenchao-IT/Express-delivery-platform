package com.campus.express.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "demo.data")
@Data
public class DemoDataProperties {

    private Admin admin = new Admin();
    private String defaultPassword = "123456";
    private Balance balance = new Balance();
    private Targets targets = new Targets();
    private List<Courier> couriers = new ArrayList<>();
    private List<Student> students = new ArrayList<>();
    private List<String> destinations = new ArrayList<>();

    @Data
    public static class Admin {
        private String username = "admin";
        private String password = "admin123";
        private String realName = "系统管理员";
    }

    @Data
    public static class Balance {
        private BigDecimal courierMinimum = new BigDecimal("200.00");
        private BigDecimal studentMinimum = new BigDecimal("100.00");
    }

    @Data
    public static class Targets {
        private int packages = 24;
        private int scheduledTasks = 8;
        private int rewardTasks = 6;
    }

    @Data
    public static class Courier {
        private String username;
        private String realName;
        private String phone;
        private String password;
    }

    @Data
    public static class Student {
        private String username;
        private String realName;
        private String phone;
        private String college;
        private String address;
        private String password;
    }
}
