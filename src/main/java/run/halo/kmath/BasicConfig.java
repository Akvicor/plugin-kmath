package run.halo.kmath;

import lombok.Data;

@Data
public class BasicConfig {
    boolean enable_frontend_render;
    String inline_selector;
    String display_selector;
    String render_engine;
    String katex_output;
}
