const MATHJAX_BASE = "/plugins/plugin-kmath/assets/static/mathjax";
const MATHJAX_SRC = `${MATHJAX_BASE}/tex-svg.js`;
const MATHJAX_SELECTOR = `script[src*="${MATHJAX_SRC}"]`;
const MATHJAX_READY_TIMEOUT = 15000;

type MathJaxRenderOptions = {
  display: boolean;
};

type MathJaxStartup = {
  adaptor?: {
    outerHTML?: (node: unknown) => string;
  };
  typeset?: boolean;
};

type MathJaxInstance = {
  loader?: {
    load?: string[];
    paths?: Record<string, string>;
  };
  startup?: MathJaxStartup;
  svg?: {
    fontCache?: string;
  };
  tex?: {
    packages?: {
      "[+]"?: string[];
    };
    displayMath?: string[][];
    inlineMath?: string[][];
  };
  tex2svg?: (content: string, options: MathJaxRenderOptions) => Element;
};

declare global {
  interface Window {
    MathJax?: MathJaxInstance;
  }
}

let mathJaxLoadingPromise: Promise<void> | null = null;

function getMathJax(): MathJaxInstance | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }
  return window.MathJax;
}

function isMathJaxReady(): boolean {
  return typeof getMathJax()?.tex2svg === "function";
}

function configureMathJax() {
  const existing = window.MathJax || {};
  const currentLoaderLoad = existing.loader?.load || [];
  const mergedLoaderLoad = currentLoaderLoad.includes("[tex]/noerrors")
    ? currentLoaderLoad
    : [...currentLoaderLoad, "[tex]/noerrors"];
  const currentTexPackages = existing.tex?.packages?.["[+]"] || [];
  const mergedTexPackages = currentTexPackages.includes("noerrors")
    ? currentTexPackages
    : [...currentTexPackages, "noerrors"];

  window.MathJax = {
    ...existing,
    loader: {
      ...existing.loader,
      load: mergedLoaderLoad,
      paths: {
        ...existing.loader?.paths,
        mathjax: MATHJAX_BASE,
      },
    },
    tex: {
      packages: {
        ...existing.tex?.packages,
        "[+]": mergedTexPackages,
      },
      inlineMath: [
        ["$", "$"],
        ["\\(", "\\)"],
      ],
      displayMath: [
        ["$$", "$$"],
        ["\\[", "\\]"],
      ],
      ...existing.tex,
    },
    svg: {
      fontCache: "local",
      ...existing.svg,
    },
    startup: {
      typeset: false,
      ...existing.startup,
    },
  };
}

function waitForMathJaxReady(): Promise<void> {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now();

    const check = () => {
      if (isMathJaxReady()) {
        resolve();
        return;
      }

      if (Date.now() - startedAt > MATHJAX_READY_TIMEOUT) {
        reject(new Error("MathJax was not ready before timeout"));
        return;
      }

      window.setTimeout(check, 50);
    };

    check();
  });
}

function appendMathJaxScript(): Promise<void> {
  const existing = document.querySelector<HTMLScriptElement>(MATHJAX_SELECTOR);
  if (existing) {
    return waitForMathJaxReady();
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = MATHJAX_SRC;
    script.async = true;
    script.onload = () => {
      waitForMathJaxReady().then(resolve).catch(reject);
    };
    script.onerror = () => {
      reject(new Error(`Failed to load MathJax from ${MATHJAX_SRC}`));
    };
    document.head.appendChild(script);
  });
}

export function ensureMathJaxReady(): Promise<void> {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return Promise.resolve();
  }

  if (isMathJaxReady()) {
    return Promise.resolve();
  }

  if (!mathJaxLoadingPromise) {
    mathJaxLoadingPromise = (async () => {
      configureMathJax();
      await appendMathJaxScript();
    })().catch((error) => {
      mathJaxLoadingPromise = null;
      throw error;
    });
  }

  return mathJaxLoadingPromise;
}

export function renderMathJax(content: string, isInline: boolean): string {
  const mathJax = getMathJax();
  if (!mathJax?.tex2svg) {
    throw new Error("MathJax is not ready");
  }

  const node = mathJax.tex2svg(content, {
    display: !isInline,
  });
  const svg = node.querySelector<SVGSVGElement>("svg");
  if (svg) {
    if (isInline) {
      svg.style.display = "inline-block";
    } else {
      svg.style.display = "block";
      svg.style.margin = "0 auto";
    }
  }

  const outerHTML = mathJax.startup?.adaptor?.outerHTML;
  if (outerHTML && svg) {
    return outerHTML(svg);
  }

  if (svg) {
    return svg.outerHTML;
  }

  return node.outerHTML;
}
