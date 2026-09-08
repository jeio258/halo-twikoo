package me.chenhe.halo.twikoo;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import run.halo.app.plugin.SettingFetcher;
import run.halo.app.theme.dialect.CommentWidget;
import tools.jackson.databind.JsonNode;

@Component
public class Comment implements CommentWidget {

    /** 与 extensions/settings.yaml 中 jsUrl 的默认值保持一致。 */
    private static final String DEFAULT_JS_URL =
        "https://cdn.jsdelivr.net/npm/twikoo@1.7.19/dist/twikoo.min.js";

    private static final String DEFAULT_CONTAINER_ID = "twikoo-comment";

    /** 容器 id 白名单，避免主题传入的属性值被原样拼进 JS 字符串。 */
    private static final String CONTAINER_ID_PATTERN = "^[A-Za-z][A-Za-z0-9_-]{0,63}$";

    /**
     * 模板占位符。这里刻意不使用 {@code String#formatted}：模板中含 CSS 百分号（如 {@code width:100%}），
     * 一旦漏写 {@code %%} 就会抛 UnknownFormatConversionException 导致整页渲染失败。
     */
    private static final Pattern PLACEHOLDER =
        Pattern.compile("\\{\\{(JS_URL_JS|CONTAINER_ID|ENV_ID|JS_URL)\\}\\}");

    /**
     * 脚本一律使用静态 {@code <script src>} 标签加载：UC / QQ / 微信 webview 等移动端内核的
     * 「省流加速 / 云端转码」会剥离或拦截运行时动态插入的外链脚本，而静态标签走浏览器
     * 原生资源加载流程，兼容性最好。仅当静态脚本确实未生效时，才动态重试一次。
     *
     * <p>内联脚本刻意顶格且不写注释：这段文本会原样进入前端，缩进与注释都会直接增加产物体积。
     */
    private static final String TEMPLATE = """
        <div id="{{CONTAINER_ID}}" style="width:100%;min-height:400px;"></div>
        <script src="{{JS_URL}}"></script>
        <script>
        (function(){
        var el=document.getElementById('{{CONTAINER_ID}}');
        if(!el||el.dataset.twikooInited==='true')return;
        el.dataset.twikooInited='true';
        var URL='{{JS_URL_JS}}';
        function fail(tip){
        console.error('[halo-twikoo] '+tip+' 脚本地址: '+URL);
        el.innerHTML='<p style="padding:16px;color:#999;">'+tip+'</p>';
        }
        function init(){
        try{
        twikoo.init({envId: '{{ENV_ID}}',el:'#{{CONTAINER_ID}}',media:true,ready:function(){
        var box=el.querySelector('.twikoo')||el;
        box.style.width='100%';box.style.minHeight='400px';box.style.display='block';box.style.visibility='visible';
        if(typeof MutationObserver!=='function')return;
        var n=0,t=null;
        var ob=new MutationObserver(function(){
        clearTimeout(t);
        t=setTimeout(function(){
        var c=el.querySelector('.twikoo')||el;
        c.style.display='block';c.style.visibility='visible';
        if(++n>=10)ob.disconnect();
        },100);
        });
        ob.observe(box,{childList:true,subtree:true});
        }});
        }catch(e){
        console.error('[halo-twikoo] twikoo.init 执行异常',e);
        fail('评论组件初始化失败，请检查 envId 配置是否正确。');
        }
        }
        function start(){
        if(typeof twikoo==='undefined'){
        var s=document.createElement('script');
        s.src=URL;
        s.onload=function(){typeof twikoo==='undefined'?fail('评论脚本已加载但未注册组件，请检查前端脚本地址是否为 Twikoo 完整版。'):init();};
        s.onerror=function(){fail('评论脚本加载失败，请检查网络；若站点为 HTTPS，请确认脚本地址同为 HTTPS。');};
        document.head.appendChild(s);
        return;
        }
        init();
        }
        if(typeof window.requestAnimationFrame==='function'){
        window.requestAnimationFrame(function(){window.requestAnimationFrame(start);});
        }else{
        setTimeout(start,300);
        }
        })();
        </script>
        """;

    private final SettingFetcher settingFetcher;

    public Comment(SettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    @Override
    public void render(ITemplateContext context, IProcessableElementTag tag,
        IElementTagStructureHandler structureHandler) {

        // SettingFetcher#getSettingValue 自 Halo 2.23.0 起替代已废弃的 get。
        // 注意：插件配置尚未保存（ConfigMap 不存在）时该调用返回 null，必须判空。
        var settings = settingFetcher.getSettingValue("basic");
        final var jsUrl = textValue(settings, "jsUrl", DEFAULT_JS_URL);
        final var envId = textValue(settings, "envId", "");
        final var containerId = resolveContainerId(tag);

        if (envId.isBlank()) {
            // 未配置时优雅降级，避免抛异常导致整个页面渲染失败
            structureHandler.replaceWith(
                "<div class=\"twikoo-placeholder\" style=\"padding:16px;color:#999;\">"
                    + "评论组件尚未配置，请在「插件设置 - Twikoo」中填写 envId。</div>", false);
            return;
        }

        // 输出编码：containerId 已过白名单；jsUrl 同时出现在 HTML 属性与 JS 字符串中，
        // 两种上下文需分别转义；envId 按 JS 字符串转义
        var html = render(TEMPLATE, Map.of(
            "CONTAINER_ID", containerId,
            "ENV_ID", escapeJsString(envId),
            "JS_URL", escapeHtmlAttr(jsUrl),
            "JS_URL_JS", escapeJsString(jsUrl)));
        structureHandler.replaceWith(html, false);
    }

    /**
     * 单次扫描替换占位符，替换值不会被再次扫描，避免嵌套注入。
     */
    private static String render(String template, Map<String, String> values) {
        var out = new StringBuilder(template.length() + 128);
        var matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            matcher.appendReplacement(out,
                Matcher.quoteReplacement(values.getOrDefault(matcher.group(1), "")));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * 解析评论容器 id：允许主题通过 {@code <halo:comment id="xxx" />} 指定，否则使用默认值。
     */
    private static String resolveContainerId(IProcessableElementTag tag) {
        var id = tag == null ? null : tag.getAttributeValue("id");
        if (id == null || !id.matches(CONTAINER_ID_PATTERN)) {
            return DEFAULT_CONTAINER_ID;
        }
        return id;
    }

    /**
     * 安全读取配置值：配置分组、字段缺失或为空时返回兜底值。
     */
    private static String textValue(JsonNode settings, String key, String fallback) {
        if (settings == null) {
            return fallback;
        }
        var node = settings.get(key);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        var value = node.asString();
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * JS 单引号字符串上下文转义：防止配置值逃逸字符串边界造成注入或语法错误。
     */
    private static String escapeJsString(String raw) {
        return raw.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("</", "<\\/");
    }

    /**
     * HTML 属性上下文转义：防止配置值闭合 {@code src="..."} 属性造成注入。
     */
    private static String escapeHtmlAttr(String raw) {
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
