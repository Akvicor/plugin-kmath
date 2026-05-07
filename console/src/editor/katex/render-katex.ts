import katex from "katex";
import { getKatexOutput, getRenderEngine } from "./katex-output-setting";
import { renderMathJaxSvgHtml } from "./render-mathjax";

export type SerializedMathAttributes = {
  renderedContent: string;
  renderedEngine: "" | "mathjax";
  renderedHtml: string;
};

const EMPTY_RENDERED_MATH: SerializedMathAttributes = {
  renderedContent: "",
  renderedEngine: "",
  renderedHtml: "",
};

/**
 * 编辑器侧公式渲染入口。
 *
 * 根据插件配置选择 KaTeX 或 MathJax。
 */
function escapeHtml(content: string): string {
  return content
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function renderKatex(content: string, isInline: boolean): string {
  const output = getKatexOutput();

  return katex.renderToString(content, {
    throwOnError: false,
    strict: false,
    displayMode: !isInline,
    maxSize: 300,
    output,
  });
}

export function renderMathForSerialization(
  content: string,
  isInline: boolean,
  attrs: Record<string, unknown> = {}
): string {
  if (
    getRenderEngine() === "mathjax" &&
    attrs.renderedEngine === "mathjax" &&
    attrs.renderedContent === content &&
    typeof attrs.renderedHtml === "string" &&
    attrs.renderedHtml
  ) {
    return attrs.renderedHtml;
  }

  if (getRenderEngine() === "mathjax") {
    return escapeHtml(content);
  }

  return renderKatex(content, isInline);
}

export async function renderMathForEditor(
  content: string,
  isInline: boolean
): Promise<{
  previewHtml: string;
  serializedAttributes: SerializedMathAttributes;
}> {
  if (getRenderEngine() === "mathjax") {
    const renderedHtml = await renderMathJaxSvgHtml(content, isInline);
    return {
      previewHtml: renderedHtml,
      serializedAttributes: {
        renderedContent: content,
        renderedEngine: "mathjax",
        renderedHtml,
      },
    };
  }

  return {
    previewHtml: renderKatex(content, isInline),
    serializedAttributes: { ...EMPTY_RENDERED_MATH },
  };
}

export function emptySerializableMath(): SerializedMathAttributes {
  return { ...EMPTY_RENDERED_MATH };
}
