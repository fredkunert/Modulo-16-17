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
  origem: "local" | "manual";
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
const STORAGE_KEY = "centinela.produtos.sessao";

function ler(): Sessao | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Sessao;
    return s.expiraEm > Date.now() + 30_000 ? s : null;
  } catch { return null; }
}
function gravar(s: Sessao | null) {
  try { s ? localStorage.setItem(STORAGE_KEY, JSON.stringify(s)) : localStorage.removeItem(STORAGE_KEY); } catch { /* armazenamento indisponível */ }
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
  const [estado, setEstado] = useState<Estado>(() => {
    const s = ler();
    return s ? { status: "ok", sessao: s } : { status: "carregando" };
  });
  const sessao = estado.status === "ok" ? estado.sessao : null;
  const sessaoRef = useRef(sessao);
  sessaoRef.current = sessao;

  const aplicar = useCallback((s: Sessao) => {
    gravar(s);
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
      gravar(null);
      sessaoRef.current = null;
      setEstado({ status: "erro", mensagem: msg });
      throw e;
    }
  }, [aplicar]);

  const entrarComToken = useCallback((token: string) => {
    const claims = decodificar(token.trim());
    const perms = claims.permissions ?? claims.permissoes ?? [];
    aplicar({
      token: token.trim(),
      tenantId: String(claims.tenant_id ?? claims.clientId ?? "—"),
      usuario: String(claims.sub ?? "—"),
      permissoes: Array.isArray(perms) ? perms.map(String) : String(perms).split(" "),
      expiraEm: typeof claims.exp === "number" ? claims.exp * 1000 : Date.now() + 3600_000,
      origem: "manual",
    });
  }, [aplicar]);

  const sair = useCallback(() => {
    gravar(null);
    sessaoRef.current = null;
    setEstado({ status: "erro", mensagem: "Sessão encerrada." });
  }, []);

  // Integra a sessão ao cliente HTTP: token atual + renovação automática após 401 (só para tokens locais).
  // Feito no render (e não em useEffect) porque os efeitos das páginas filhas rodam antes dos do provider.
  configurarAutenticacao(
    () => sessaoRef.current?.token ?? null,
    async () => {
      const s = sessaoRef.current;
      if (s?.origem !== "local") return false;
      try { await entrarLocal(s.perfil, s.tenantId); return true; } catch { return false; }
    },
  );

  // Primeiro acesso: tenta obter o token de desenvolvimento automaticamente.
  useEffect(() => {
    if (estado.status === "carregando") entrarLocal().catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
