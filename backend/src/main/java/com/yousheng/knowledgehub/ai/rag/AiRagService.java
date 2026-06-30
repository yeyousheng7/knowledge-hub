package com.yousheng.knowledgehub.ai.rag;

import com.yousheng.knowledgehub.ai.chat.AiChatClient;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchHit;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchResult;
import com.yousheng.knowledgehub.ai.index.AiIndexSearchService;
import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class AiRagService {

    private static final Logger log = LoggerFactory.getLogger(AiRagService.class);
    private static final String NO_ANSWER = "没有在你的笔记中找到足够相关的内容。";

    private final AiIndexSearchService searchService;
    private final AiChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是 KHub 的个人知识库 RAG 助手。
            你的主要任务是基于下面提供的笔记片段回答用户的问题。

            回答策略：
            1. 如果用户只是打招呼、询问你是谁、询问你能做什么、询问如何使用本功能，可以简短自然地回应，并引导用户提出与笔记相关的问题。
            2. 如果用户提出的是模糊学习主题，例如“我想了解 Spring”“帮我复习 Redis”，请结合检索到的笔记片段，给出学习建议、阅读顺序、相关笔记推荐或进一步提问方向。
            3. 如果用户询问具体知识、事实、项目细节、笔记内容或要求总结，请优先根据笔记片段回答。
            4. 如果笔记片段不足以支持具体回答，请说明“笔记中没有足够信息来回答这个问题”，并建议用户换个关键词、补充问题或先创建相关笔记。
            5. 不要编造不存在的笔记内容，不要声称看过未提供的笔记。
            6. 当前 RAG 问答没有长期记忆，不要声称记得之前的对话。

            来源链接规则：
            - 当回答引用了笔记片段时，在相关句子或段落末尾添加来源链接：[《笔记标题》](kh-source://note/{noteId})。
            - 只能使用下面提供的真实 noteId 和标题，不要编造。
            - 如果只是打招呼、功能说明或没有可靠笔记依据，不要添加来源链接。

            笔记片段：
            """;

    public AiRagAnswer ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }

        String trimmedQuestion = question.trim();
        AiIndexSearchResult searchResult = searchService.search(trimmedQuestion);
        List<AiIndexSearchHit> hits = searchResult.hits();

        if (hits.isEmpty()) {
            return new AiRagAnswer(NO_ANSWER, Collections.emptyList());
        }

        String prompt = buildPrompt(trimmedQuestion, hits);
        String answer = chat(prompt, trimmedQuestion.length(), hits.size());

        List<AiRagSource> sources = hits.stream()
                .map(AiRagService::toSource)
                .toList();

        return new AiRagAnswer(answer, sources);
    }

    private String chat(String prompt, int questionLength, int sourceCount) {
        try {
            return chatClient.chat(prompt);
        } catch (RuntimeException ex) {
            log.warn("AI chat call failed, questionLength={}, sourceCount={}", questionLength, sourceCount, ex);
            throw new BizException(ErrorCode.AI_CHAT_SERVICE_UNAVAILABLE, ex);
        }
    }

    private String buildPrompt(String question, List<AiIndexSearchHit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT);

        for (int i = 0; i < hits.size(); i++) {
            AiIndexSearchHit hit = hits.get(i);
            sb.append("---\n");
            sb.append("笔记ID：").append(hit.noteId()).append("\n");
            sb.append("标题：").append(hit.title()).append("\n");
            sb.append("内容：").append(hit.chunkText()).append("\n");
        }

        sb.append("\n用户问题：").append(question).append("\n\n");
        sb.append("请根据上述规则回答：");

        return sb.toString();
    }

    private static AiRagSource toSource(AiIndexSearchHit hit) {
        return new AiRagSource(
                hit.noteId(),
                hit.title(),
                hit.chunkText(),
                hit.chunkIndex(),
                hit.distance(),
                hit.visibility(),
                hit.updatedAt()
        );
    }
}
