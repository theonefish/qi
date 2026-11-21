<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="brand">
        <div class="brand-mark">蜀漆云馆</div>
        <div class="brand-sub">内容管理后台</div>
      </div>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="管理员账号" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
  import { reactive, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { authApi } from '../api'

  const router = useRouter()
  const form = reactive({ username: '', password: '' })
  const loading = ref(false)

  async function onLogin() {
    if (!form.username || !form.password) return
    loading.value = true
    try {
      const res = await authApi.login({ username: form.username, password: form.password })
      localStorage.setItem('shuqi_admin_token', res.token)
      localStorage.setItem('shuqi_admin_name', res.displayName)
      router.push('/overview')
    } catch (e) {
      /* 拦截器已提示 */
    } finally {
      loading.value = false
    }
  }
</script>

<style scoped>
  .login-wrap {
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: radial-gradient(ellipse at 75% 25%, #a75c42 0%, #7e352d 45%, #351b1b 100%);
  }
  .login-card {
    width: 380px;
    border-radius: 14px;
  }
  .brand {
    text-align: center;
    margin-bottom: 26px;
  }
  .brand-mark {
    font-size: 26px;
    letter-spacing: 8px;
    color: #762d25;
    font-weight: 600;
  }
  .brand-sub {
    margin-top: 6px;
    font-size: 13px;
    color: #81776d;
    letter-spacing: 2px;
  }
  .login-btn {
    width: 100%;
    letter-spacing: 4px;
  }
</style>
