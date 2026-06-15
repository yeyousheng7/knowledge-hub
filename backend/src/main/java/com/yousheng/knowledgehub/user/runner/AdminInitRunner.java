package com.yousheng.knowledgehub.user.runner;

import com.yousheng.knowledgehub.user.service.AdminInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AdminInitRunner implements ApplicationRunner {
    private final AdminInitService adminInitService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        adminInitService.initializeAdminIfNecessary();
    }
}
