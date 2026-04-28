<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">客户审稿页</h1>
        <p class="page-subtitle">访问令牌：{{ route.params.token }}</p>
      </div>
    </header>

    <div class="panel">
      <div class="panel-body">设计稿预览、在线标注和确认定稿功能将在审稿主流程中实现。</div>
    </div>
  </section>
</template>
