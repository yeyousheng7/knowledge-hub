package com.yousheng.knowledgehub.ai.agent;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import com.yousheng.knowledgehub.security.CurrentUserPrincipal;
import com.yousheng.knowledgehub.user.entity.AppUser;
import com.yousheng.knowledgehub.user.enums.UserStatus;
import com.yousheng.knowledgehub.user.mapper.AppUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAgentSessionServiceTest {

    private static final long TEST_USER_ID = 1L;

    private AppUserMapper appUserMapper;

    @BeforeEach
    void setUp() {
        appUserMapper = mock(AppUserMapper.class);
        when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(user(UserStatus.ENABLED.name()));

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                new CurrentUserPrincipal(TEST_USER_ID, "testuser", "USER"),
                null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateConversationId_hasCorrectPrefix() {
        AiAgentSessionService service = service(null);

        String id = service.generateConversationId();

        assertThat(id).isEqualTo("kh:ai:agent:session:" + TEST_USER_ID + ":current");
    }

    @Test
    void clearSession_memoryDisabled_noOpSucceeds() {
        AiAgentSessionService service = service(null);

        service.clearSession();

        verify(appUserMapper).selectById(TEST_USER_ID);
    }

    @Test
    void clearSession_disabledUser_throwsUserDisabled() {
        when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(user(UserStatus.DISABLED.name()));
        AiAgentSessionService service = service(null);

        assertThatThrownBy(service::clearSession)
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.USER_DISABLED);
                });
    }

    @Test
    void clearSession_missingUser_throwsUnauthorized() {
        when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(null);
        AiAgentSessionService service = service(null);

        assertThatThrownBy(service::clearSession)
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizEx = (BizException) ex;
                    assertThat(bizEx.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                });
    }

    @Test
    void clearSession_memoryEnabled_clearsCurrentUserConversation() {
        InMemoryChatMemoryRepository repo = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo).maxMessages(20).build();
        AiAgentSessionService service = service(chatMemory);

        String conversationId = service.generateConversationId();
        List<Message> messages = List.of(
                new UserMessage("hello"),
                new AssistantMessage("hi there"));
        chatMemory.add(conversationId, messages);

        assertThat(chatMemory.get(conversationId)).isNotEmpty();
        assertThat(repo.findByConversationId(conversationId)).isNotEmpty();

        service.clearSession();

        assertThat(chatMemory.get(conversationId)).isEmpty();
        assertThat(repo.findByConversationId(conversationId)).isEmpty();
    }

    @Test
    void clearSession_onlyClearsCurrentUser() {
        InMemoryChatMemoryRepository repo = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo).maxMessages(20).build();
        AiAgentSessionService service = service(chatMemory);

        String currentId = service.generateConversationId();
        String otherId = "kh:ai:agent:session:99999:current";

        List<Message> messages = List.of(new UserMessage("hello"));
        chatMemory.add(currentId, messages);
        chatMemory.add(otherId, messages);

        service.clearSession();

        assertThat(chatMemory.get(currentId)).isEmpty();
        assertThat(chatMemory.get(otherId)).isNotEmpty();
    }

    private AiAgentSessionService service(ChatMemory chatMemory) {
        return new AiAgentSessionService(chatMemory, appUserMapper);
    }

    private static AppUser user(String status) {
        AppUser user = new AppUser();
        user.setId(TEST_USER_ID);
        user.setStatus(status);
        return user;
    }
}
