import katex from "katex";
import { getKatexOutput } from "./katex-output-setting";

/**
 * 编辑器侧公式渲染入口。
 *
 * 编辑器侧固定使用 KaTeX 预览
 */
export function renderKatex(content: string, isInline: boolean): string {
  const output = getKatexOutput();

  return katex.renderToString(content, {
    throwOnError: false,
    strict: false,
    displayMode: !isInline,
    maxSize: 300,
    output,
  });
}
