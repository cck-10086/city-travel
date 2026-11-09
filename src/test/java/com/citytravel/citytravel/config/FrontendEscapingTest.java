package com.citytravel.citytravel.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 前端 XSS 防护的静态检查。
 * 单页应用的渲染逻辑全在 index.html 里，没法用 JUnit 直接跑，
 * 这里退一步做源码级断言：确保转义函数存在，且已知的注入点都已套上 esc()。
 */
@DisplayName("前端模板转义")
class FrontendEscapingTest {

    private static final Path INDEX = Paths.get("src/main/resources/static/index.html");

    private String html() throws IOException {
        return new String(Files.readAllBytes(INDEX), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("index.html 必须定义 esc() 转义函数")
    void shouldDefineEscapeHelper() throws IOException {
        String html = html();
        assertThat(html).contains("function esc(value)");
        assertThat(html).contains(".replace(/&/g, '&amp;')");
        assertThat(html).contains(".replace(/</g, '&lt;')");
        assertThat(html).contains(".replace(/>/g, '&gt;')");
    }

    @Test
    @DisplayName("攻略正文与评论等用户可控字段不得裸插值进 innerHTML")
    void shouldNotRenderUserContentRaw() throws IOException {
        String html = html();
        // 这些字段来自用户输入，直接拼进 innerHTML 就是存储型 XSS
        assertThat(html).doesNotContain(">${strategy.content}<");
        assertThat(html).doesNotContain(">${comment.content}");
        assertThat(html).doesNotContain(">${c.content}<");
        assertThat(html).doesNotContain(">${comment.userName}<");

        // 对应的转义版本必须存在
        assertThat(html).contains("esc(strategy.content)");
        assertThat(html).contains("esc(comment.content)");
    }

    @Test
    @DisplayName("esc() 覆盖的商品字段数量应保持稳定，防止后续改动漏掉")
    void shouldEscapeProductFields() throws IOException {
        String html = html();
        assertThat(html).contains("esc(item.name)");
        assertThat(html).contains("esc(item.description || '暂无描述')");
        assertThat(html).contains("esc(item.location)");
    }
}
