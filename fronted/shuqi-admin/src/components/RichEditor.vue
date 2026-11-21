<template>
  <div class="rich-editor-shell">
    <Toolbar class="re-toolbar" :editor="editorRef" :default-config="toolbarConfig" />
    <Editor class="re-content" :style="{ height: height }" v-model="innerValue" :default-config="editorConfig"
      @on-created="onCreated" />
  </div>
</template>

<script setup>
  import { onBeforeUnmount, ref, watch } from 'vue'
  import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
  import '@wangeditor/editor/dist/css/style.css'
  import { ElMessage } from 'element-plus'
  import { uploadConfig } from '../api'

  const props = defineProps({
    modelValue: { type: String, default: '' },
    height: { type: String, default: '200px' }
  })
  const emit = defineEmits(['update:modelValue'])

  const editorRef = ref()
  // 视频上传本期不做（音视频 P1）
  const toolbarConfig = { excludeKeys: ['group-video', 'insertVideo', 'uploadVideo'] }
  const editorConfig = {
    placeholder: '开始编写：支持图文混排、加粗、标题等排版',
    MENU_CONF: {
      uploadImage: {
        server: uploadConfig.action,
        fieldName: 'file',
        headers: uploadConfig.headers(),
        customInsert(res, insertFn) {
          if (res && res.code === 0 && res.data && res.data.url) {
            insertFn('http://localhost:8080' + res.data.url, '', '')
          } else {
            ElMessage.error((res && res.msg) || '图片上传失败')
          }
        }
      }
    }
  }

  const innerValue = ref(props.modelValue)
  watch(() => props.modelValue, (val) => {
    if (val !== innerValue.value) innerValue.value = val
  })
  watch(innerValue, (val) => emit('update:modelValue', val))

  function onCreated(editor) {
    editorRef.value = editor
  }

  onBeforeUnmount(() => {
    const editor = editorRef.value
    if (editor) editor.destroy()
  })
</script>

<style scoped>
  .rich-editor-shell {
    width: 100%;
    border: 1px solid #e6dccd;
    border-radius: 8px;
    overflow: hidden;
    background: #fff;
    z-index: 1;
  }
  .re-toolbar { border-bottom: 1px solid #e6dccd; }
  :deep(.w-e-drop-panel),
  :deep(.w-e-modal),
  :deep(.w-e-select-list) { z-index: 4000 !important; }
</style>
