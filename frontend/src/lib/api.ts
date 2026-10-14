import type {
  BuscaResultado, Item, ItemAtualizacao, ItemCriacao, Pagina, Parceiro,
} from "@/types/produtos";

const BASE = `${import.meta.env.VITE_API_URL ?? ""}/api/produtos`;

export interface ErroCampo { campo: string | null; codigo: string; detalhe: string }
interface Envelope<T> { success: boolean; data: T; message: string | null; errors: ErroCampo[] }

export class ApiError extends Error {
  constructor(public status: number, message: string, public errors: ErroCampo[] = []) {
    super(message);
  }
  /** Mapa campo → mensagem, para exibir junto ao FormField correspondente. */
  get porCampo(): Record<string, string> {
    return Object.fromEntries(this.errors.filter((e) => e.campo).map((e) => [e.campo!, e.detalhe]));
  }
}

// A sessão registra aqui como obter o token atual e como renová-lo após um 401.
let tokenAtual: () => string | null = () => null;
let renovarToken: (() => Promise<boolean>) | null = null;
export function configurarAutenticacao(getter: () => string | null, renovar: (() => Promise<boolean>) | null) {
  tokenAtual = getter;
  renovarToken = renovar;
}

async function request<T>(method: string, path: string, body?: unknown, tentouRenovar = false): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  const token = tokenAtual();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json; charset=utf-8";

  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, "Não foi possível conectar ao backend. Verifique se o serviço está em execução na porta 8083.");
  }

  if (res.status === 401 && !tentouRenovar && renovarToken && (await renovarToken())) {
    return request<T>(method, path, body, true);
  }

  const json = (await res.json().catch(() => null)) as Envelope<T> | null;
  if (!res.ok || !json?.success) {
    const msg = json?.message ?? json?.errors?.[0]?.detalhe ?? mensagemPadrao(res.status);
    throw new ApiError(res.status, msg, json?.errors ?? []);
  }
  return json.data;
}

function mensagemPadrao(status: number) {
  if (status === 401) return "Sessão expirada ou token inválido.";
  if (status === 403) return "Você não tem permissão para esta ação.";
  if (status === 404) return "Registro não encontrado.";
  if (status === 409) return "Conflito com os dados cadastrados.";
  if (status >= 500) return "Erro interno no servidor.";
  return `Erro inesperado (HTTP ${status}).`;
}

function qs(params: Record<string, string | number | boolean | null | undefined>) {
  const s = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null && v !== "") s.set(k, String(v));
  const str = s.toString();
  return str ? `?${str}` : "";
}

export interface ItensFiltro {
  q?: string;
  categoria?: string;
  fabricanteId?: string;
  distribuidorId?: string;
  ativo?: boolean | null;
  pagina?: number;
  tamanho?: number;
  ordenar?: "nome,asc" | "nome,desc";
}

export interface PaginaParams { pagina?: number; tamanho?: number; ordenar?: "nome,asc" | "nome,desc" }

export type TipoParceiro = "fabricantes" | "distribuidores";

export const api = {
  health: () => request<unknown>("GET", "/health"),

  itens: {
    listar: (f: ItensFiltro) => request<Pagina<Item>>("GET", `/itens${qs({ ...f })}`),
    buscar: (id: string) => request<Item>("GET", `/itens/${id}`),
    criar: (d: ItemCriacao) => request<Item>("POST", "/itens", d),
    atualizar: (id: string, d: ItemAtualizacao) => request<Item>("PATCH", `/itens/${id}`, d),
  },

  busca: (q: string) => request<BuscaResultado[]>("GET", `/busca${qs({ q })}`),

  parceiros: (tipo: TipoParceiro) => ({
    listar: (p: PaginaParams) => request<Pagina<Parceiro>>("GET", `/${tipo}${qs({ ...p })}`),
    criar: (d: { nome: string; ativo: boolean }) => request<Parceiro>("POST", `/${tipo}`, d),
    atualizar: (id: string, d: Partial<{ nome: string; ativo: boolean }>) => request<Parceiro>("PATCH", `/${tipo}/${id}`, d),
  }),

  /** Lista todos os parceiros (para selects), percorrendo as páginas de 100. */
  async todosParceiros(tipo: TipoParceiro): Promise<Parceiro[]> {
    const out: Parceiro[] = [];
    for (let pagina = 0; ; pagina++) {
      const p = await api.parceiros(tipo).listar({ pagina, tamanho: 100 });
      out.push(...p.itens);
      if (out.length >= p.total || p.itens.length === 0) return out;
    }
  },

  dev: {
    token: (perfil: "catalogo" | "resumo", tenantId?: string) =>
      request<{ accessToken: string; tokenType: string; expiresIn: number; tenantId: string; permissoes: string[] }>(
        "POST", "/dev/token", { perfil, tenantId: tenantId || null }, true),
  },
};
