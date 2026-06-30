package com.yousheng.knowledgehub.ai.tool.note;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoteReadToolsToolCallbacksTest {

    @Test
    void noteReadTools_hasFourToolMethods() {
        NoteReadToolFacade facade = mock(NoteReadToolFacade.class);
        NoteReadTools tools = new NoteReadTools(facade);

        Method[] methods = tools.getClass().getDeclaredMethods();
        long toolMethodCount = Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .count();

        assertThat(toolMethodCount).isEqualTo(4);
    }

    @Test
    void toolMethodNamesAreCorrect() {
        NoteReadToolFacade facade = mock(NoteReadToolFacade.class);
        NoteReadTools tools = new NoteReadTools(facade);

        Set<String> toolNames = Arrays.stream(tools.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrder(
                "search_my_notes",
                "get_my_note_detail",
                "list_my_notes",
                "list_my_published_notes"
        );
    }

    @Test
    void toolMethodsHaveDescriptions() {
        NoteReadToolFacade facade = mock(NoteReadToolFacade.class);
        NoteReadTools tools = new NoteReadTools(facade);

        Method[] methods = tools.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool != null) {
                assertThat(tool.description())
                        .as("Tool %s should have a description", method.getName())
                        .isNotBlank();
            }
        }
    }

    @Test
    void search_my_notes_hasPageAndSizeParamsNotRequired() {
        NoteReadToolFacade facade = mock(NoteReadToolFacade.class);
        NoteReadTools tools = new NoteReadTools(facade);

        try {
            Method method = tools.getClass().getDeclaredMethod(
                    "search_my_notes", String.class, Integer.class, Integer.class);

            Parameter[] params = method.getParameters();
            assertThat(params).hasSize(3);

            Parameter pageParam = params[1];
            ToolParam pageToolParam = pageParam.getAnnotation(ToolParam.class);
            assertThat(pageToolParam).isNotNull();
            assertThat(pageToolParam.required()).isFalse();

            Parameter sizeParam = params[2];
            ToolParam sizeToolParam = sizeParam.getAnnotation(ToolParam.class);
            assertThat(sizeToolParam).isNotNull();
            assertThat(sizeToolParam.required()).isFalse();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Expected method not found", e);
        }
    }

    @Test
    void list_my_published_notes_hasPageAndSizeParamsNotRequired() {
        NoteReadToolFacade facade = mock(NoteReadToolFacade.class);
        NoteReadTools tools = new NoteReadTools(facade);

        try {
            Method method = tools.getClass().getDeclaredMethod(
                    "list_my_published_notes", Integer.class, Integer.class);

            Parameter[] params = method.getParameters();
            assertThat(params).hasSize(2);

            Parameter pageParam = params[0];
            ToolParam pageToolParam = pageParam.getAnnotation(ToolParam.class);
            assertThat(pageToolParam).isNotNull();
            assertThat(pageToolParam.required()).isFalse();

            Parameter sizeParam = params[1];
            ToolParam sizeToolParam = sizeParam.getAnnotation(ToolParam.class);
            assertThat(sizeToolParam).isNotNull();
            assertThat(sizeToolParam.required()).isFalse();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Expected method not found", e);
        }
    }
}
