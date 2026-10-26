import { useCallback, useEffect, useState, type FormEvent } from "react";
import { AlertCircle, BadgeDollarSign, History, Lock, Pencil, RefreshCw } from "lucide-react";
import {
  Button, DetailField, DetailSection, FormField, FormSection, Input, Pagination, Select, Spinner, useToast,
} from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { dataHora } from "@/lib/format";
import { useSession } from "@/lib/session";
import type {
  MarkupTipo, Pagina, Precificacao, PrecificacaoDados, PrecificacaoHistorico, ValorTipo,
} from "@/types/produtos";

const CAMPO_LABEL: Record<keyof PrecificacaoDados, string> = {
  moeda: "Moeda", custo: "Custo", preco: "Preço", margemPercentual: "Margem",
  markupValor: "Markup", markupTipo: "Tipo de markup", impostosValor: "Impostos", impostosTipo: "Tipo de impostos",
  comissaoValor: "Comissão", comissaoTipo: "Tipo de comissão",
};
const TIPO_LABEL: Record<MarkupTipo | ValorTipo, string> = { PERCENTUAL: "Percentual", FATOR: "Fator", MONETARIO: "Monetário" };

/** Mesmos limites de @Digits do PrecificacaoRequest. */
const CASAS: Partial<Record<keyof PrecificacaoDados, number>> = {
  custo: 2, preco: 2, margemPercentual: 4, markupValor: 4, impostosValor: 4, comissaoValor: 4,
};
/** Mesma lista de CamposSensiveis.java: voltam null sem produtos.precificacao.ver_valores_sensiveis. */
const SENSIVEIS = new Set<keyof PrecificacaoDados>(["custo", "margemPercentual", "markupValor", "markupTipo", "comissaoValor", "comissaoTipo"]);
const PARES =[["markupValor", "markupTipo"], ["impostosValor", "impostosTipo"], ["comissaoValor", "comissaoTipo"]] as const;

type Form = Record<keyof PrecificacaoDados, string>;

const paraForm = (p: Precificacao | null, moedaPadrao: string): Form => ({
  moeda: p?.moeda ?? moedaPadrao,
  custo: num(p?.custo), preco: num(p?.preco), margemPercentual: num(p?.margemPercentual),
  markupValor: num(p?.markupValor), markupTipo: p?.markupTipo ?? "",
  impostosValor: num(p?.impostosValor), impostosTipo: p?.impostosTipo ?? "",
  comissaoValor: num(p?.comissaoValor), comissaoTipo: p?.comissaoTipo ?? "",
});
function num(v: number | null | undefined) {
  return v === null || v === undefined ? "" : String(v).replace(".", ",");
}

/** Converte o formulário no corpo do PUT, validando só formato/precisão (o servidor também valida). */
function paraDados(f: Form): { dados: PrecificacaoDados; erros: Record<string, string> } {
  const erros: Record<string, string> = {};
  const decimal = (campo: keyof PrecificacaoDados, casasMax: number): number | null => {
    const bruto = f[campo].trim().replace(/\s/g, "");
    if (!bruto) return null;
    const normal = bruto.includes(",") ? bruto.replace(/\./g, "").replace(",", ".") : bruto;
    if (!/^-?\d+(\.\d+)?$/.test(normal)) { erros[campo] = "Informe um número válido."; return null; }
    if ((normal.split(".")[1]?.length ?? 0) > casasMax) { erros[campo] = `No máximo ${casasMax} casas decimais.`; return null; }
    return Number(normal);
  };
  const texto = (campo: keyof PrecificacaoDados) => f[campo].trim() || null;

  const moeda = texto("moeda")?.toUpperCase() ?? null;
  if (moeda && !/^[A-Z]{3}$/.test(moeda)) erros.moeda = "Código ISO de 3 letras, ex.: BRL.";

  const dados = {
    moeda,
    custo: decimal("custo", 2),
    preco: decimal("preco", 2),
    margemPercentual: decimal("margemPercentual", 4),
    markupValor: decimal("markupValor", 4),
    markupTipo: texto("markupTipo") as MarkupTipo | null,
    impostosValor: decimal("impostosValor", f.impostosTipo === "MONETARIO" ? 2 : 4),
    impostosTipo: texto("impostosTipo") as ValorTipo | null,
    comissaoValor: decimal("comissaoValor", f.comissaoTipo === "MONETARIO" ? 2 : 4),
    comissaoTipo: texto("comissaoTipo") as ValorTipo | null,
  };
  for (const [valor, tipo] of PARES) {
    if (erros[valor]) continue;
    if (dados[valor] !== null && !dados[tipo]) erros[tipo] = "Escolha o tipo para este valor.";
    if (dados[valor] === null && dados[tipo]) erros[valor] = "Informe o valor ou deixe o tipo em branco.";
  }
  return { dados, erros };
}

export function PrecificacaoTab({
  itemId, moedaItem, onSalvo,
}: { itemId: string; moedaItem: string; onSalvo: () => void }) {
  const { can } = useSession();
  const toast = useToast();
  const verSensiveis = can("produtos.precificacao.ver_valores_sensiveis");
  // Sem ver os campos sensíveis, o PUT (que substitui tudo) apagaria valores que o usuário nem enxerga.
  const podeEditar = can("produtos.precificacao.editar") && verSensiveis;

  const [dados, setDados] = useState<Precificacao | null>(null);
  const [erro, setErro] = useState<ApiError | null>(null);
  const [form, setForm] = useState<Form | null>(null);
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);

  const [historico, setHistorico] = useState<Pagina<PrecificacaoHistorico> | null>(null);
  const [paginaHist, setPaginaHist] = useState(0);

  const carregar = useCallback(async () => {
    setErro(null);
    try {
      setDados(await api.precificacao.buscar(itemId));
    } catch (e) {
      setErro(e instanceof ApiError ? e : new ApiError(0, (e as Error).message));
    }
  }, [itemId]);

  const carregarHistorico = useCallback(async () => {
    try {
      setHistorico(await api.precificacao.historico(itemId, { pagina: paginaHist, tamanho: 10 }));
    } catch {
      setHistorico(null);
    }
  }, [itemId, paginaHist]);

  useEffect(() => { carregar(); }, [carregar]);
  useEffect(() => { carregarHistorico(); }, [carregarHistorico]);

  const moeda = dados?.moeda || moedaItem || "BRL";
  const fmtMoeda = (v: number | null) => {
    if (v === null) return null;
    try { return new Intl.NumberFormat("pt-BR", { style: "currency", currency: moeda }).format(v); }
    catch { return `${moeda} ${v.toLocaleString("pt-BR")}`; }
  };
  const fmtNum = (v: number | null) => (v === null ? null : v.toLocaleString("pt-BR", { maximumFractionDigits: 4 }));
  const fmtValorTipo = (v: number | null, tipo: string | null) => {
    if (v === null) return null;
    if (tipo === "PERCENTUAL") return `${fmtNum(v)}%`;
    if (tipo === "FATOR") return `× ${fmtNum(v)}`;
    if (tipo === "MONETARIO") return fmtMoeda(v);
    return fmtNum(v);
  };
  const sensivel = (v: string | null) =>
    verSensiveis ? v : <span className="inline-flex items-center gap-1 text-gray-400"><Lock className="w-3 h-3" /> Restrito</span>;

  const fmtHistorico = (campo: keyof PrecificacaoDados, v: number | string | null) => {
    if (v === null) return SENSIVEIS.has(campo) && !verSensiveis ? sensivel(null) : <span className="text-gray-300">—</span>;
    if (typeof v === "string") return TIPO_LABEL[v as MarkupTipo] ?? v;
    if (campo === "custo" || campo === "preco") return fmtMoeda(v);
    if (campo === "margemPercentual") return `${fmtNum(v)}%`;
    return fmtNum(v);
  };

  async function salvar(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    const { dados: corpo, erros } = paraDados(form);
    setErrosCampo(erros);
    if (Object.keys(erros).length) return;
    setSalvando(true);
    try {
      setDados(await api.precificacao.salvar(itemId, corpo));
      setForm(null);
      toast.success("Precificação salva.");
      setPaginaHist(0);
      carregarHistorico();
      onSalvo();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrosCampo(err.porCampo);
        toast.error(err.status === 409 ? "A precificação foi alterada por outra pessoa. Recarregue e tente novamente." : err.message);
      } else {
        toast.error((err as Error).message);
      }
    } finally {
      setSalvando(false);
    }
  }

  if (erro) {
    return (
      <div className="flex flex-col items-center text-center py-10">
        <div className="w-14 h-14 rounded-full bg-red-50 text-red-500 flex items-center justify-center mb-3">
          <AlertCircle className="w-7 h-7" />
        </div>
        <p className="font-semibold text-brand-950">
          {erro.status === 403 ? "Sem permissão para ver a precificação" : "Não foi possível carregar a precificação"}
        </p>
        <p className="text-sm text-gray-500 mt-1 max-w-sm">{erro.message}</p>
        <Button variant="outline" size="sm" className="mt-4" onClick={carregar}><RefreshCw className="w-3 h-3" /> Tentar novamente</Button>
      </div>
    );
  }
  if (!dados) return <Spinner />;

  if (form) {
    const campo = (k: keyof PrecificacaoDados, label: string, extra?: { placeholder?: string; hint?: string }) => (
      <FormField label={label} error={errosCampo[k]} hint={extra?.hint}>
        <Input
          inputMode="decimal"
          value={form[k]}
          invalid={!!errosCampo[k]}
          placeholder={extra?.placeholder}
          onChange={(e) => setForm({ ...form, [k]: e.target.value })}
        />
      </FormField>
    );
    const tipo = (k: "markupTipo" | "impostosTipo" | "comissaoTipo", label: string, opcoes: (MarkupTipo | ValorTipo)[]) => (
      <FormField label={label} error={errosCampo[k]}>
        <Select value={form[k]} invalid={!!errosCampo[k]} onChange={(e) => setForm({ ...form, [k]: e.target.value })}>
          <option value="">Não informado</option>
          {opcoes.map((o) => <option key={o} value={o}>{TIPO_LABEL[o]}</option>)}
        </Select>
      </FormField>
    );
    return (
      <form onSubmit={salvar} noValidate>
        <p className="text-xs text-gray-500 mb-4">
          Todos os campos são opcionais e gravados exatamente como digitados — nada é recalculado. Campo em branco fica vazio.
        </p>
        <FormSection number={1} title="Valores">
          <FormField label="Moeda" error={errosCampo.moeda} hint="Código ISO, ex.: BRL">
            <Input value={form.moeda} maxLength={3} invalid={!!errosCampo.moeda}
              onChange={(e) => setForm({ ...form, moeda: e.target.value.toUpperCase() })} />
          </FormField>
          {campo("preco", "Preço", { hint: `Até ${CASAS.preco} casas decimais` })}
          {campo("custo", "Custo", { hint: `Até ${CASAS.custo} casas decimais` })}
          {campo("margemPercentual", "Margem (%)", { hint: `Até ${CASAS.margemPercentual} casas decimais` })}
        </FormSection>
        <FormSection number={2} title="Composição">
          {campo("markupValor", "Markup")}
          {tipo("markupTipo", "Tipo de markup", ["PERCENTUAL", "FATOR"])}
          {campo("impostosValor", "Impostos", { hint: form.impostosTipo === "MONETARIO" ? "Até 2 casas decimais" : undefined })}
          {tipo("impostosTipo", "Tipo de impostos", ["PERCENTUAL", "MONETARIO"])}
          {campo("comissaoValor", "Comissão", { hint: form.comissaoTipo === "MONETARIO" ? "Até 2 casas decimais" : undefined })}
          {tipo("comissaoTipo", "Tipo de comissão", ["PERCENTUAL", "MONETARIO"])}
        </FormSection>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={() => { setForm(null); setErrosCampo({}); }} disabled={salvando}>Cancelar</Button>
          <Button type="submit" loading={salvando}>Salvar precificação</Button>
        </div>
      </form>
    );
  }

  const vazia = dados.updatedAt === null;
  const abrirEdicao = () => { setErrosCampo({}); setForm(paraForm(vazia ? null : dados, moedaItem)); };

  return (
    <>
      {vazia ? (
        <div className="flex flex-col items-center text-center py-10">
          <div className="w-14 h-14 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center mb-3">
            <BadgeDollarSign className="w-7 h-7" />
          </div>
          <p className="font-semibold text-brand-950">Sem precificação cadastrada</p>
          <p className="text-sm text-gray-500 mt-1 max-w-sm">Enquanto nada for definido, os valores permanecem vazios.</p>
          {podeEditar && <Button size="sm" className="mt-4" onClick={abrirEdicao}><Pencil className="w-3 h-3" /> Definir precificação</Button>}
        </div>
      ) : (
        <>
          <div className="flex items-center justify-between gap-3 mb-4">
            <p className="text-xs text-gray-500">
              Versão {dados.versao} · atualizada em {dataHora(dados.updatedAt)}
            </p>
            {podeEditar && <Button variant="secondary" size="sm" onClick={abrirEdicao}><Pencil className="w-3 h-3" /> Editar</Button>}
          </div>
          <DetailSection number={1} title={`Valores (${moeda})`}>
            <DetailField label="Preço" value={fmtMoeda(dados.preco)} />
            <DetailField label="Custo" value={sensivel(fmtMoeda(dados.custo))} />
            <DetailField label="Margem" value={sensivel(dados.margemPercentual === null ? null : `${fmtNum(dados.margemPercentual)}%`)} />
          </DetailSection>
          <DetailSection number={2} title="Composição">
            <DetailField label="Markup" value={sensivel(fmtValorTipo(dados.markupValor, dados.markupTipo))} />
            <DetailField label="Impostos" value={fmtValorTipo(dados.impostosValor, dados.impostosTipo)} />
            <DetailField label="Comissão" value={sensivel(fmtValorTipo(dados.comissaoValor, dados.comissaoTipo))} />
          </DetailSection>
        </>
      )}

      {!podeEditar && can("produtos.precificacao.editar") && (
        <p className="text-xs text-gray-400 mt-2">
          A edição exige também a permissão de ver valores sensíveis, para não apagar campos ocultos.
        </p>
      )}

      {historico && historico.total > 0 && (
        <section className="mt-6">
          <div className="flex items-center gap-2 mb-3">
            <History className="w-4 h-4 text-brand-700" />
            <h3 className="text-sm font-bold text-brand-950">Histórico de alterações</h3>
            <div className="flex-1 h-px bg-gray-100" />
          </div>
          <div className="rounded-lg border border-gray-100 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead className="bg-gray-50 text-gray-500">
                  <tr>
                    <th className="text-left font-semibold px-3 py-2">Campo</th>
                    <th className="text-left font-semibold px-3 py-2">De</th>
                    <th className="text-left font-semibold px-3 py-2">Para</th>
                    <th className="text-left font-semibold px-3 py-2">Por</th>
                    <th className="text-left font-semibold px-3 py-2">Quando</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {historico.itens.map((h, i) => (
                    <tr key={`${h.alteradoEm}-${h.campoAlterado}-${i}`}>
                      <td className="px-3 py-2 font-medium text-gray-700">{CAMPO_LABEL[h.campoAlterado] ?? h.campoAlterado}</td>
                      <td className="px-3 py-2 text-gray-500">{fmtHistorico(h.campoAlterado, h.valorAnterior)}</td>
                      <td className="px-3 py-2 text-gray-800">{fmtHistorico(h.campoAlterado, h.valorNovo)}</td>
                      <td className="px-3 py-2 text-gray-500">{h.alteradoPor}</td>
                      <td className="px-3 py-2 text-gray-500 whitespace-nowrap">{dataHora(h.alteradoEm)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {historico.total > historico.tamanho && (
              <Pagination pagina={historico.pagina} tamanho={historico.tamanho} total={historico.total} onPagina={setPaginaHist} />
            )}
          </div>
        </section>
      )}
    </>
  );
}
