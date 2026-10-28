import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    server: {
      port: 5173,
      // O backend não habilita CORS: em desenvolvimento as chamadas passam pelo proxy do Vite.
      // No perfil "local" o Spring escuta só em 127.0.0.1, por isso o alvo não usa "localhost".
      // A precificação é outro serviço (8084) com o mesmo prefixo /api/produtos — mesma divisão do gateway/nginx.conf.
      // A ordem importa: a regra mais específica precisa vir antes de "/api".
      proxy: {
        "^/api/produtos/itens/[^/]+/precificacao": {
          target: env.PRECIFICACAO_URL || "http://127.0.0.1:8084", changeOrigin: false,
        },
        "/api": { target: env.BACKEND_URL || "http://127.0.0.1:8083", changeOrigin: false },
      },
    },
  };
});
