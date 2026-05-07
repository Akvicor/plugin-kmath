import katex from "katex";
import { getKatexOutput, getRenderEngine } from "./katex-output-setting";
import { renderMathJaxSvgHtml } from "./render-mathjax";

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
  isInline: boolean
): string {
  if (getRenderEngine() === "mathjax") {
    return escapeHtml(content);
  }

  return renderKatex(content, isInline);
}

export async function renderMathPreview(
  content: string,
  isInline: boolean
): Promise<string> {
  if (getRenderEngine() === "mathjax") {
    return renderMathJaxSvgHtml(content, isInline);
  }

  return renderKatex(content, isInline);
}
