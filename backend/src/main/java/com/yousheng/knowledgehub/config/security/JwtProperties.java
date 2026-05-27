package com.yousheng.knowledgehub.config.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    @NotBlank(message = "JWT secret 不能为空")
    @Size(min = 32, message = "JWT secret 长度不能少于 32 个字符")
    private String secret;

    @Positive(message = "JWT 过期时间必须为正数")
    @Max(value = 604800, message = "JWT 过期时间不能超过 7 天")
    private long expireSeconds;
}
