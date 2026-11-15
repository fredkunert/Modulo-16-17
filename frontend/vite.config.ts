import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [react(), tailwindcss(), {
      name: "token-local-apenas-no-computador",
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const caminho = (req.url ?? "").split("?")[0];
          const ip = req.socket.remoteAddress ?? "";
          // O proxy usa loopback no backend; preserve aqui a restrição do emissor local.
          const publico = req.method === "GET" && (/^\/api\/produtos\/publico\/itens\/[0-9a-f-]+$/i.test(caminho) || caminho === "/api/produtos/health");
          if (caminho.startsWith("/api") && !publico && !["127.0.0.1", "::1", "::ffff:127.0.0.1"].includes(ip)) {
            res.statusCode = 403;
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify({ success: false, data: null, message: "Acesso de desenvolvimento permitido somente neste computador.", errors: [] }));
            return;
          }
          next();
        });
      },
    }],
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    server: {
      port: 5173,
      // O backend não habilita CORS: em desenvolvimento as chamadas passam pelo proxy do Vite.
      // No perfil "local" o Spring escuta só em 127.0.0.1, por isso o alvo não usa "localhost".
      proxy: {
        "/api": { target: env.BACKEND_URL || "http://127.0.0.1:8083", changeOrigin: false },
      },
    },
  };
});
