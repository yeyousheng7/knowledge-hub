package com.yousheng.knowledgehub.ai.tool.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DemoActionTools {

    @Tool(name = "prepare_demo_action", returnDirect = true, description = """
            Prepare a demo structured action for frontend rendering.
            Use this only when the user explicitly asks to demonstrate structured actions.
            This tool is terminal: it does not write data, does not confirm operations, and does not execute business actions.""")
    public String prepareDemoAction(
            @ToolParam(description = "Short user-visible preview text for this demo action") String preview) {
        String safePreview = preview == null || preview.isBlank()
                ? "Demo structured action prepared."
                : preview;
        return """
                {"answer":"Demo action prepared. No real business operation was executed.","actions":[{"type":"DEMO_ACTION","payload":{"preview":%s,"source":"returnDirect-spike"}}]}
                """.formatted(toJsonString(safePreview));
    }

    private static String toJsonString(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
