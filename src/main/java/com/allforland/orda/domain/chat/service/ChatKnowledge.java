package com.allforland.orda.domain.chat.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * 챗봇 시스템 프롬프트를 만든다. 안내 지침과 서비스 위키(classpath:chatbot/wiki/*.md)를 서버 시작 시 한 번 읽는다.
 *
 * <p>내용이 요청마다 바뀌지 않아야 프롬프트 캐시가 유지되므로, 시각 같은 값은 넣지 않는다.
 */
@Component
public class ChatKnowledge {
    private static final String INSTRUCTIONS = "classpath:chatbot/instructions.md";
    private static final String WIKI_PATTERN = "classpath:chatbot/wiki/*.md";

    private final String systemPrompt;

    public ChatKnowledge() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] pages = resolver.getResources(WIKI_PATTERN);
        if (pages.length == 0) {
            throw new IllegalStateException("No chatbot wiki pages found at " + WIKI_PATTERN);
        }
        Arrays.sort(pages, Comparator.comparing(Resource::getFilename));
        StringBuilder prompt = new StringBuilder(read(resolver.getResource(INSTRUCTIONS)).strip());
        prompt.append("\n\n<wiki>\n");
        for (Resource page : pages) {
            prompt.append("<page name=\"")
                    .append(page.getFilename())
                    .append("\">\n")
                    .append(read(page).strip())
                    .append("\n</page>\n");
        }
        prompt.append("</wiki>");
        this.systemPrompt = prompt.toString();
    }

    /** 안내 지침과 위키를 합친 시스템 프롬프트. */
    public String systemPrompt() {
        return systemPrompt;
    }

    private static String read(Resource resource) throws IOException {
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
