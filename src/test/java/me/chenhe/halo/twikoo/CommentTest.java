package me.chenhe.halo.twikoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import run.halo.app.plugin.SettingFetcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * {@link Comment} 渲染逻辑单元测试。
 *
 * <p>重点覆盖：配置缺失时的空指针防护、未配置时的降级渲染、以及配置值的输出转义。
 */
class CommentTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private IProcessableElementTag tag;
    private IElementTagStructureHandler handler;

    @BeforeEach
    void setUp() {
        tag = mock(IProcessableElementTag.class);
        handler = mock(IElementTagStructureHandler.class);
    }

    @Test
    void shouldRenderTwikooScriptWhenConfigured() {
        var html = render(settings("https://cdn.example.com/twikoo.js", "env-abc"));

        assertThat(html).contains("id=\"twikoo-comment\"");
        assertThat(html).contains("envId: 'env-abc'");
        assertThat(html).contains("https://cdn.example.com/twikoo.js");
        assertThat(html).doesNotContain("尚未配置");
    }

    @Test
    void shouldNotThrowWhenSettingGroupIsMissing() {
        // 用户安装插件后未保存配置时，getSettingValue 返回 null
        var html = render(null);

        assertThat(html).contains("尚未配置");
        assertThat(html).doesNotContain("twikoo.init");
    }

    @Test
    void shouldNotThrowWhenFieldsAreMissing() {
        var html = render(MAPPER.createObjectNode());

        assertThat(html).contains("尚未配置");
    }

    @Test
    void shouldRenderPlaceholderWhenEnvIdIsBlank() {
        var html = render(settings("https://cdn.example.com/twikoo.js", "   "));

        assertThat(html).contains("尚未配置");
    }

    @Test
    void shouldFallbackToDefaultJsUrlWhenMissing() {
        var settings = MAPPER.createObjectNode();
        settings.put("envId", "env-abc");

        var html = render(settings);

        assertThat(html).contains("https://cdn.jsdelivr.net/npm/twikoo@1.7.19/dist/twikoo.min.js");
    }

    @Test
    void shouldEscapeEnvIdToPreventScriptInjection() {
        var html = render(settings("https://cdn.example.com/twikoo.js", "a';alert(1);//"));

        assertThat(html).doesNotContain("envId: 'a';alert(1);//'");
        assertThat(html).contains("\\'");
    }

    @Test
    void shouldEscapeJsUrlToPreventAttributeInjection() {
        var html = render(settings("https://cdn.example.com\"/><script>alert(1)</script>", "env-abc"));

        assertThat(html).doesNotContain("\"/><script>alert(1)</script>");
    }

    @Test
    void shouldUseCustomContainerIdFromTag() {
        when(tag.getAttributeValue("id")).thenReturn("post-comment");

        var html = render(settings("https://cdn.example.com/twikoo.js", "env-abc"));

        assertThat(html).contains("id=\"post-comment\"");
        assertThat(html).contains("el:'#post-comment'");
    }

    @Test
    void shouldIgnoreIllegalContainerId() {
        when(tag.getAttributeValue("id")).thenReturn("a';alert(1);//");

        var html = render(settings("https://cdn.example.com/twikoo.js", "env-abc"));

        assertThat(html).contains("id=\"twikoo-comment\"");
    }

    @Test
    void shouldLoadScriptWithStaticTag() {
        // 静态 script 标签走浏览器原生资源加载流程，可避免移动端内核拦截动态插入的外链脚本
        var html = render(settings("https://cdn.example.com/twikoo.js", "env-abc"));

        assertThat(html)
            .contains("<script src=\"https://cdn.example.com/twikoo.js\"></script>");
    }

    @Test
    void shouldRenderDistinctFailureTips() {
        var html = render(settings("https://cdn.example.com/twikoo.js", "env-abc"));

        // 脚本加载失败与 envId 配置错误需分别提示，避免误导排查方向
        assertThat(html).contains("评论脚本加载失败");
        assertThat(html).contains("评论组件初始化失败");
    }

    private ObjectNode settings(String jsUrl, String envId) {
        var settings = MAPPER.createObjectNode();
        settings.put("jsUrl", jsUrl);
        settings.put("envId", envId);
        return settings;
    }

    private String render(JsonNode settings) {
        var settingFetcher = mock(SettingFetcher.class);
        when(settingFetcher.getSettingValue("basic")).thenReturn(settings);

        new Comment(settingFetcher).render(null, tag, handler);

        var captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(handler).replaceWith(captor.capture(), anyBoolean());
        return captor.getValue().toString();
    }
}
