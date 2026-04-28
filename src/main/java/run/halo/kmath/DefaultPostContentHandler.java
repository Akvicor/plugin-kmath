package run.halo.kmath;

import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.ReactivePostContentHandler;

@Component("kmathDefaultPostContentHandler")
@RequiredArgsConstructor
@Slf4j
public class DefaultPostContentHandler implements ReactivePostContentHandler {

    private static final String DEFAULT_INLINE_SELECTOR =
        "[math-inline],.katex-inline";

    private static final String DEFAULT_DISPLAY_SELECTOR =
        "[math-display],.katex-block";

    private static final String DEFAULT_KATEX_OUTPUT = "mathml";

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    private static void injectJS(PostContentContext contentContext, String engine,
        String inlineSelector, String displaySelector, String katexOutput) {
        String parsedScript =
            KaTeXJSInjector.getParsedScript(engine, inlineSelector, displaySelector, katexOutput);
        String content = contentContext.getContent() == null ? "" : contentContext.getContent();
        contentContext.setContent(content + "\n" + parsedScript);
    }

    @Override
    public Mono<PostContentContext> handle(PostContentContext contentContext) {
        return reactiveSettingFetcher.fetch("basic", BasicConfig.class)
            .defaultIfEmpty(new BasicConfig())
            .map(basicConfig -> {
                if (basicConfig.isEnable_frontend_render()) {
                    String engine = basicConfig.getRender_engine();
                    if (engine == null || engine.isBlank()) {
                        engine = "katex";
                    }
                    String inlineSelector = mergeSelectors(
                        nullToDefault(basicConfig.getInline_selector(), DEFAULT_INLINE_SELECTOR),
                        DEFAULT_INLINE_SELECTOR
                    );
                    String displaySelector = mergeSelectors(
                        nullToDefault(basicConfig.getDisplay_selector(), DEFAULT_DISPLAY_SELECTOR),
                        DEFAULT_DISPLAY_SELECTOR
                    );
                    String katexOutput = normalizeKatexOutput(
                        nullToDefault(basicConfig.getKatex_output(), DEFAULT_KATEX_OUTPUT)
                    );
                    injectJS(contentContext, engine, inlineSelector, displaySelector, katexOutput);
                }
                return contentContext;
            }).onErrorResume(e -> {
                log.error("plugin-kmath Post handle failed", Throwables.getRootCause(e));
                return Mono.just(contentContext);
            });
    }

    private static String nullToDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String mergeSelectors(String custom, String required) {
        Set<String> selectors = new LinkedHashSet<>();
        addSelectors(selectors, custom);
        addSelectors(selectors, required);
        return String.join(",", selectors);
    }

    private static void addSelectors(Set<String> selectors, String value) {
        Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(selector -> !selector.isEmpty())
            .forEach(selectors::add);
    }

    private static String normalizeKatexOutput(String value) {
        return switch (value) {
            case "html", "mathml", "htmlAndMathml" -> value;
            default -> DEFAULT_KATEX_OUTPUT;
        };
    }
}
