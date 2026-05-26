<!--
  根组件 - 提供路由视图容器
  多人联机贪吃蛇游戏
-->
<template>
  <router-view />
</template>

<script setup>
// App.vue 作为根组件，仅提供路由出口
</script>

<style>
/* 全局重置，防止默认边距影响布局 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
