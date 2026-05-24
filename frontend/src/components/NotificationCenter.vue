<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits(['update:visible'])

const router = useRouter()
const notificationStore = useNotificationStore()

const handleClose = () => {
  emit('update:visible', false)
}

const handleNotificationClick = async (n: any) => {
  if (!n.readAt) {
    await notificationStore.readOne(n.id)
  }
  if (n.projectId) {
    router.push(`/admin/projects/${n.projectId}`)
    handleClose()
  }
}

const formatTime = (time: string) => {
  return formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN })
}

watch(() => props.visible, (val) => {
  if (val) {
    notificationStore.fetchNotifications()
  }
})

onMounted(() => {
  notificationStore.fetchUnreadCount()
})
</script>

<template>
  <el-drawer
    title="通知中心"
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    direction="rtl"
    size="350px"
  >
    <template #header>
      <div class="drawer-header">
        <span>通知中心</span>
        <el-button v-if="notificationStore.unreadCount > 0" type="primary" link @click="notificationStore.readAll">
          全部已读
        </el-button>
      </div>
    </template>

    <div v-loading="notificationStore.loading" class="notification-list">
      <el-empty v-if="notificationStore.notifications.length === 0" description="暂无通知" />
      <div
        v-for="n in notificationStore.notifications"
        :key="n.id"
        class="notification-item"
        :class="{ unread: !n.readAt }"
        @click="handleNotificationClick(n)"
      >
        <div class="item-header">
          <span class="type-tag" :class="n.type.toLowerCase()">{{ n.type === 'NEW_ANNOTATION' ? '新反馈' : '已确认' }}</span>
          <span class="time">{{ formatTime(n.createdAt) }}</span>
        </div>
        <div class="title">{{ n.title }}</div>
        <div class="content">{{ n.content }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}
.notification-list {
  padding: 0;
}
.notification-item {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s;
  position: relative;
}
.notification-item:hover {
  background: #f9f9f9;
}
.notification-item.unread {
  background: #fdf6ec;
}
.notification-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #e6a23c;
}
.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.type-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #eee;
}
.type-tag.new_annotation {
  background: #e6f7ff;
  color: #1890ff;
}
.type-tag.confirmation {
  background: #f6ffed;
  color: #52c41a;
}
.time {
  font-size: 12px;
  color: #999;
}
.title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}
.content {
  font-size: 13px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
