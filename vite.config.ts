import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { defineConfig } from 'vite';

// Configuração do Vite para o frontend do Sistema de Detecção de Perdas
// Não Técnicas de Energia (GridAudit EDM). O proxy de "/api" evita problemas
// de CORS durante o desenvolvimento, encaminhando os pedidos para o backend
// Spring Boot que corre em localhost:8080.
export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
