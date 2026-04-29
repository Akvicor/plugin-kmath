import { ensureMathJaxReady } from "./render-mathjax";

export type KatexOutput = "html" | "mathml" | "htmlAndMathml";
export type RenderEngine = "katex" | "mathjax";

const CONFIG_MAP_NAME = "plugin-kmath-configMap";
const BASIC_GROUP_KEY = "basic";
const DEFAULT_KATEX_OUTPUT: KatexOutput = "mathml";
const DEFAULT_RENDER_ENGINE: RenderEngine = "katex";

export interface EditorMathRenderSetting {
  katexOutput: KatexOutput;
  renderEngine: RenderEngine;
}

const DEFAULT_SETTING: EditorMathRenderSetting = {
  katexOutput: DEFAULT_KATEX_OUTPUT,
  renderEngine: DEFAULT_RENDER_ENGINE,
};

let cachedSetting: EditorMathRenderSetting = { ...DEFAULT_SETTING };
let loadingPromise: Promise<EditorMathRenderSetting> | null = null;

function normalizeKatexOutput(value: unknown): KatexOutput {
  if (value === "html" || value === "mathml" || value === "htmlAndMathml") {
    return value;
  }
  return DEFAULT_KATEX_OUTPUT;
}

function normalizeRenderEngine(value: unknown): RenderEngine {
  if (value === "katex" || value === "mathjax") {
    return value;
  }
  return DEFAULT_RENDER_ENGINE;
}

function parseSettingFromConfigMap(
  data: Record<string, string> | undefined
): EditorMathRenderSetting {
  const basicConfigRaw = data?.[BASIC_GROUP_KEY];
  if (!basicConfigRaw) {
    return { ...DEFAULT_SETTING };
  }

  try {
    const basicConfig = JSON.parse(basicConfigRaw) as Record<string, unknown>;
    return {
      katexOutput: normalizeKatexOutput(basicConfig.katex_output),
      renderEngine: normalizeRenderEngine(basicConfig.render_engine),
    };
  } catch {
    return { ...DEFAULT_SETTING };
  }
}

export function getKatexOutput(): KatexOutput {
  return cachedSetting.katexOutput;
}

export function getRenderEngine(): RenderEngine {
  return cachedSetting.renderEngine;
}

export async function initEditorMathRenderSetting(): Promise<EditorMathRenderSetting> {
  if (!loadingPromise) {
    loadingPromise = (async () => {
      try {
        const response = await fetch(
          `/api/v1alpha1/configmaps/${encodeURIComponent(CONFIG_MAP_NAME)}`,
          {
            method: "GET",
            credentials: "same-origin",
            headers: {
              Accept: "application/json",
            },
          }
        );

        if (!response.ok) {
          throw new Error(`request failed: ${response.status}`);
        }

        const configMap = (await response.json()) as {
          data?: Record<string, string>;
        };
        cachedSetting = parseSettingFromConfigMap(configMap.data);
      } catch {
        cachedSetting = { ...DEFAULT_SETTING };
      }

      if (cachedSetting.renderEngine === "mathjax") {
        try {
          await ensureMathJaxReady();
        } catch (error) {
          console.error("MathJax init failed, fallback to KaTeX:", error);
          cachedSetting = {
            ...cachedSetting,
            renderEngine: DEFAULT_RENDER_ENGINE,
          };
        }
      }

      return cachedSetting;
    })();
  }

  return loadingPromise;
}
