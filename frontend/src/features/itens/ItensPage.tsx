import { useCallback, useEffect, useRef, useState } from "react";
import { AlertCircle, ArrowDownAZ, ArrowUpZA, Ban, Package, Plus, RefreshCw, RotateCcw, ShieldOff } from "lucide-react";
import {
  Button, EmptyState, FilterBar, FilterSelect, Modal, PageHeader, Pagination, SearchBar, Spinner, ViewToggle,
  useToast, type ViewMode,
} from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { useSession } from "@/lib/session";
import { useParceiros } from "@/lib/useParceiros";
import { useDebounced, usePersistentState } from "@/lib/hooks";
import { CATEGORIAS, type Item, type Pagina } from "@/types/produtos";
import { ItemForm } from "./components/ItemForm";
import { ItemDetailPanel } from "./components/ItemDetailPanel";
import { ItemCards, ItemTable } from "./components/ItemList";

export function ItensPage({ abrirItemId, onAberto }: { abrirItemId: string | null; onAberto: () => void }) {
  const { can } = useSession();
  const toast = useToast();
  const podeVer = can("produtos.item.ver");
  const podeCriar = can("produtos.item.criar");
  const podeEditar = can("produtos.item.editar");
  const parceiros = useParceiros(podeVer);

  const [busca, setBusca] = useState("");
  const q = useDebounced(busca.trim(), 300);
  const [categoria, setCategoria] = useState("");
  const [fabricanteId, setFabricanteId] = useState("");
  const [distribuidorId, setDistribuidorId] = useState("");
  const [situacao, setSituacao] = useState<"" | "true" | "false">("");
  const [ordenar, setOrdenar] = useState<"nome,asc" | "nome,desc">("nome,asc");
  const [pagina, setPagina] = useState(0);
  const [tamanho, setTamanho] = usePersistentState("itens.tamanho", 20);
  const [view, setView] = usePersistentState<ViewMode>("itens.view", "lista");

  const [dados, setDados] = useState<Pagina<Item> | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<ApiError | null>(null);

  const [form, setForm] = useState<{ open: boolean; item: Item | null }>({ open: false, item: null });
  const [detalhe, setDetalhe] = useState<Item | null>(null);
  const [confirmar, setConfirmar] = useState<Item | null>(null);
  const [alternando, setAlternando] = useState(false);

  // Volta para a primeira página sempre que um filtro muda.
  useEffect(() => { setPagina(0); }, [q, categoria, fabricanteId, distribuidorId, situacao, ordenar, tamanho]);

  const seq = useRef(0);
  const carregar = useCallback(async () => {
    if (!podeVer) return;
    const atual = ++seq.current; // descarta respostas de requisições anteriores que cheguem fora de ordem
    setCarregando(true);
    try {
      const r = await api.itens.listar({
        q, categoria, fabricanteId, distribuidorId, ordenar, pagina, tamanho,
        ativo: situacao === "" ? null : situacao === "true",
      });
      if (atual !== seq.current) return;
      setDados(r);
      setErro(null);
    } catch (e) {
      if (atual !== seq.current) return;
      setErro(e instanceof ApiError ? e : new ApiError(0, (e as Error).message));
    } finally {
      if (atual === seq.current) setCarregando(false);
    }
  }, [podeVer, q, categoria, fabricanteId, distribuidorId, situacao, ordenar, pagina, tamanho]);

  useEffect(() => { carregar(); }, [carregar]);

  // Abertura vinda da busca global (⌘K).
  useEffect(() => {
    if (!abrirItemId) return;
    onAberto();
    api.itens.buscar(abrirItemId).then(setDetalhe).catch((e: Error) => toast.error(e.message));
  }, [abrirItemId, onAberto, toast]);

  const aplicarSalvo = (salvo: Item) => {
    setDados((d) => d && { ...d, itens: d.itens.map((i) => (i.id === salvo.id ? salvo : i)) });
    setDetalhe((d) => (d && d.id === salvo.id ? salvo : d));
  };

  async function alternarAtivo() {
    if (!confirmar) return;
    setAlternando(true);
    try {
      const salvo = await api.itens.atualizar(confirmar.id, { ativo: !confirmar.ativo });
      aplicarSalvo(salvo);
      toast.success(salvo.ativo ? "Item reativado." : "Item inativado.");
      setConfirmar(null);
      if (situacao !== "") carregar();
    } catch (e) {
      toast.error((e as Error).message);
    } finally {
      setAlternando(false);
    }
  }

  const filtrosAtivos = !!(q || categoria || fabricanteId || distribuidorId || situacao);
  const limpar = () => { setBusca(""); setCategoria(""); setFabricanteId(""); setDistribuidorId(""); setSituacao(""); };
  const total = dados?.total ?? 0;

  if (!podeVer) {
    return (
      <div className="flex flex-col h-full">
        <PageHeader title="Itens" subtitle="Catálogo de produtos e serviços" />
        <EmptyState
          icon={<ShieldOff className="w-7 h-7" />}
          title="Sem permissão para listar o catálogo"
          description="Seu token possui apenas a permissão de resumo. Use a busca global (Ctrl + K) para localizar itens ativos."
        />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full min-h-0">
      <PageHeader
        title="Itens"
        subtitle={dados ? `${total} ${total === 1 ? "item encontrado" : "itens encontrados"}` : "Catálogo de produtos e serviços"}
        actions={
          <>
            <Button variant="secondary" size="sm" onClick={carregar} disabled={carregando} aria-label="Atualizar">
              <RefreshCw className={`w-3 h-3 ${carregando ? "animate-spin" : ""}`} />
              <span className="hidden sm:inline">Atualizar</span>
            </Button>
            {podeCriar && (
              <Button size="sm" onClick={() => setForm({ open: true, item: null })}>
                <Plus className="w-3 h-3" /> Novo Item
              </Button>
            )}
          </>
        }
      />

      <FilterBar>
        <SearchBar value={busca} onChange={setBusca} placeholder="Buscar por nome ou SKU…" className="flex-1 min-w-[200px]" />
        <FilterSelect value={categoria} onChange={(e) => setCategoria(e.target.value)}>
          <option value="">Categoria</option>
          {CATEGORIAS.map((c) => <option key={c}>{c}</option>)}
        </FilterSelect>
        <FilterSelect value={fabricanteId} onChange={(e) => setFabricanteId(e.target.value)}>
          <option value="">Fabricante</option>
          {parceiros.fabricantes.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
        </FilterSelect>
        <FilterSelect value={distribuidorId} onChange={(e) => setDistribuidorId(e.target.value)}>
          <option value="">Distribuidor</option>
          {parceiros.distribuidores.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
        </FilterSelect>
        <FilterSelect value={situacao} onChange={(e) => setSituacao(e.target.value as typeof situacao)}>
          <option value="">Situação</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </FilterSelect>
        <Button
          variant="outline"
          size="sm"
          className="h-9! shrink-0"
          onClick={() => setOrdenar((o) => (o === "nome,asc" ? "nome,desc" : "nome,asc"))}
          title={ordenar === "nome,asc" ? "Nome A→Z" : "Nome Z→A"}
        >
          {ordenar === "nome,asc" ? <ArrowDownAZ className="w-4 h-4" /> : <ArrowUpZA className="w-4 h-4" />}
        </Button>
      </FilterBar>

      <div className="flex justify-end px-5 pt-3 shrink-0">
        <ViewToggle value={view} onChange={setView} />
      </div>

      <div className="flex-1 overflow-y-auto px-5 pt-3 pb-5 relative">
        {erro ? (
          <EmptyState
            icon={<AlertCircle className="w-7 h-7" />}
            title={erro.status === 403 ? "Acesso negado" : "Não foi possível carregar os itens"}
            description={erro.message}
            action={<Button variant="outline" onClick={carregar}><RefreshCw className="w-4 h-4" /> Tentar novamente</Button>}
          />
        ) : !dados ? (
          <Spinner />
        ) : dados.itens.length === 0 ? (
          <EmptyState
            icon={<Package className="w-7 h-7" />}
            title={filtrosAtivos ? "Nenhum item corresponde aos filtros" : "Nenhum item cadastrado"}
            description={filtrosAtivos ? "Ajuste a busca ou limpe os filtros." : "Cadastre o primeiro produto ou serviço do catálogo."}
            action={
              filtrosAtivos ? <Button variant="outline" onClick={limpar}>Limpar filtros</Button>
                : podeCriar ? <Button onClick={() => setForm({ open: true, item: null })}><Plus className="w-4 h-4" /> Novo Item</Button>
                  : undefined
            }
          />
        ) : (
          <div className={carregando ? "opacity-60 transition-opacity" : "transition-opacity"}>
            {view === "lista"
              ? <ItemTable itens={dados.itens} porId={parceiros.porId} onSelect={setDetalhe} />
              : <ItemCards itens={dados.itens} porId={parceiros.porId} onSelect={setDetalhe} />}
          </div>
        )}
      </div>

      {dados && dados.total > 0 && (
        <Pagination pagina={pagina} tamanho={tamanho} total={dados.total} onPagina={setPagina} onTamanho={setTamanho} />
      )}

      <Modal open={!!detalhe} onClose={() => setDetalhe(null)} size="xl" noPad>
        {detalhe && (
          <ItemDetailPanel
            item={detalhe}
            fabricante={parceiros.porId(detalhe.fabricanteId)}
            distribuidor={parceiros.porId(detalhe.distribuidorId)}
            podeEditar={podeEditar}
            onEdit={() => setForm({ open: true, item: detalhe })}
            onToggleAtivo={() => setConfirmar(detalhe)}
            onPrecificacaoSalva={() => api.itens.buscar(detalhe.id).then(aplicarSalvo).catch(() => {})}
            onClose={() => setDetalhe(null)}
          />
        )}
      </Modal>

      <ItemForm
        open={form.open}
        item={form.item}
        fabricantes={parceiros.fabricantes}
        distribuidores={parceiros.distribuidores}
        onClose={() => setForm({ open: false, item: null })}
        onSaved={(salvo) => {
          const novo = !form.item;
          setForm({ open: false, item: null });
          if (novo) { carregar(); setDetalhe(salvo); } else aplicarSalvo(salvo);
        }}
      />

      <Modal
        open={!!confirmar}
        onClose={() => setConfirmar(null)}
        size="sm"
        title={confirmar?.ativo ? "Inativar item?" : "Reativar item?"}
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmar(null)} disabled={alternando}>Cancelar</Button>
            <Button variant={confirmar?.ativo ? "danger" : "primary"} onClick={alternarAtivo} loading={alternando}>
              {confirmar?.ativo ? <><Ban className="w-4 h-4" /> Inativar</> : <><RotateCcw className="w-4 h-4" /> Reativar</>}
            </Button>
          </>
        }
      >
        {confirmar && (
          <p className="text-sm text-gray-600">
            {confirmar.ativo
              ? <>O item <strong className="text-brand-950">{confirmar.nome}</strong> deixará de aparecer na busca global. Os vínculos com fabricante e distribuidor são preservados.</>
              : <>O item <strong className="text-brand-950">{confirmar.nome}</strong> voltará a ficar disponível no catálogo e na busca global.</>}
          </p>
        )}
      </Modal>
    </div>
  );
}
