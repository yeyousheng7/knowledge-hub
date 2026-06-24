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
        sb.append("你是一个知识库助手。请仅根据下面提供的笔记片段回答用户的问题。\n");
        sb.append("如果笔记片段中没有足够的信息来回答问题，请明确说明\"笔记中没有足够信息来回答这个问题\"。\n\n");
        sb.append("笔记片段：\n");

        for (int i = 0; i < hits.size(); i++) {
            AiIndexSearchHit hit = hits.get(i);
            sb.append("---\n");
            sb.append("标题：").append(hit.title()).append("\n");
            sb.append("内容：").append(hit.chunkText()).append("\n");
        }

        sb.append("\n用户问题：").append(question).append("\n\n");
        sb.append("请回答：");

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
