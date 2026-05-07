<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { nodeViewProps, NodeViewWrapper } from "@halo-dev/richtext-editor";
import { VDropdown } from "@halo-dev/components";
import { renderMathPreview } from "./render-katex";

const props = defineProps(nodeViewProps);

const content = computed(() => props.node.attrs.content || "");

const renderedMath = ref("");

watch(content, async (value, _, onCleanup) => {
  let stale = false;
  onCleanup(() => {
    stale = true;
  });

  if (!value) {
    renderedMath.value = "";
    return;
  }

  try {
    const rendered = await renderMathPreview(value, true);
    if (!stale) {
      renderedMath.value = rendered;
    }
  } catch (error) {
    console.error("Math preview error:", error);
    if (!stale) {
      renderedMath.value = "";
    }
  }
}, { immediate: true });

const showEditor = ref(false);
onMounted(() => {
  showEditor.value = props.node.attrs.editMode;
});

function onEditorChange(value: string) {
  props.updateAttributes({ content: value });
}
</script>
<template>
  <node-view-wrapper
    class="katex-inline-container"
    as="span"
    contenteditable="false"
    :class="{ 'katex-node-view-selected': props.selected }"
  >
    <VDropdown
      v-model:shown="showEditor"
      :classes="['no-padding']"
      :distance="12"
      placement="bottom"
    >
      <div class="katex-node-view-content-wrapper" @click.stop="showEditor = true">
        <span
          v-if="node.attrs.content"
          contenteditable="false"
          v-html="renderedMath"
        ></span>
        <span v-else> 添加LaTeX公式 </span>
      </div>
      <template #popper>
        <div class="katex-inline-view-code">
          <VCodemirror
            :model-value="content"
            height="180px"
            @change="onEditorChange"
          />
        </div>
      </template>
    </VDropdown>
  </node-view-wrapper>
</template>
<style>
.katex-inline-container {
  cursor: pointer;
  padding: 0 0.25rem;
  transition: background 0.2s;
  display: inline-block;
}

.katex-node-view-selected .katex-node-view-content-wrapper {
  background: #f2f2f2;
}

.katex-node-view-content-wrapper {
  background: #f6f5f5;
  display: inline-block;
  padding: 3px;
  border-radius: 3px;
  &:hover {
    background: #f2f2f2;
  }
}

.katex-inline-view-code {
  width: 300px;
}

.no-padding {
  padding: 0 !important;
}
</style>
