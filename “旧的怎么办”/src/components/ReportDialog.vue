<template>
  <div v-if="visible" class="profile-modal-backdrop" @click.self="handleCancel">
    <section class="profile-modal-card report-dialog-card">
      <div class="profile-modal-header">
        <div class="floor-header">
          <div>
            <h3>举报商品</h3>
            <p class="muted report-dialog-copy">
              {{ itemTitle ? `正在举报：${itemTitle}` : '请填写举报原因，提交后商品会进入待审状态。' }}
            </p>
          </div>
          <button class="outline-btn" type="button" :disabled="submitting" @click="handleCancel">关闭</button>
        </div>
      </div>

      <div class="profile-modal-body">
        <label>
          <span>举报原因</span>
          <textarea
            v-model="reason"
            class="report-dialog-textarea"
            rows="4"
            maxlength="255"
            placeholder="例如：虚假描述、违规商品、恶意引流等"
          />
        </label>
        <p class="helper-text">请尽量描述清楚，管理员会根据举报原因进行审核。</p>
      </div>

      <div class="modal-actions report-dialog-actions">
        <button class="outline-btn" type="button" :disabled="submitting" @click="handleCancel">取消</button>
        <button class="search-btn" type="button" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交举报' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  itemTitle: {
    type: String,
    default: ''
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['cancel', 'submit'])

const reason = ref('')

watch(
  () => props.visible,
  value => {
    if (value) {
      reason.value = ''
    }
  }
)

function handleCancel() {
  if (props.submitting) {
    return
  }
  emit('cancel')
}

function handleSubmit() {
  emit('submit', reason.value)
}
</script>
