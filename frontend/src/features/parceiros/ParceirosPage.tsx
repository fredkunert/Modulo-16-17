import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { AlertCircle, Ban, Package, Pencil, Plus, RefreshCw, RotateCcw, Save, X } from "lucide-react";
import {
  Avatar, Badge, Button, DetailField, DetailSection, EmptyState, FilterBar, FormField, FormSection, Input, Modal,
  PageHeader, Pagination, SearchBar, Select, FilterSelect, Spinner, ViewToggle, statusBadge, useToast, type ViewMode,
} from "@/components/ui";
import { api, ApiError, type TipoParceiro } from "@/lib/api";
import { useSession } from "@/lib/session";
import { usePersistentState } from "@/lib/hooks";
import { CATEGORIA_VARIANT } from "@/lib/format";
import type { Item, Pagina, Parceiro } from "@/types/produtos";

export interface ParceiroConfig {
  tipo: TipoParceiro;
  titulo: string;   // "Fabricantes"
  singular: string; // "fabricante"
  icon: ReactNode;
}

/**
 * Página compartilhada por Fabricantes e Distribuidores. O backend não oferece busca textual nessas rotas,
 * então a lista completa é carregada (páginas de 100) e filtrada no cliente.
 */
export function ParceirosPage({ cfg, onAbrirItem }: { cfg: ParceiroConfig; onAbrirItem: (id: string) => void }) {
  const { can } = useSession();
  const toast = useToast();
  const podeVer = can("produtos.item.ver");
  const podeCriar = can("produtos.item.criar");
  const podeEditar = can("produtos.item.editar");
  const Singular = cfg.singular[0].toUpperCase() + cfg.singular.slice(1);

  const [lista, setLista] = useState<Parceiro[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [busca, setBusca] = useState("");
  const [situacao, setSituacao] = useState<"" | "true" | "false">("");
  const [pagina, setPagina] = useState(0);
  const [tamanho, setTamanho] = usePersistentState(`${cfg.tipo}.tamanho`, 20);
  const [view, setView] = usePersistentState<ViewMode>(`${cfg.tipo}.view`, "lista");

  const [form, setForm] = useState<{ open: boolean; p: Parceiro | null }>({ open: false, p: null });
  const [detalhe, setDetalhe] = useState<Parceiro | null>(null);
  const [confirmar, setConfirmar] = useState<Parceiro | null>(null);
  const [alternando, setAlternando] = useState(false);

  const carregar = useCallback(async () => {
    if (!podeVer) return;
    setCarregando(true);
    try { setLista(await api.todosParceiros(cfg.tipo)); setErro(null); }
    catch (e) { setErro((e as Error).message); }
    finally { setCarregando(false); }
  }, [cfg.tipo, podeVer]);

  useEffect(() => { setLista(null); setDetalhe(null); carregar(); }, [carregar]);
  useEffect(() => { setPagina(0); }, [busca, situacao, tamanho]);

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLocaleLowerCase("pt-BR");
    return (lista ?? []).filter((p) =>
      (!termo || p.nome.toLocaleLowerCase("pt-BR").includes(termo)) &&
      (situacao === "" || String(p.ativo) === situacao));
  }, [lista, busca, situacao]);
  const visiveis = filtrados.slice(pagina * tamanho, (pagina + 1) * tamanho);

  const aplicarSalvo = (s: Parceiro) => {
    setLista((l) => (l ? (l.some((p) => p.id === s.id) ? l.map((p) => (p.id === s.id ? s : p)) : [...l, s].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"))) : l));
    setDetalhe((d) => (d && d.id === s.id ? s : d));
  };

  /** Abre com o dado da lista e confirma no servidor (GET /{tipo}/{id}), que pode ter mudado desde a listagem. */
  function abrirDetalhe(p: Parceiro) {
    setDetalhe(p);
    api.parceiros(cfg.tipo).buscar(p.id).then(aplicarSalvo).catch((e) => {
      if (e instanceof ApiError && e.status === 404) {
        toast.error(`${Singular} não encontrado. A lista foi atualizada.`);
        setDetalhe((d) => (d && d.id === p.id ? null : d));
        carregar();
      }
    });
  }

  async function alternarAtivo() {
    if (!confirmar) return;
    setAlternando(true);
    try {
      const s = await api.parceiros(cfg.tipo).atualizar(confirmar.id, { ativo: !confirmar.ativo });
      aplicarSalvo(s);
      toast.success(`${Singular} ${s.ativo ? "reativado" : "inativado"}.`);
      setConfirmar(null);
    } catch (e) { toast.error((e as Error).message); }
    finally { setAlternando(false); }
  }

  if (!podeVer) {
    return (
      <div className="flex flex-col h-full">
        <PageHeader title={cfg.titulo} />
        <EmptyState icon={cfg.icon} title={`Sem permissão para listar ${cfg.titulo.toLowerCase()}`} description="Seu token não possui a permissão produtos.item.ver." />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full min-h-0">
      <PageHeader
        title={cfg.titulo}
        subtitle={lista ? `${filtrados.length} ${filtrados.length === 1 ? `${cfg.singular} encontrado` : `${cfg.titulo.toLowerCase()} encontrados`}` : "Parceiros do catálogo"}
        actions={
          <>
            <Button variant="secondary" size="sm" onClick={carregar} disabled={carregando} aria-label="Atualizar">
              <RefreshCw className={`w-3 h-3 ${carregando ? "animate-spin" : ""}`} />
              <span className="hidden sm:inline">Atualizar</span>
            </Button>
            {podeCriar && (
              <Button size="sm" onClick={() => setForm({ open: true, p: null })}>
                <Plus className="w-3 h-3" /> Novo {Singular}
              </Button>
            )}
          </>
        }
      />

      <FilterBar>
        <SearchBar value={busca} onChange={setBusca} placeholder={`Buscar ${cfg.singular} pelo nome…`} className="flex-1 min-w-[200px]" />
        <FilterSelect value={situacao} onChange={(e) => setSituacao(e.target.value as typeof situacao)}>
          <option value="">Situação</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </FilterSelect>
      </FilterBar>

      <div className="flex justify-end px-5 pt-3 shrink-0">
        <ViewToggle value={view} onChange={setView} />
      </div>

      <div className="flex-1 overflow-y-auto px-5 pt-3 pb-5">
        {erro ? (
          <EmptyState
            icon={<AlertCircle className="w-7 h-7" />}
            title={`Não foi possível carregar ${cfg.titulo.toLowerCase()}`}
            description={erro}
            action={<Button variant="outline" onClick={carregar}><RefreshCw className="w-4 h-4" /> Tentar novamente</Button>}
          />
        ) : !lista ? (
          <Spinner />
        ) : visiveis.length === 0 ? (
          <EmptyState
            icon={cfg.icon}
            title={lista.length ? "Nenhum resultado para os filtros" : `Nenhum ${cfg.singular} cadastrado`}
            action={
              lista.length ? <Button variant="outline" onClick={() => { setBusca(""); setSituacao(""); }}>Limpar filtros</Button>
                : podeCriar ? <Button onClick={() => setForm({ open: true, p: null })}><Plus className="w-4 h-4" /> Novo {Singular}</Button> : undefined
            }
          />
        ) : view === "lista" ? (
          <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-x-auto">
            <table className="w-full text-sm min-w-[480px]">
              <thead>
                <tr className="border-b border-gray-100 text-left text-[11px] uppercase tracking-wide text-gray-500">
                  <th className="px-4 py-3 font-semibold">{Singular}</th>
                  <th className="px-4 py-3 font-semibold">Situação</th>
                  <th className="px-4 py-3 font-semibold text-right w-32">Ações</th>
                </tr>
              </thead>
              <tbody>
                {visiveis.map((p) => (
                  <tr key={p.id} onClick={() => abrirDetalhe(p)} className={`border-b border-gray-100 last:border-0 cursor-pointer hover:bg-brand-50 transition-colors ${p.ativo ? "" : "opacity-60"}`}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar text={p.nome} />
                        <span className="font-medium text-brand-950">{p.nome}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">{statusBadge(p.ativo ? "ativo" : "inativo")}</td>
                    <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                      {podeEditar && (
                        <div className="inline-flex gap-1">
                          <Button variant="ghost" size="sm" onClick={() => setForm({ open: true, p })} aria-label="Editar" title="Editar">
                            <Pencil className="w-3 h-3" />
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => setConfirmar(p)} aria-label={p.ativo ? "Inativar" : "Reativar"} title={p.ativo ? "Inativar" : "Reativar"}>
                            {p.ativo ? <Ban className="w-3 h-3" /> : <RotateCcw className="w-3 h-3" />}
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
            {visiveis.map((p) => (
              <button key={p.id} onClick={() => abrirDetalhe(p)} className={`text-left bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:border-brand-200 transition-all p-4 ${p.ativo ? "" : "opacity-60"}`}>
                <div className="flex items-start justify-between gap-2">
                  <Avatar text={p.nome} className="w-10 h-10 rounded-lg" />
                  {statusBadge(p.ativo ? "ativo" : "inativo")}
                </div>
                <p className="font-semibold text-brand-950 mt-3 leading-snug">{p.nome}</p>
                <p className="text-xs text-gray-400 mt-0.5">{Singular}</p>
              </button>
            ))}
          </div>
        )}
      </div>

      {lista && filtrados.length > 0 && (
        <Pagination pagina={pagina} tamanho={tamanho} total={filtrados.length} onPagina={setPagina} onTamanho={setTamanho} />
      )}

      <Modal open={!!detalhe} onClose={() => setDetalhe(null)} size="lg" noPad>
        {detalhe && (
          <ParceiroDetailPanel
            cfg={cfg}
            parceiro={detalhe}
            podeEditar={podeEditar}
            onEdit={() => setForm({ open: true, p: detalhe })}
            onToggleAtivo={() => setConfirmar(detalhe)}
            onClose={() => setDetalhe(null)}
            onAbrirItem={(id) => { setDetalhe(null); onAbrirItem(id); }}
          />
        )}
      </Modal>

      <ParceiroForm
        cfg={cfg}
        open={form.open}
        parceiro={form.p}
        onClose={() => setForm({ open: false, p: null })}
        onSaved={(s) => { setForm({ open: false, p: null }); aplicarSalvo(s); }}
      />

      <Modal
        open={!!confirmar}
        onClose={() => setConfirmar(null)}
        size="sm"
        title={confirmar?.ativo ? `Inativar ${cfg.singular}?` : `Reativar ${cfg.singular}?`}
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmar(null)} disabled={alternando}>Cancelar</Button>
            <Button variant={confirmar?.ativo ? "danger" : "primary"} onClick={alternarAtivo} loading={alternando}>
              {confirmar?.ativo ? "Inativar" : "Reativar"}
            </Button>
          </>
        }
      >
        {confirmar && (
          <p className="text-sm text-gray-600">
            {confirmar.ativo
              ? <><strong className="text-brand-950">{confirmar.nome}</strong> não poderá receber novos vínculos de itens. Os vínculos existentes são preservados.</>
              : <><strong className="text-brand-950">{confirmar.nome}</strong> voltará a aceitar novos vínculos de itens.</>}
          </p>
        )}
      </Modal>
    </div>
  );
}

function ParceiroForm({
  cfg, open, parceiro, onClose, onSaved,
}: { cfg: ParceiroConfig; open: boolean; parceiro: Parceiro | null; onClose: () => void; onSaved: (p: Parceiro) => void }) {
  const toast = useToast();
  const [nome, setNome] = useState("");
  const [ativo, setAtivo] = useState("true");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNome(parceiro?.nome ?? "");
    setAtivo(parceiro && !parceiro.ativo ? "false" : "true");
    setErro(null);
  }, [open, parceiro]);

  async function salvar() {
    const n = nome.trim();
    if (!n) { setErro("Informe o nome."); return; }
    setSalvando(true);
    try {
      const svc = api.parceiros(cfg.tipo);
      let s: Parceiro;
      if (parceiro) {
        const patch: { nome?: string; ativo?: boolean } = {};
        if (n !== parceiro.nome) patch.nome = n;
        if ((ativo === "true") !== parceiro.ativo) patch.ativo = ativo === "true";
        if (!Object.keys(patch).length) { toast.info("Nenhuma alteração para salvar."); onClose(); return; }
        s = await svc.atualizar(parceiro.id, patch);
        toast.success("Alterações salvas.");
      } else {
        s = await svc.criar({ nome: n, ativo: ativo === "true" });
        toast.success(`${s.nome} cadastrado.`);
      }
      onSaved(s);
    } catch (e) {
      setErro(e instanceof ApiError ? e.porCampo.nome ?? e.message : (e as Error).message);
    } finally { setSalvando(false); }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="md"
      title={parceiro ? `Editar ${cfg.singular}` : `Novo ${cfg.singular}`}
      subtitle={parceiro ? parceiro.nome : `Cadastre um ${cfg.singular} para vincular aos itens do catálogo`}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={salvando}>Cancelar</Button>
          <Button onClick={salvar} loading={salvando}>{!salvando && <Save className="w-4 h-4" />} Salvar</Button>
        </>
      }
    >
      <form onSubmit={(e) => { e.preventDefault(); salvar(); }} noValidate>
        <FormSection number={1} title="Dados principais">
          <FormField label="Nome" required colSpan={2} error={erro ?? undefined}>
            <Input value={nome} onChange={(e) => { setNome(e.target.value); setErro(null); }} invalid={!!erro} autoFocus placeholder={`Nome do ${cfg.singular}`} />
          </FormField>
          <FormField label="Situação">
            <Select value={ativo} onChange={(e) => setAtivo(e.target.value)}>
              <option value="true">Ativo</option>
              <option value="false">Inativo</option>
            </Select>
          </FormField>
        </FormSection>
        <button type="submit" hidden />
      </form>
    </Modal>
  );
}

const TABS = ["Dados", "Itens vinculados"];

function ParceiroDetailPanel({
  cfg, parceiro, podeEditar, onEdit, onToggleAtivo, onClose, onAbrirItem,
}: {
  cfg: ParceiroConfig;
  parceiro: Parceiro;
  podeEditar: boolean;
  onEdit: () => void;
  onToggleAtivo: () => void;
  onClose: () => void;
  onAbrirItem: (id: string) => void;
}) {
  const [tab, setTab] = useState(0);
  const [itens, setItens] = useState<Pagina<Item> | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (tab !== 1 || itens) return;
    const filtro = cfg.tipo === "fabricantes" ? { fabricanteId: parceiro.id } : { distribuidorId: parceiro.id };
    api.itens.listar({ ...filtro, tamanho: 100 }).then(setItens).catch((e: Error) => setErro(e.message));
  }, [tab, itens, cfg.tipo, parceiro.id]);

  return (
    <div className="flex flex-col overflow-hidden bg-white w-full min-h-0">
      <div className="px-5 pt-5 pb-4 bg-brand-950 text-white shrink-0">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <Avatar text={parceiro.nome} className="w-11 h-11 rounded-lg bg-brand-900" />
            <div className="min-w-0">
              <h2 className="font-bold text-lg leading-snug">{parceiro.nome}</h2>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-brand-300 text-sm">{cfg.singular[0].toUpperCase() + cfg.singular.slice(1)}</span>
                {statusBadge(parceiro.ativo ? "ativo" : "inativo")}
              </div>
            </div>
          </div>
          <div className="flex gap-1.5 shrink-0">
            {podeEditar && (
              <>
                <Button variant="secondary" size="sm" onClick={onEdit}><Pencil className="w-3 h-3" /> Editar</Button>
                <Button variant="secondary" size="sm" onClick={onToggleAtivo} aria-label={parceiro.ativo ? "Inativar" : "Reativar"}>
                  {parceiro.ativo ? <Ban className="w-3 h-3" /> : <RotateCcw className="w-3 h-3" />}
                </Button>
              </>
            )}
            <button onClick={onClose} aria-label="Fechar" className="p-1.5 rounded-lg hover:bg-brand-800 text-brand-300 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <div className="flex border-b border-gray-100 bg-white shrink-0 overflow-x-auto">
        {TABS.map((t, i) => (
          <button
            key={t}
            onClick={() => setTab(i)}
            className={`px-4 py-2.5 text-xs font-semibold whitespace-nowrap border-b-2 transition-colors
              ${tab === i ? "border-brand-700 text-brand-800" : "border-transparent text-gray-500 hover:text-gray-700"}`}
          >
            {t}{i === 1 && itens ? ` (${itens.total})` : ""}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-5 min-h-[220px]">
        {tab === 0 && (
          <DetailSection number={1} title="Cadastro">
            <DetailField label="Nome" value={parceiro.nome} />
            <DetailField label="Situação" value={parceiro.ativo ? "Ativo — aceita novos vínculos" : "Inativo — vínculos existentes preservados"} />
            <DetailField label="ID" value={parceiro.id} mono colSpan={2} />
          </DetailSection>
        )}
        {tab === 1 && (
          erro ? <p className="text-sm text-red-600 flex items-center gap-2"><AlertCircle className="w-4 h-4 shrink-0" />{erro}</p>
            : !itens ? <Spinner />
              : itens.itens.length === 0 ? (
                <EmptyState icon={<Package className="w-7 h-7" />} title="Nenhum item vinculado" description={`Vincule itens a este ${cfg.singular} pelo cadastro de itens.`} />
              ) : (
                <ul className="divide-y divide-gray-100 -my-2">
                  {itens.itens.map((i) => (
                    <li key={i.id}>
                      <button onClick={() => onAbrirItem(i.id)} className="w-full flex items-center gap-3 py-2.5 px-2 -mx-2 rounded-lg hover:bg-brand-50 text-left">
                        <Avatar text={i.nome} />
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-brand-950 truncate">{i.nome}</p>
                          <p className="text-xs text-gray-400 font-mono">{i.sku}</p>
                        </div>
                        <Badge variant={CATEGORIA_VARIANT[i.categoria]}>{i.categoria}</Badge>
                        {!i.ativo && statusBadge("inativo")}
                      </button>
                    </li>
                  ))}
                  {itens.total > itens.itens.length && (
                    <li className="pt-3 text-xs text-gray-400">Exibindo {itens.itens.length} de {itens.total}. Use os filtros da página Itens para ver todos.</li>
                  )}
                </ul>
              )
        )}
      </div>
    </div>
  );
}
