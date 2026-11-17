import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { api, ApiError, configurarAutenticacao } from "@/lib/api";

export type Permissao =
  | "produtos.item.criar" | "produtos.item.editar" | "produtos.item.ver" | "produtos.item.ver_resumo"
  | "produtos.precificacao.ver" | "produtos.precificacao.editar" | "produtos.precificacao.ver_valores_sensiveis";
export type PerfilLocal = "catalogo" | "resumo";

export interface Sessao {
  token: string;
  tenantId: string;
  usuario: string;
  permissoes: string[];
  expiraEm: number; // epoch ms
  origem: "local" | "manual" | "plataforma";
  perfil?: PerfilLocal;
}

type Estado = { status: "carregando" } | { status: "ok"; sessao: Sessao } | { status: "erro"; mensagem: string };

interface SessionCtx {
  estado: Estado;
  sessao: Sessao | null;
  can: (p: Permissao) => boolean;
  entrarLocal: (perfil?: PerfilLocal, tenantId?: string) => Promise<void>;
  entrarComToken: (token: string) => void;
  sair: () => void;
}

const Ctx = createContext<SessionCtx | null>(null);
export const embutidoNaPlataforma = window.parent !== window;

type MensagemParaPlataforma =
  | { tipo: "modulo:pronto" | "modulo:token-expirado" }
  | { tipo: "modulo:altura"; altura: number };

function avisarPlataforma(mensagem: MensagemParaPlataforma) {
  window.parent.postMessage(mensagem, window.location.origin);
}

/** Lê o payload do JWT apenas para exibição; a validação real é do backend. */
function decodificar(token: string): Record<string, unknown> {
  const part = token.split(".")[1];
  if (!part) throw new Error("Token JWT malformado.");
  const b64 = part.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(part.length / 4) * 4, "=");
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<Estado>({ status: "carregando" });
  const sessao = estado.status === "ok" ? estado.sessao : null;
  const sessaoRef = useRef<Sessao | null>(null);
  const renovacao = useRef<{ promessa: Promise<boolean>; resolver: (ok: boolean) => void } | null>(null);

  const aplicar = useCallback((s: Sessao) => {
    sessaoRef.current = s;
    setEstado({ status: "ok", sessao: s });
  }, []);

  const entrarLocal = useCallback(async (perfil: PerfilLocal = "catalogo", tenantId?: string) => {
    try {
      const t = await api.dev.token(perfil, tenantId);
      aplicar({
        token: t.accessToken, tenantId: t.tenantId, usuario: "usuario-local", permissoes: t.permissoes,
        expiraEm: Date.now() + t.expiresIn * 1000, origem: "local", perfil,
      });
    } catch (e) {
      const msg = e instanceof ApiError && e.status === 0
        ? e.message
        : e instanceof ApiError && (e.status === 404 || e.status === 403)
          ? "O backend não está no perfil \"local\" — inicie com -Dspring-boot.run.profiles=local ou informe um token da equipe."
          : (e as Error).message;
      sessaoRef.current = null;
      setEstado({ status: "erro", mensagem: msg });
      throw e;
    }
  }, [aplicar]);

  const aplicarToken = useCallback((token: string, origem: "manual" | "plataforma", usuario?: string) => {
    const claims = decodificar(token.trim());
    const perms = claims.perms ?? claims.permissions ?? claims.permissoes ?? [];
    aplicar({
      token: token.trim(),
      tenantId: String(claims.tenant_id ?? claims.clientId ?? "—"),
      usuario: usuario ?? String(claims.nome ?? claims.sub ?? "—"),
      permissoes: Array.isArray(perms) ? perms.map(String) : String(perms).split(" "),
      expiraEm: typeof claims.exp === "number" ? claims.exp * 1000 : Date.now() + 3600_000,
      origem,
    });
  }, [aplicar]);

  const entrarComToken = useCallback((token: string) => {
    aplicarToken(token, "manual");
  }, [aplicarToken]);

  const sair = useCallback(() => {
    sessaoRef.current = null;
    setEstado({ status: "erro", mensagem: "Sessão encerrada." });
  }, []);

  // Integra a sessão ao cliente HTTP: token atual + renovação automática após 401 (só para tokens locais).
  // Feito no render (e não em useEffect) porque os efeitos das páginas filhas rodam antes dos do provider.
  configurarAutenticacao(
    () => sessaoRef.current?.token ?? null,
    async (tokenRecusado) => {
      const s = sessaoRef.current;
      if (s?.origem === "plataforma") {
        if (s.token !== tokenRecusado) return true;
        if (!renovacao.current) {
          let resolver!: (ok: boolean) => void;
          const promessa = new Promise<boolean>((resolve) => {
            const prazo = window.setTimeout(() => resolver(false), 5000);
            resolver = (ok) => {
              window.clearTimeout(prazo);
              renovacao.current = null;
              resolve(ok);
            };
          });
          renovacao.current = { promessa, resolver };
          avisarPlataforma({ tipo: "modulo:token-expirado" });
          return promessa;
        }
        return renovacao.current.promessa;
      }
      if (s?.origem !== "local") return false;
      try { await entrarLocal(s.perfil, s.tenantId); return true; } catch { return false; }
    },
  );

  useEffect(() => {
    if (!embutidoNaPlataforma) {
      if (import.meta.env.DEV) entrarLocal().catch(() => {});
      else setEstado({ status: "erro", mensagem: "Abra Produtos e Serviços pelo menu do Centinela." });
      return;
    }

    const receber = (evento: MessageEvent) => {
      if (evento.origin !== window.location.origin || evento.source !== window.parent) return;
      const mensagem = evento.data;
      const atual = sessaoRef.current;
      const novaSessao = mensagem?.tipo === "plataforma:sessao";
      if (novaSessao || mensagem?.tipo === "plataforma:tema") {
        if (mensagem.tema === "claro" || mensagem.tema === "escuro") {
          document.documentElement.dataset.tema = mensagem.tema;
        }
      }
      const tokenRenovado = mensagem?.tipo === "plataforma:token" && atual?.origem === "plataforma";
      if (typeof mensagem?.token !== "string" || (!novaSessao && !tokenRenovado)) return;
      try {
        aplicarToken(mensagem.token, "plataforma", novaSessao ? mensagem.usuario?.nome : atual?.usuario);
        renovacao.current?.resolver(mensagem.token !== atual?.token);
      } catch {
        renovacao.current?.resolver(false);
        sessaoRef.current = null;
        setEstado({ status: "erro", mensagem: "A sessão recebida do Centinela é inválida." });
      }
    };
    window.addEventListener("message", receber);
    const tamanho = new ResizeObserver(() => {
      avisarPlataforma({ tipo: "modulo:altura", altura: document.documentElement.scrollHeight });
    });
    tamanho.observe(document.body);
    avisarPlataforma({ tipo: "modulo:pronto" });

    return () => {
      renovacao.current?.resolver(false);
      tamanho.disconnect();
      window.removeEventListener("message", receber);
    };
  }, [aplicarToken, entrarLocal]);

  const value = useMemo<SessionCtx>(() => ({
    estado, sessao, entrarLocal, entrarComToken, sair,
    can: (p) => !!sessao?.permissoes.includes(p),
  }), [estado, sessao, entrarLocal, entrarComToken, sair]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSession() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useSession fora de SessionProvider");
  return c;
}
