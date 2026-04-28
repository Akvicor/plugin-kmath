package run.halo.kmath;

import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component("kmathHeadProcessor")
@RequiredArgsConstructor
public class KaTeXHeadProcessor implements TemplateHeadProcessor {

    static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER =
        new PropertyPlaceholderHelper("${", "}");

    private final PluginContext pluginContext;

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        final IModelFactory modelFactory = context.getModelFactory();
        return getHeadTags()
            .map(headTags -> {
                model.add(modelFactory.createText(headTags));
                return headTags;
            })
            .then();
    }

    private Mono<String> getHeadTags() {
        final Properties properties = new Properties();
        properties.setProperty("version", pluginContext.getVersion());

        return reactiveSettingFetcher.fetch("basic", BasicConfig.class)
            .defaultIfEmpty(new BasicConfig())
            .map(basicConfig -> {
                String engine = basicConfig.getRender_engine();
                if (engine == null || engine.isBlank()) {
                    engine = "katex";
                }

                StringBuilder headTags = new StringBuilder();
                headTags.append("<!-- plugin-kmath start -->\n");

                if ("mathjax".equalsIgnoreCase(engine)) {
                    headTags.append("""
                        <style>
                          mjx-container[jax="SVG"] {
                            display: inline-block;
                            vertical-align: middle;
                          }
                          mjx-container[jax="SVG"] > svg {
                            display: inline-block !important;
                          }
                          mjx-container[jax="SVG"][display="true"] {
                            display: block;
                            text-align: center;
                            margin: 1em 0;
                          }
                          mjx-container[jax="SVG"][display="true"] > svg {
                            display: block !important;
                            margin: 0 auto;
                          }
                        </style>
                    """);
                    if (basicConfig.isEnable_frontend_render()) {
                        // 完全本地化的 MathJax 配置
                        headTags.append("""
                            <script>
                              window.MathJax = {
                                loader: {
                                  paths: { mathjax: '/plugins/plugin-kmath/assets/static/mathjax' }
                                },
                                tex: {
                                  inlineMath: [ ['$', '$'], ['\\\\(', '\\\\)'] ],
                                  displayMath: [ ['$$', '$$'], ['\\\\[', '\\\\]'] ]
                                },
                                svg: { fontCache: 'local' },
                                startup: { typeset: false }
                              };
                            </script>
                        """);
                        headTags.append(
                            "<script async "
                                + "src=\"/plugins/plugin-kmath/assets/static/mathjax/tex-svg.js?version=${version}\">"
                                + "</script>\n"
                        );
                    }
                } else {
                    headTags.append(
                        "<link rel=\"stylesheet\" "
                            + "href=\"/plugins/plugin-kmath/assets/static/katex.min.css?version=${version}\" />\n"
                    );
                    if (basicConfig.isEnable_frontend_render()) {
                        headTags.append(
                            "<script "
                                + "src=\"/plugins/plugin-kmath/assets/static/katex.min.js?version=${version}\">"
                                + "</script>\n"
                        );
                    }
                }

                headTags.append("<!-- plugin-kmath end -->\n");

                return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(headTags.toString(), properties);
            });
    }
}
