<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Lock, RefreshRight } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, updateUserStatus, resetUserPassword, type UserResponse } from '@/api/admin'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const loading = ref(false)
const staffList = ref<UserResponse[]>([])

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const form = ref({
  userId: '',
  username: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  roleCodes: [] as string[]
})

const passwordDialogVisible = ref(false)
const passwordForm = ref({
  userId: '',
  nickname: '',
  password: ''
})

const fetchStaff = async () => {
  loading.value = true
  try {
    const data = await getUsers()
    staffList.value = data
  } catch (error: any) {
    ElMessage.error(error.message || '获取员工列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  dialogType.value = 'create'
  form.value = {
    userId: '',
    username: '',
    nickname: '',
    phone: '',
    email: '',
    password: '',
    roleCodes: ['designer']
  }
  dialogVisible.value = true
}

const handleEdit = (row: UserResponse) => {
  dialogType.value = 'edit'
  form.value = {
    userId: row.userId,
    username: row.username,
    nickname: row.nickname,
    phone: row.phone || '',
    email: row.email || '',
    password: '',
    roleCodes: [...row.roles]
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  formLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createUser({
        username: form.value.username,
        nickname: form.value.nickname,
        phone: form.value.phone || undefined,
        email: form.value.email || undefined,
        password: form.value.password,
        roleCodes: form.value.roleCodes
      })
      ElMessage.success('创建成功')
    } else {
      await updateUser(form.value.userId, {
        nickname: form.value.nickname,
        phone: form.value.phone || undefined,
        email: form.value.email || undefined,
        roleCodes: form.value.roleCodes
      })
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    fetchStaff()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

const handleToggleStatus = async (row: UserResponse) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const actionText = newStatus === 'active' ? '启用' : '停用'
  
  if (row.userId === session.user?.userId) {
    ElMessage.warning('不能停用当前登录账号')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要${actionText}员工「${row.nickname}」吗？`, '提示', {
      type: 'warning'
    })
    await updateUserStatus(row.userId, newStatus)
    ElMessage.success(`${actionText}成功`)
    fetchStaff()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleResetPassword = (row: UserResponse) => {
  passwordForm.value = {
    userId: row.userId,
    nickname: row.nickname,
    password: ''
  }
  passwordDialogVisible.value = true
}

const submitPasswordReset = async () => {
  if (!passwordForm.value.password || passwordForm.value.password.length < 6) {
    ElMessage.warning('密码长度至少为 6 位')
    return
  }
  
  formLoading.value = true
  try {
    await resetUserPassword(passwordForm.value.userId, {
      password: passwordForm.value.password
    })
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

onMounted(() => {
  fetchStaff()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">员工管理</h1>
        <p class="page-subtitle">管理门店老板、设计师和管理员账号。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleCreate">创建员工</el-button>
    </header>

    <div class="panel">
      <el-table :data="staffList" v-loading="loading" style="width: 100%">
        <el-table-column prop="nickname" label="姓名" min-width="120" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 4px">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              link 
              :type="row.status === 'active' ? 'danger' : 'success'" 
              :icon="row.status === 'active' ? Lock : RefreshRight"
              :disabled="row.userId === session.user?.userId"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="warning" :icon="Lock" @click="handleResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '创建员工' : '编辑员工'"
      width="500px"
    >
      <el-form :model="form" label-width="80px" label-position="top">
        <el-form-item label="用户名" required v-if="dialogType === 'create'">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.nickname" placeholder="请输入员工姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="初始密码" required v-if="dialogType === 'create'">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="form.roleCodes">
            <el-checkbox label="admin">管理员</el-checkbox>
            <el-checkbox label="owner">老板</el-checkbox>
            <el-checkbox label="designer">设计师</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="重置密码"
      width="400px"
    >
      <p style="margin-bottom: 16px">确定要重置员工「{{ passwordForm.nickname }}」的密码吗？</p>
      <el-form :model="passwordForm" label-position="top">
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitPasswordReset">确定重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}
.page-subtitle {
  color: #666;
  font-size: 14px;
}
.panel {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}
</style>
