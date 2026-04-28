package run.halo.kmath;

public final class KaTeXJSInjector {

    private KaTeXJSInjector() {
    }

    static String getParsedScript(String engine, String inlineSelector, String displaySelector,
        String katexOutput) {
        if ("mathjax".equalsIgnoreCase(engine)) {
            return getParsedMathJaxScript(inlineSelector, displaySelector);
        }
        return getParsedKatexScript(inlineSelector, displaySelector, katexOutput);
    }

    static String getParsedKatexScript(String inlineSelector, String displaySelector,
        String katexOutput) {
        String katexScript = """
            <script data-pjax>
              (function () {
                var KATEX_SRC = '/plugins/plugin-kmath/assets/static/katex.min.js';
                var INLINE_SELECTOR = %s;
                var DISPLAY_SELECTOR = %s;
                var KATEX_OUTPUT = %s;
                var KATEX_SELECTOR =
                  'script[src*="/plugins/plugin-kmath/assets/static/katex.min.js"]';
                if (KATEX_OUTPUT !== 'html' && KATEX_OUTPUT !== 'mathml'
                  && KATEX_OUTPUT !== 'htmlAndMathml') {
                  KATEX_OUTPUT = 'mathml';
                }
                function getRawTex(el) {
                  if (el.dataset && el.dataset.content) return el.dataset.content;
                  if (el.hasAttribute && el.hasAttribute('content')) return el.getAttribute('content');
                  var ann = el.querySelector && el.querySelector('annotation');
                  if (ann) return ann.textContent || '';
                  return el.textContent || '';
                }
                function selectNodes(selector) {
                  var uniq = new Set();
                  var nodes = [];
                  selector.split(',').forEach(function (fragment) {
                    var part = fragment.trim();
                    if (!part) return;
                    try {
                      document.body.querySelectorAll(part).forEach(function (el) {
                        if (uniq.has(el)) return;
                        uniq.add(el);
                        nodes.push(el);
                      });
                    } catch {
                    }
                  });
                  return nodes;
                }
                function waitForKatex(callback) {
                  if (window.katex && typeof window.katex.render === 'function') {
                    callback();
                    return;
                  }
                  setTimeout(function () { waitForKatex(callback); }, 50);
                }
                function ensureKatexReady(callback) {
                  if (window.katex && typeof window.katex.render === 'function') {
                    callback();
                    return;
                  }
                  var existing = document.querySelector(KATEX_SELECTOR);
                  if (existing) {
                    waitForKatex(callback);
                    return;
                  }
                  var script = document.createElement('script');
                  script.src = KATEX_SRC;
                  script.async = true;
                  script.onload = function () {
                    callback();
                  };
                  document.head.appendChild(script);
                }
                function renderMath(selector, displayMode) {
                  if (!window.katex) {
                    return;
                  }
                  var nodes = selectNodes(selector);
                  nodes.forEach(function (el) {
                    if (el.dataset.kmathEngine === 'katex') {
                      return;
                    }
                    try {
                      var tex = getRawTex(el);
                      katex.render(tex, el, {
                        displayMode: displayMode,
                        throwOnError: false,
                        strict: false,
                        output: KATEX_OUTPUT
                      });
                      el.dataset.kmathEngine = 'katex';
                    } catch {
                    }
                  });
                }
                ensureKatexReady(function () {
                  renderMath(INLINE_SELECTOR, false);
                  renderMath(DISPLAY_SELECTOR, true);
                });
              })();
            </script>
            """;
        return String.format(
            katexScript,
            toJsString(inlineSelector),
            toJsString(displaySelector),
            toJsString(katexOutput)
        );
    }

    static String getParsedMathJaxScript(String inlineSelector, String displaySelector) {
        String mathJaxScript = """
            <script data-pjax>
              (function () {
                var MATHJAX_BASE = '/plugins/plugin-kmath/assets/static/mathjax';
                var MATHJAX_SRC = MATHJAX_BASE + '/tex-svg.js';
                var INLINE_SELECTOR = %s;
                var DISPLAY_SELECTOR = %s;
                var MATHJAX_SELECTOR =
                  'script[src*="/plugins/plugin-kmath/assets/static/mathjax/tex-svg.js"]';
                function getRawTex(el) {
                  if (el.dataset && el.dataset.content) return el.dataset.content;
                  if (el.hasAttribute && el.hasAttribute('content')) return el.getAttribute('content');
                  var ann = el.querySelector && el.querySelector('annotation');
                  if (ann) return ann.textContent || '';
                  return el.textContent || '';
                }
                function selectNodes(selector) {
                  var uniq = new Set();
                  var nodes = [];
                  selector.split(',').forEach(function (fragment) {
                    var part = fragment.trim();
                    if (!part) return;
                    try {
                      document.body.querySelectorAll(part).forEach(function (el) {
                        if (uniq.has(el)) return;
                        uniq.add(el);
                        nodes.push(el);
                      });
                    } catch {
                    }
                  });
                  return nodes;
                }
                function once(callback) {
                  var called = false;
                  return function () {
                    if (called) return;
                    called = true;
                    callback();
                  };
                }
                function waitForMathJax(callback) {
                  if (window.MathJax && typeof window.MathJax.tex2svg === 'function') {
                    callback();
                    return;
                  }
                  setTimeout(function () { waitForMathJax(callback); }, 50);
                }
                function ensureMathJaxReady(callback) {
                  var finish = once(callback);
                  if (window.MathJax && typeof window.MathJax.tex2svg === 'function') {
                    finish();
                    return;
                  }
                  if (!window.MathJax) {
                    window.MathJax = {
                      loader: {
                        paths: { mathjax: MATHJAX_BASE }
                      },
                      tex: {
                        inlineMath: [ ['$', '$'], ['\\\\(', '\\\\)'] ],
                        displayMath: [ ['$$', '$$'], ['\\\\[', '\\\\]'] ]
                      },
                      svg: { fontCache: 'local' },
                      startup: {
                        typeset: false,
                        ready: function () {
                          try {
                            window.MathJax.startup.defaultReady();
                            finish();
                          } catch {
                          }
                        }
                      }
                    };
                  }
                  var existing = document.querySelector(MATHJAX_SELECTOR);
                  if (existing) {
                    waitForMathJax(finish);
                    return;
                  }
                  var script = document.createElement('script');
                  script.src = MATHJAX_SRC;
                  script.async = true;
                  script.onload = function () {
                    waitForMathJax(finish);
                  };
                  document.head.appendChild(script);
                }
                async function ready() {
                  function renderInto(el, displayMode) {
                    if (el.dataset.kmathEngine === 'mathjax') {
                      return Promise.resolve();
                    }
                    if (!el.isConnected) {
                      return Promise.resolve();
                    }
                    var tex = getRawTex(el);
                    var convertPromise =
                      typeof MathJax.tex2svgPromise === 'function'
                        ? MathJax.tex2svgPromise(tex, { display: displayMode })
                        : Promise.resolve(MathJax.tex2svg(tex, { display: displayMode }));
                    return convertPromise.then(function (node) {
                      if (!el.isConnected) {
                        return;
                      }
                      el.replaceChildren(node);
                      el.dataset.kmathEngine = 'mathjax';
                    }).catch(function () {
                    });
                  }
                  var tasks = [];
                  var inlineNodes = selectNodes(INLINE_SELECTOR);
                  inlineNodes.forEach(function (el) {
                    tasks.push(renderInto(el, false));
                  });
                  var displayNodes = selectNodes(DISPLAY_SELECTOR);
                  displayNodes.forEach(function (el) {
                    tasks.push(renderInto(el, true));
                  });
                  await Promise.all(tasks);
                }
                ensureMathJaxReady(function () {
                  if (document.readyState === 'complete' || document.readyState === 'interactive') {
                    ready().catch(function () {});
                  } else {
                    window.addEventListener('DOMContentLoaded', function () {
                      ready().catch(function () {});
                    }, { once: true });
                  }
                });
              })();
            </script>
            """;
        String inlineSelectorLiteral = toJsString(inlineSelector);
        String displaySelectorLiteral = toJsString(displaySelector);
        return String.format(mathJaxScript, inlineSelectorLiteral, displaySelectorLiteral);
    }

    static String toJsString(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '/' -> escaped.append("\\/");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
