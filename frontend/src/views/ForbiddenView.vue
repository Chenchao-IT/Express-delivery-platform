<template>
  <div class="page-shell">
    <section class="panel-card">
      <div class="empty-state py-16">
        <div class="text-5xl">🚫</div>
        <h1 class="mt-6 text-3xl font-bold text-slate-900">403 无权限访问</h1>
        <p class="mt-3 max-w-xl text-center text-sm leading-7 text-slate-500">
          你当前没有访问该页面的权限，请返回允许访问的业务页面继续操作。
        </p>
        <div class="mt-8 flex flex-wrap justify-center gap-3">
          <router-link to="/" class="primary-cta px-5">返回首页</router-link>
          <router-link to="/tasks" class="secondary-cta px-5">前往任务大厅</router-link>
        </div>
      </div>
    </section>
  </div>
</template>
