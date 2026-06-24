package com.yousheng.knowledgehub.ai.index.controller;

import com.yousheng.knowledgehub.ai.index.AiIndexWriteResult;
import com.yousheng.knowledgehub.ai.index.AiIndexWriterService;
import com.yousheng.knowledgehub.ai.index.dto.AiIndexRebuildResponse;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.common.response.ApiResponse;
import com.yousheng.knowledgehub.config.openapi.OpenApiConfig;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-index", description = "AI 笔记索引接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai/index")
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
@ConditionalOnProperty(prefix = "app.ai.index", name = "vector-store", havingValue = "redis")
public class AiIndexController {

    private final AiIndexWriterService aiIndexWriterService;
    private final AppUserMapper appUserMapper;

    @PostMapping("/rebuild")
    public ApiResponse<AiIndexRebuildResponse> rebuild() {
        Long userId = requireCurrentEnabledUserId();
        AiIndexWriteResult result = aiIndexWriterService.rebuildUserIndex(userId);
        return ApiResponse.ok(toResponse(result));
    }

    private AiIndexRebuildResponse toResponse(AiIndexWriteResult result) {
        return new AiIndexRebuildResponse(
                result.userId(),
                result.chunkCount(),
                result.indexedAt()
        );
    }

    private Long requireCurrentEnabledUserId() {
        Long userId = CurrentUser.getUserId();
        AppUser user = appUserMapper.selectById(userId);

        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!UserStatus.ENABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        return userId;
    }
}
