package com.thalia.fisioterapia.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class UsersProperties {

    private List<UserEntry> users = new ArrayList<>();

    @Data
    public static class UserEntry {
        private String email;
        private String password;
        private String role;
    }
}
