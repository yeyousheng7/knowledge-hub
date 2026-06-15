package com.yousheng.knowledgehub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("app.admin.init")
public class AdminInitProperties {
    boolean enabled;
    String username;
    String password;
    String nickname;
}
