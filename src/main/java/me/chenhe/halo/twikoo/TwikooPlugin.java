package me.chenhe.halo.twikoo;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * 插件主类。原实现中的 {@code start()} / {@code stop()} 为空重写，已删除——
 * {@link BasePlugin} 并未定义这两个方法，默认行为继承自 pf4j {@code Plugin}。
 */
@Component
public class TwikooPlugin extends BasePlugin {

    public TwikooPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }
}
