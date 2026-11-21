<template>
  <div class="preview-sticky">
    <div class="pv-hint">小程序实时预览 · iPhone 12 Pro（390 × 844）布局示意，实际以真机为准</div>
    <div class="phone">
      <div class="phone-notch"></div>
      <div class="phone-screen">
        <div class="pv-navbar">蜀漆云馆</div>
        <div class="pv-body">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  /* iPhone 12 Pro：屏幕逻辑分辨率 390 × 844（460ppi，6.1"），外框含中框边距 */
  .preview-sticky { position: sticky; top: 16px; }
  .pv-hint { text-align: center; font-size: 12px; color: #81776d; margin-bottom: 10px; }
  .phone {
    width: 414px;            /* 390 屏幕 + 左右各 12px 中框 */
    margin: 0 auto;
    border: 12px solid #2b2320;
    border-radius: 54px;     /* iPhone 12 Pro 外壳圆角 */
    background: #f6f1e8;
    overflow: hidden;
    position: relative;
    box-shadow: 0 20px 50px rgba(50, 35, 25, .25);
  }
  .phone-notch {
    position: absolute; top: 0; left: 50%; transform: translateX(-50%);
    width: 160px; height: 28px; background: #2b2320;
    border-radius: 0 0 16px 16px; z-index: 5;
  }
  .phone-screen {
    width: 390px; height: 844px;
    margin: 0 auto;
    overflow-y: auto; overflow-x: hidden;
    background: #f6f1e8;
  }
  .pv-navbar {
    position: sticky; top: 0; z-index: 4; text-align: center;
    padding: 30px 0 12px;   /* 顶部让出刘海区域 */
    background: #f6f1e8; font-size: 13px; color: #28231f;
    border-bottom: 1px solid #e6dccd;
  }
  .pv-body { padding-bottom: 30px; }
</style>
