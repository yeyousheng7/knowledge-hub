package com.yousheng.knowledgehub.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
@Validated
public class InviteCodeProperties {
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
