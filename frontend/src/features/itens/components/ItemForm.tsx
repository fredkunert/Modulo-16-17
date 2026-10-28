import { useEffect, useState } from "react";
import { AlertCircle, Save } from "lucide-react";
import { Button, FormField, FormSection, Input, Modal, Select, Textarea, useToast } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import {
  CATEGORIAS, PERIODICIDADES, PERIODICIDADE_LABEL, UNIDADES, UNIDADE_LABEL, casasDecimais,
  type Categoria, type Item, type ItemAtualizacao, type ItemCriacao, type Parceiro, type Periodicidade, type Unidade,
} from "@/types/produtos";

interface FormState {
  sku: string;
  nome: string;
  categoria: Categoria | "";
  descricao: string;
  fabricanteId: string;
  distribuidorId: string;
  unidade: Unidade | "";
  periodicidade: Periodicidade | "";
  quantidadeMinima: string;
  ativo: "true" | "false";
}

const VAZIO: FormState = {
  sku: "", nome: "", categoria: "", descricao: "", fabricanteId: "", distribuidorId: "",
  unidade: "", periodicidade: "", quantidadeMinima: "1", ativo: "true",
};

function deItem(i: Item): FormState {
  return {
    sku: i.sku, nome: i.nome, categoria: i.categoria, descricao: i.descricao ?? "",
    fabricanteId: i.fabricanteId ?? "", distribuidorId: i.distribuidorId ?? "",
    unidade: i.unidade, periodicidade: i.periodicidade,
    quantidadeMinima: String(Number(i.quantidadeMinima)).replace(".", ","), ativo: i.ativo ? "true" : "false",
  };
}

/** Converte "1,5" → "1.5000" e valida conforme a unidade (mesmas regras do backend). */
function normalizarQuantidade(raw: string, unidade: Unidade | ""): { valor?: string; erro?: string } {
  const s = raw.trim().replace(",", ".");
  if (!s) return { erro: "Informe a quantidade mínima." };
  if (!/^\d+(\.\d+)?$/.test(s)) return { erro: "Use apenas números, com vírgula ou ponto decimal." };
  const casas = unidade ? casasDecimais(unidade) : 4;
  const [int, dec = ""] = s.split(".");
  const decSignificativo = dec.replace(/0+$/, "");
  if (decSignificativo.length > casas) {
    return { erro: casas === 0 ? "Esta unidade exige quantidade inteira." : "Máximo de 4 casas decimais." };
  }
  if (Number(s) <= 0) return { erro: "A quantidade deve ser maior que zero." };
  if (int.replace(/^0+/, "").length > 11) return { erro: "Quantidade acima do limite permitido." };
  return { valor: `${BigInt(int)}.${decSignificativo.padEnd(4, "0")}` };
}

export function ItemForm({
  open, item, fabricantes, distribuidores, onClose, onSaved,
}: {
  open: boolean;
  item: Item | null;
  fabricantes: Parceiro[];
  distribuidores: Parceiro[];
  onClose: () => void;
  onSaved: (i: Item) => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState<FormState>(VAZIO);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const editando = !!item;

  useEffect(() => {
    if (!open) return;
    setForm(item ? deItem(item) : VAZIO);
    setErros({});
    setErroGeral(null);
  }, [open, item]);

  const upd = <K extends keyof FormState>(k: K, v: FormState[K]) => {
    setForm((f) => ({ ...f, [k]: v }));
    setErros((e) => {
      if (!e[k]) return e;
      const { [k]: _, ...rest } = e;
      return rest;
    });
  };

  function validar() {
    const e: Record<string, string> = {};
    if (!form.sku.trim()) e.sku = "Informe o SKU.";
    else if (form.sku.trim().length > 64) e.sku = "Máximo de 64 caracteres.";
    if (!form.nome.trim()) e.nome = "Informe o nome.";
    if (!form.categoria) e.categoria = "Selecione a categoria.";
    if (!form.unidade) e.unidade = "Selecione a unidade.";
    if (!form.periodicidade) e.periodicidade = "Selecione a periodicidade.";
    const q = normalizarQuantidade(form.quantidadeMinima, form.unidade);
    if (q.erro) e.quantidadeMinima = q.erro;
    setErros(e);
    return Object.keys(e).length === 0 ? q.valor! : null;
  }

  async function salvar() {
    setErroGeral(null);
    const quantidade = validar();
    if (!quantidade) return;

    const dados: ItemCriacao = {
      sku: form.sku.trim().toUpperCase(),
      nome: form.nome.trim(),
      categoria: form.categoria as Categoria,
      descricao: form.descricao.trim() || null,
      fabricanteId: form.fabricanteId || null,
      distribuidorId: form.distribuidorId || null,
      unidade: form.unidade as Unidade,
      periodicidade: form.periodicidade as Periodicidade,
      quantidadeMinima: quantidade,
      ativo: form.ativo === "true",
    };

    setSalvando(true);
    try {
      let salvo: Item;
      if (item) {
        const patch: ItemAtualizacao = {};
        if (dados.sku !== item.sku) patch.sku = dados.sku;
        if (dados.nome !== item.nome) patch.nome = dados.nome;
        if (dados.categoria !== item.categoria) patch.categoria = dados.categoria;
        if (dados.descricao !== (item.descricao ?? null)) patch.descricao = dados.descricao;
        if (dados.fabricanteId !== item.fabricanteId) patch.fabricanteId = dados.fabricanteId;
        if (dados.distribuidorId !== item.distribuidorId) patch.distribuidorId = dados.distribuidorId;
        if (dados.unidade !== item.unidade) patch.unidade = dados.unidade;
        if (dados.periodicidade !== item.periodicidade) patch.periodicidade = dados.periodicidade;
        if (Number(dados.quantidadeMinima) !== Number(item.quantidadeMinima)) patch.quantidadeMinima = dados.quantidadeMinima;
        if (dados.ativo !== item.ativo) patch.ativo = dados.ativo;
        if (Object.keys(patch).length === 0) {
          toast.info("Nenhuma alteração para salvar.");
          onClose();
          return;
        }
        salvo = await api.itens.atualizar(item.id, patch);
        toast.success("Item atualizado.");
      } else {
        salvo = await api.itens.criar(dados);
        toast.success(`Item ${salvo.sku} cadastrado.`);
      }
      onSaved(salvo);
    } catch (e) {
      if (e instanceof ApiError) {
        const porCampo = e.porCampo;
        setErros(porCampo);
        if (Object.keys(porCampo).length === 0) setErroGeral(e.message);
      } else {
        setErroGeral((e as Error).message);
      }
    } finally {
      setSalvando(false);
    }
  }

  // Parceiros inativos só aparecem se já estiverem vinculados (o backend recusa novos vínculos com inativos).
  const opcoes = (lista: Parceiro[], atual: string) =>
    lista
      .filter((p) => p.ativo || p.id === atual)
      .map((p) => <option key={p.id} value={p.id}>{p.nome}{p.ativo ? "" : " (inativo)"}</option>);

  const casas = form.unidade ? casasDecimais(form.unidade) : null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editando ? `Editar ${item!.sku}` : "Novo Item"}
      subtitle={editando ? "Altere somente o que precisa — os demais campos permanecem intactos" : "Preencha os dados cadastrais do produto ou serviço"}
      size="xl"
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={salvando}>Cancelar</Button>
          <Button onClick={salvar} loading={salvando}>
            {!salvando && <Save className="w-4 h-4" />}
            {editando ? "Salvar alterações" : "Criar Item"}
          </Button>
        </>
      }
    >
      <form onSubmit={(e) => { e.preventDefault(); salvar(); }} noValidate>
        {erroGeral && (
          <div className="mb-5 flex items-start gap-2 rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700">
            <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" /> {erroGeral}
          </div>
        )}

        <FormSection number={1} title="Identificação">
          <FormField label="SKU" required error={erros.sku} hint="Armazenado em maiúsculas, único por tenant">
            <Input
              value={form.sku}
              onChange={(e) => upd("sku", e.target.value.toUpperCase())}
              placeholder="Ex.: BKP-001"
              maxLength={64}
              invalid={!!erros.sku}
              autoFocus
            />
          </FormField>
          <FormField label="Categoria" required error={erros.categoria}>
            <Select value={form.categoria} onChange={(e) => upd("categoria", e.target.value as Categoria)} invalid={!!erros.categoria}>
              <option value="">Selecione…</option>
              {CATEGORIAS.map((c) => <option key={c}>{c}</option>)}
            </Select>
          </FormField>
          <FormField label="Nome" required colSpan={2} error={erros.nome}>
            <Input value={form.nome} onChange={(e) => upd("nome", e.target.value)} placeholder="Ex.: Licença de backup" invalid={!!erros.nome} />
          </FormField>
          <FormField label="Descrição" colSpan={2} error={erros.descricao}>
            <Textarea value={form.descricao} onChange={(e) => upd("descricao", e.target.value)} placeholder="Ex.: Backup por usuário" />
          </FormField>
        </FormSection>

        <FormSection number={2} title="Comercialização">
          <FormField label="Unidade" required error={erros.unidade}>
            <Select value={form.unidade} onChange={(e) => upd("unidade", e.target.value as Unidade)} invalid={!!erros.unidade}>
              <option value="">Selecione…</option>
              {UNIDADES.map((u) => <option key={u} value={u}>{UNIDADE_LABEL[u]}</option>)}
            </Select>
          </FormField>
          <FormField label="Periodicidade" required error={erros.periodicidade}>
            <Select value={form.periodicidade} onChange={(e) => upd("periodicidade", e.target.value as Periodicidade)} invalid={!!erros.periodicidade}>
              <option value="">Selecione…</option>
              {PERIODICIDADES.map((p) => <option key={p} value={p}>{PERIODICIDADE_LABEL[p]}</option>)}
            </Select>
          </FormField>
          <FormField
            label="Quantidade mínima"
            required
            error={erros.quantidadeMinima}
            hint={casas === null ? "Selecione a unidade" : casas === 0 ? "Somente números inteiros" : "Até 4 casas decimais"}
          >
            <Input
              value={form.quantidadeMinima}
              onChange={(e) => upd("quantidadeMinima", e.target.value)}
              inputMode="decimal"
              invalid={!!erros.quantidadeMinima}
            />
          </FormField>
          <FormField label="Situação" error={erros.ativo}>
            <Select value={form.ativo} onChange={(e) => upd("ativo", e.target.value as "true" | "false")}>
              <option value="true">Ativo</option>
              <option value="false">Inativo</option>
            </Select>
          </FormField>
        </FormSection>

        <FormSection number={3} title="Parceiros">
          <FormField label="Fabricante" error={erros.fabricanteId}>
            <Select value={form.fabricanteId} onChange={(e) => upd("fabricanteId", e.target.value)} invalid={!!erros.fabricanteId}>
              <option value="">Nenhum</option>
              {opcoes(fabricantes, item?.fabricanteId ?? "")}
            </Select>
          </FormField>
          <FormField label="Distribuidor" error={erros.distribuidorId}>
            <Select value={form.distribuidorId} onChange={(e) => upd("distribuidorId", e.target.value)} invalid={!!erros.distribuidorId}>
              <option value="">Nenhum</option>
              {opcoes(distribuidores, item?.distribuidorId ?? "")}
            </Select>
          </FormField>
        </FormSection>
        <button type="submit" hidden />
      </form>
    </Modal>
  );
}
