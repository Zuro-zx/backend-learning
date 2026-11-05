import { reactive } from 'vue'

/** 轻量提示：403 这类"认出了你但不够格"的反馈只弹提示，不打断操作 */
export const toastState = reactive<{ message: string; visible: boolean }>({
  message: '',
  visible: false
})

let timer: number | undefined

export function toast(message: string, ms = 2400): void {
  toastState.message = message
  toastState.visible = true
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    toastState.visible = false
  }, ms)
}
