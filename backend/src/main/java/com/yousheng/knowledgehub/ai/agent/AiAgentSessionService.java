package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUser;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.Objects;

public class AiAgentSessionService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentSessionService.class);

    static final String CONVERSATION_ID_PREFIX = "kh:ai:agent:session:";
    static final String CONVERSATION_ID_SUFFIX = ":current";

    private final ChatMemory chatMemory;
    private final AppUserMapper appUserMapper;

    public AiAgentSessionService(ChatMemory chatMemory, AppUserMapper appUserMapper) {
        this.chatMemory = chatMemory;
        this.appUserMapper = Objects.requireNonNull(appUserMapper, "appUserMapper must not be null");
    }

    public Long requireCurrentEnabledUserId() {
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

    public String generateConversationId() {
        Long userId = requireCurrentEnabledUserId();
        return generateConversationId(userId);
    }

    public String generateConversationId(Long userId) {
        return CONVERSATION_ID_PREFIX + userId + CONVERSATION_ID_SUFFIX;
    }

    public void clearSession() {
        Long userId = requireCurrentEnabledUserId();
        String conversationId = generateConversationId(userId);
        if (chatMemory != null) {
            chatMemory.clear(conversationId);
        }
        log.debug("Cleared current user's agent session");
    }
}
