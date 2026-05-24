package com.yousheng.knowledgehub.system;

import com.yousheng.knowledgehub.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PingController {
    @Operation(summary = "Ping test")
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }
}
