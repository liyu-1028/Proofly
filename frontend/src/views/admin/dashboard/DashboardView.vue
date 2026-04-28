<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">工作台</h1>
        <p class="page-subtitle">集中查看审稿项目状态、客户反馈和确认进度。</p>
      </div>
    </header>

    <div class="metric-grid">
      <div class="metric">
        <span>待客户反馈</span>
        <strong>8</strong>
      </div>
      <div class="metric">
        <span>需修改</span>
        <strong>5</strong>
      </div>
      <div class="metric">
        <span>待确认</span>
        <strong>3</strong>
      </div>
      <div class="metric">
        <span>已确认</span>
        <strong>16</strong>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body">
        <ul class="placeholder-list">
          <li>
            <span>门头招牌设计稿 V2 等待客户确认</span>
            <span class="status-pill">待确认</span>
          </li>
          <li>
            <span>宣传单页新增 2 条客户标注</span>
            <span class="status-pill">需修改</span>
          </li>
          <li>
            <span>包装盒设计稿 V1 已确认定稿</span>
            <span class="status-pill">已确认</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>
