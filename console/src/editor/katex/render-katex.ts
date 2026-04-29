import katex from "katex";
import { getKatexOutput, getRenderEngine } from "./katex-output-setting";
import { renderMathJax } from "./render-mathjax";

/**
 * 编辑器侧公式渲染入口。
 *
 * 根据插件配置选择 KaTeX 或 MathJax。
 */
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

export function renderMath(content: string, isInline: boolean): string {
  if (getRenderEngine() === "mathjax") {
    return renderMathJax(content, isInline);
  }

  return renderKatex(content, isInline);
}
