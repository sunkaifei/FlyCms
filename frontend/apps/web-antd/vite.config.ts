import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          // FlyCms 后端 REST（/api 前缀经 rewrite 剥除后由 target 的 /api 补回，净映射 /api/** -> 后端 /api/**）
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            target: 'http://localhost:80/api',
            ws: true,
          },
          // 图形验证码：后端无 /api 前缀，单独直连
          '/captcha': {
            changeOrigin: true,
            target: 'http://localhost:80',
          },
        },
      },
    },
  };
});
