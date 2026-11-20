import { useCallback, useEffect, useState, type ReactElement, type ReactNode } from "react";
import { ChevronLeft, ChevronRight, Factory, Package, Search, Truck } from "lucide-react";
import { api } from "@/lib/api";
import { embutidoNaPlataforma, useSession } from "@/lib/session";
import { usePersistentState } from "@/lib/hooks";
import { SearchModal } from "@/components/shell/SearchModal";
import { NotificationsMenu, UserMenu } from "@/components/shell/HeaderMenus";
import { SessionGate } from "@/components/shell/SessionGate";
import { Logo } from "@/components/shell/Logo";
import { PageHeaderExtra } from "@/components/ui";
import { ItensPage } from "@/features/itens/ItensPage";
import { FabricantesPage } from "@/features/fabricantes/FabricantesPage";
import { DistribuidoresPage } from "@/features/distribuidores/DistribuidoresPage";

type Page = "itens" | "fabricantes" | "distribuidores";
interface NavItem { id: Page; label: string; icon: ReactNode; group: string }

// Para adicionar um módulo: registre-o aqui e em pageMap (design system §9).
const NAV: NavItem[] = [
  { id: "itens", label: "Itens", icon: <Package className="w-4 h-4 shrink-0" />, group: "Produtos" },
  { id: "fabricantes", label: "Fabricantes", icon: <Factory className="w-4 h-4 shrink-0" />, group: "Produtos" },
  { id: "distribuidores", label: "Distribuidores", icon: <Truck className="w-4 h-4 shrink-0" />, group: "Produtos" },
];

// Rotas que o Centinela usa no submenu (itensSubmenu do registro do módulo). A plataforma abre
// o iframe em /modulos/produtos/<rota>; o módulo avisa trocas de tela com "modulo:navegar".
const ROTA: Record<Page, string> = { itens: "/", fabricantes: "/fabricantes", distribuidores: "/distribuidores" };

function paginaDaUrl(): Page {
  const base = import.meta.env.BASE_URL.replace(/\/$/, "");
  const rota = window.location.pathname.slice(base.length).replace(/\/+$/, "") || "/";
  const achada = (Object.keys(ROTA) as Page[]).find((p) => ROTA[p] !== "/" && (rota === ROTA[p] || rota.startsWith(`${ROTA[p]}/`)));
  return achada ?? "itens";
}

function usePagina(): [Page, (p: Page) => void] {
  const [salva, setSalva] = usePersistentState<Page>("pagina", "itens");
  const [daUrl, setDaUrl] = useState<Page>(paginaDaUrl);
  const irPara = useCallback((p: Page) => {
    setDaUrl(p);
    const base = import.meta.env.BASE_URL.replace(/\/$/, "");
    window.history.replaceState(null, "", base + ROTA[p]);
    window.parent.postMessage({ tipo: "modulo:navegar", rota: ROTA[p] }, window.location.origin);
  }, []);
  return embutidoNaPlataforma ? [daUrl, irPara] : [salva, setSalva];
}

export default function App() {
  const { estado } = useSession();
  const [page, setPage] = usePagina();
  const [recolhida, setRecolhida] = usePersistentState("sidebar.recolhida", false);
  const [buscaAberta, setBuscaAberta] = useState(false);
  const [abrirItemId, setAbrirItemId] = useState<string | null>(null);
  const [backendOk, setBackendOk] = useState<boolean | null>(null);

  // Atalho Ctrl/⌘ + K para a busca global.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") { e.preventDefault(); setBuscaAberta(true); }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  // Health público: alimenta o aviso de conectividade nas notificações.
  useEffect(() => {
    const check = () => api.health().then(() => setBackendOk(true)).catch(() => setBackendOk(false));
    check();
    const t = setInterval(check, 60_000);
    return () => clearInterval(t);
  }, []);

  const abrirItem = useCallback((id: string) => { setPage("itens"); setAbrirItemId(id); }, [setPage]);
  const itemAberto = useCallback(() => setAbrirItemId(null), []);

  if (estado.status !== "ok") return <SessionGate />;

  const pageMap: Record<Page, ReactElement> = {
    itens: <ItensPage abrirItemId={abrirItemId} onAberto={itemAberto} />,
    fabricantes: <FabricantesPage key="fabricantes" onAbrirItem={abrirItem} />,
    distribuidores: <DistribuidoresPage key="distribuidores" onAbrirItem={abrirItem} />,
  };
  const atual = NAV.find((n) => n.id === page) ?? NAV[0];
  const grupos = [...new Set(NAV.map((n) => n.group))];
  const busca = (
    <SearchModal
      open={buscaAberta}
      onClose={() => setBuscaAberta(false)}
      onSelect={(r) => { setBuscaAberta(false); abrirItem(r.id); }}
    />
  );

  // Dentro do Centinela a plataforma já desenha sidebar, breadcrumb, busca, notificações e
  // usuário. O módulo mostra só o conteúdo; as abas entre as suas telas ficam logo abaixo do
  // PageHeader de cada página (via PageHeaderExtra).
  if (embutidoNaPlataforma) {
    const abas = (
        <nav className="flex items-center gap-1 px-4 sm:px-5 bg-white border-b border-gray-100 shrink-0" aria-label="Telas de Produtos">
          {NAV.map((n) => {
            const ativo = n.id === page;
            return (
              <button
                key={n.id}
                onClick={() => setPage(n.id)}
                aria-current={ativo ? "page" : undefined}
                className={`flex items-center gap-2 h-11 px-3 -mb-px border-b-2 text-sm transition-colors
                  ${ativo ? "border-brand-600 text-brand-950 font-medium" : "border-transparent text-gray-500 hover:text-gray-800"}`}
              >
                {n.icon}
                {n.label}
              </button>
            );
          })}
        </nav>
    );
    return (
      <div className="flex flex-col h-full overflow-hidden">
        <PageHeaderExtra.Provider value={abas}>
          <main className="flex-1 min-h-0">{pageMap[page]}</main>
        </PageHeaderExtra.Provider>
        {busca}
      </div>
    );
  }

  return (
    <div className="flex h-full overflow-hidden">
      <aside className={`flex flex-col bg-brand-950 text-gray-300 shrink-0 transition-[width] duration-200 ${recolhida ? "w-14" : "w-14 md:w-60"}`}>
        <div className={`flex items-center gap-2.5 h-14 border-b border-brand-900 ${recolhida ? "justify-center" : "justify-center md:justify-start md:px-4"}`}>
          <Logo className="w-9 h-9" />
          {!recolhida && (
            <div className="hidden md:block leading-tight">
              <p className="text-white font-bold text-sm">Centinela</p>
              <p className="text-brand-400 text-[11px]">soluções · Produtos</p>
            </div>
          )}
        </div>

        <nav className="flex-1 overflow-y-auto py-3">
          {grupos.map((g) => (
            <div key={g} className="mb-2">
              {!recolhida && <p className="hidden md:block px-4 pb-1.5 text-[11px] font-bold uppercase tracking-wider text-brand-500">{g}</p>}
              {NAV.filter((n) => n.group === g).map((n) => {
                const ativo = n.id === page;
                return (
                  <button
                    key={n.id}
                    onClick={() => setPage(n.id)}
                    title={n.label}
                    className={`w-full flex items-center gap-2.5 h-9 text-sm transition-colors
                      ${recolhida ? "justify-center" : "justify-center md:justify-start md:px-4"}
                      ${ativo ? "bg-brand-700 text-white font-medium" : "text-gray-300 hover:bg-brand-800 hover:text-white"}`}
                  >
                    {n.icon}
                    {!recolhida && <span className="hidden md:inline">{n.label}</span>}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <button
          onClick={() => setRecolhida((r) => !r)}
          className={`hidden md:flex items-center gap-2 h-11 border-t border-brand-900 text-xs text-brand-300 hover:text-white hover:bg-brand-900 ${recolhida ? "justify-center" : "px-4"}`}
        >
          {recolhida ? <ChevronRight className="w-4 h-4" /> : <><ChevronLeft className="w-4 h-4" /> Recolher</>}
        </button>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="flex items-center justify-between gap-3 h-14 px-4 sm:px-5 bg-white border-b border-gray-100 shrink-0">
          <p className="text-xs text-gray-400 truncate">
            Produtos <span className="mx-1">/</span> <span className="text-gray-600">{atual.label}</span>
          </p>
          <div className="flex items-center gap-1.5 sm:gap-2">
            <button
              onClick={() => setBuscaAberta(true)}
              className="flex items-center gap-2 h-8 pl-2.5 pr-1.5 rounded-lg border border-gray-200 text-xs text-gray-400 hover:border-gray-300 hover:text-gray-600"
            >
              <Search className="w-4 h-4" />
              <span className="hidden sm:inline">Buscar</span>
              <kbd className="hidden sm:inline text-[10px] bg-gray-100 rounded-lg px-1.5 py-0.5 font-medium">Ctrl K</kbd>
            </button>
            <NotificationsMenu backendOk={backendOk} />
            <UserMenu />
          </div>
        </header>

        <main className="flex-1 min-h-0">{pageMap[page]}</main>
      </div>

      {busca}
    </div>
  );
}
