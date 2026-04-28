import { definePlugin } from "@halo-dev/console-shared";
import "katex/dist/katex.css";
import { initKatexOutputSetting } from "./editor/katex/katex-output-setting";

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {
    "default:editor:extension:create": async () => {
      void initKatexOutputSetting();
      const { ExtensionKatexBlock, ExtensionKatexInline } = await import(
        "./editor/katex"
      );
      return [ExtensionKatexBlock, ExtensionKatexInline];
    },
  },
});
