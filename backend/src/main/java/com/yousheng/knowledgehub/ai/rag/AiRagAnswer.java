package com.yousheng.knowledgehub.ai.rag;

import java.util.Collections;
import java.util.List;

public record AiRagAnswer(
        String answer,
        List<AiRagSource> sources
) {
    public AiRagAnswer {
        sources = sources != null ? List.copyOf(sources) : Collections.emptyList();
    }
}
