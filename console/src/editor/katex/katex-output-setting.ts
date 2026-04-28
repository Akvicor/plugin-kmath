export type KatexOutput = "html" | "mathml" | "htmlAndMathml";

const CONFIG_MAP_NAME = "plugin-kmath-configMap";
const BASIC_GROUP_KEY = "basic";
const DEFAULT_KATEX_OUTPUT: KatexOutput = "mathml";

let cachedKatexOutput: KatexOutput = DEFAULT_KATEX_OUTPUT;
let loadingPromise: Promise<KatexOutput> | null = null;

function normalizeKatexOutput(value: unknown): KatexOutput {
  if (value === "html" || value === "mathml" || value === "htmlAndMathml") {
    return value;
  }
  return DEFAULT_KATEX_OUTPUT;
}

function parseKatexOutputFromConfigMap(
  data: Record<string, string> | undefined
): KatexOutput {
  const basicConfigRaw = data?.[BASIC_GROUP_KEY];
  if (!basicConfigRaw) {
    return DEFAULT_KATEX_OUTPUT;
  }

  try {
    const basicConfig = JSON.parse(basicConfigRaw) as Record<string, unknown>;
    return normalizeKatexOutput(basicConfig.katex_output);
  } catch {
    return DEFAULT_KATEX_OUTPUT;
  }
}

export function getKatexOutput(): KatexOutput {
  return cachedKatexOutput;
}

export async function initKatexOutputSetting(): Promise<KatexOutput> {
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
        cachedKatexOutput = parseKatexOutputFromConfigMap(configMap.data);
      } catch {
        cachedKatexOutput = DEFAULT_KATEX_OUTPUT;
      }
      return cachedKatexOutput;
    })();
  }

  return loadingPromise;
}
