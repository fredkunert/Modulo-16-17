import { StrictMode, useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App";
import { ToastProvider } from "@/components/ui";
import { SessionProvider } from "@/lib/session";
import { ProdutoPublico } from "@/features/itens/components/ProdutoPublico";

function Entrada() {
  const lerToken = () => new URLSearchParams(window.location.hash.slice(1)).get("produto");
  const [token, setToken] = useState(lerToken);
  useEffect(() => {
    const atualizar = () => setToken(lerToken());
    window.addEventListener("hashchange", atualizar);
    return () => window.removeEventListener("hashchange", atualizar);
  }, []);
  // A página pública nunca monta a sessão nem tenta emitir token de desenvolvimento.
  return token !== null ? <ProdutoPublico token={token} /> : <SessionProvider><App /></SessionProvider>;
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ToastProvider>
      <Entrada />
    </ToastProvider>
  </StrictMode>,
);
