import { rsbuildConfig } from "@halo-dev/ui-plugin-bundler-kit";
import Icons from "unplugin-icons/rspack";
import rspack from "@rspack/core";

const OUT_DIR_PROD = "../src/main/resources/console";
const OUT_DIR_DEV = "../build/resources/main/console";

export default rsbuildConfig({
  rsbuild: ({ envMode }) => {
    const isProduction = envMode === "production";
    const outDir = isProduction ? OUT_DIR_PROD : OUT_DIR_DEV;

    return {
      output: {
        distPath: {
          root: outDir,
        },
      },
      tools: {
        rspack: {
          plugins: [
            Icons({ compiler: "vue3" }),
            new rspack.CopyRspackPlugin({
              patterns: [
                // KaTeX：包含 css、js、字体
                {
                  from: "./node_modules/katex/dist",
                  to: "../static",
                },
                // MathJax：整包本地化，避免任何外部请求
                {
                  from: "./node_modules/mathjax",
                  to: "../static/mathjax",
                  globOptions: {
                    ignore: [
                      "**/package.json",
                      "**/README.md",
                      "**/CONTRIBUTING.md",
                      "**/LICENSE",
                    ],
                  },
                },
              ],
            }),
          ],
        },
      },
    };
  },
});
