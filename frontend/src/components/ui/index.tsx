import {
  createContext, useCallback, useContext, useEffect, useMemo, useRef, useState,
  type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import {
  AlertCircle, Check, CheckCircle2, ChevronLeft, ChevronRight, Info, LayoutGrid, List, Loader2, Search, X,
} from "lucide-react";

const cx = (...c: (string | false | null | undefined)[]) => c.filter(Boolean).join(" ");

/* ─────────────────────────────── Button ─────────────────────────────── */

type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type ButtonSize = "sm" | "md";

const BTN_VARIANT: Record<ButtonVariant, string> = {
  primary: "bg-brand-800 text-white hover:bg-brand-700 shadow-sm",
  secondary: "bg-brand-100 text-brand-800 hover:bg-brand-200",
  outline: "border border-brand-200 bg-white text-gray-700 hover:bg-brand-50",
  ghost: "text-gray-500 hover:bg-gray-100 hover:text-gray-800",
  danger: "bg-red-600 text-white hover:bg-red-700 shadow-sm",
};
const BTN_SIZE: Record<ButtonSize, string> = {
  sm: "h-8 px-3 text-xs gap-1.5",
  md: "h-9 px-4 text-sm gap-2",
};

export function Button({
  variant = "primary", size = "md", loading, className, children, disabled, type = "button", ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; size?: ButtonSize; loading?: boolean }) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={cx(
        "inline-flex items-center justify-center rounded-lg font-semibold transition-colors whitespace-nowrap",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-600 focus-visible:ring-offset-1",
        "disabled:opacity-50 disabled:cursor-not-allowed",
        BTN_VARIANT[variant], BTN_SIZE[size], className,
      )}
      {...rest}
    >
      {loading && <Loader2 className="w-3 h-3 animate-spin shrink-0" />}
      {children}
    </button>
  );
}

/* ─────────────────────────────── Modal ─────────────────────────────── */

const MODAL_SIZE = { sm: "max-w-md", md: "max-w-xl", lg: "max-w-2xl", xl: "max-w-4xl", full: "max-w-6xl" } as const;

export function Modal({
  open, onClose, title, subtitle, size = "xl", footer, noPad, children,
}: {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  subtitle?: ReactNode;
  size?: keyof typeof MODAL_SIZE;
  footer?: ReactNode;
  noPad?: boolean;
  children?: ReactNode;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { window.removeEventListener("keydown", onKey); document.body.style.overflow = prev; };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start sm:items-center justify-center p-2 sm:p-6 animate-fade-in">
      <div className="absolute inset-0 bg-brand-950/50 backdrop-blur-[2px]" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        className={cx(
          "relative w-full bg-white rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[calc(100vh-1rem)] sm:max-h-[calc(100vh-3rem)] animate-pop-in",
          MODAL_SIZE[size],
        )}
      >
        {noPad ? children : (
          <>
            {title && (
              <div className="flex items-start justify-between gap-4 px-6 pt-5 pb-4 border-b border-gray-100 shrink-0">
                <div>
                  <h2 className="text-lg font-bold text-brand-950 tracking-tight">{title}</h2>
                  {subtitle && <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>}
                </div>
                <button onClick={onClose} aria-label="Fechar" className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-700">
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}
            <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
            {footer && (
              <div className="flex items-center justify-end gap-2 px-6 py-4 bg-gray-50 border-t border-gray-100 shrink-0">
                {footer}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

/* ─────────────────────────── Campos de formulário ─────────────────────────── */

const FIELD_BASE =
  "w-full rounded-lg border bg-white text-sm text-gray-800 placeholder:text-gray-400 transition-colors " +
  "focus:outline-none focus:ring-2 focus:ring-brand-600/30 focus:border-brand-700 disabled:bg-gray-50 disabled:text-gray-400";
const fieldBorder = (invalid?: boolean) => (invalid ? "border-red-400" : "border-gray-300");

export function Input({ invalid, className, ...rest }: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }) {
  return <input className={cx(FIELD_BASE, fieldBorder(invalid), "h-10 px-3", className)} aria-invalid={invalid} {...rest} />;
}

export function Select({ invalid, className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }) {
  return (
    <select className={cx(FIELD_BASE, fieldBorder(invalid), "h-10 pl-3 pr-8 cursor-pointer", className)} aria-invalid={invalid} {...rest}>
      {children}
    </select>
  );
}

export function Textarea({ invalid, className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }) {
  return <textarea className={cx(FIELD_BASE, fieldBorder(invalid), "px-3 py-2 min-h-[88px] resize-y", className)} aria-invalid={invalid} {...rest} />;
}

/* ─────────────────────────────── Badge ─────────────────────────────── */

export type BadgeVariant = "green" | "red" | "blue" | "indigo" | "yellow" | "gray" | "purple" | "orange";

const BADGE: Record<BadgeVariant, string> = {
  green: "bg-brand-100 text-brand-800",
  red: "bg-red-50 text-red-700",
  blue: "bg-blue-50 text-blue-700",
  indigo: "bg-indigo-50 text-indigo-700",
  yellow: "bg-yellow-50 text-yellow-800",
  gray: "bg-gray-100 text-gray-600",
  purple: "bg-purple-50 text-purple-700",
  orange: "bg-orange-50 text-orange-700",
};

export function Badge({ variant = "gray", children, className }: { variant?: BadgeVariant; children: ReactNode; className?: string }) {
  return (
    <span className={cx("inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold whitespace-nowrap", BADGE[variant], className)}>
      {children}
    </span>
  );
}

// Mapa compartilhado de status. Módulos podem estender, mantendo as variantes existentes.
const STATUS_MAP: Record<string, BadgeVariant> = {
  ativo: "green", "cliente ativo": "green", adimplente: "green", concluído: "green",
  inativo: "gray", "sem resposta": "gray", "sem contrato": "gray", "sem serviço": "gray", "sem preço": "gray",
  cancelado: "red", inadimplente: "red",
  prospect: "blue", lead: "blue", "em andamento": "blue",
  pendente: "indigo",
  rascunho: "yellow", "em elaboração": "yellow",
  atenção: "purple",
  suspenso: "orange",
};

export function statusBadge(status: string) {
  return <Badge variant={STATUS_MAP[status.toLowerCase()] ?? "gray"}>{status}</Badge>;
}

/* ─────────────────────────── FormSection / FormField ─────────────────────────── */

export function FormSection({ number, title, children }: { number: number; title: string; children: ReactNode }) {
  return (
    <section className="mb-6 last:mb-0">
      <div className="flex items-center gap-2 mb-3">
        <span className="w-5 h-5 rounded-full bg-brand-100 text-brand-800 text-[11px] font-bold flex items-center justify-center shrink-0">
          {number}
        </span>
        <h3 className="text-sm font-bold text-brand-950">{title}</h3>
        <div className="flex-1 h-px bg-gray-100" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4 gap-y-3">{children}</div>
    </section>
  );
}

export function FormField({
  label, required, colSpan = 1, error, hint, children,
}: { label: string; required?: boolean; colSpan?: 1 | 2; error?: string; hint?: ReactNode; children: ReactNode }) {
  return (
    <label className={cx("flex flex-col gap-1", colSpan === 2 && "md:col-span-2")}>
      <span className="text-xs font-medium text-gray-600">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </span>
      {children}
      {error ? (
        <span className="text-xs text-red-600 flex items-center gap-1"><AlertCircle className="w-3 h-3 shrink-0" />{error}</span>
      ) : hint ? (
        <span className="text-xs text-gray-400">{hint}</span>
      ) : null}
    </label>
  );
}

/* ─────────────────────────── SearchBar / ViewToggle / FilterBar ─────────────────────────── */

export function SearchBar({
  value, onChange, placeholder = "Buscar…", className,
}: { value: string; onChange: (v: string) => void; placeholder?: string; className?: string }) {
  return (
    <div className={cx("relative", className)}>
      <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className={cx(FIELD_BASE, "border-gray-300 h-9 pl-9 pr-8")}
      />
      {value && (
        <button onClick={() => onChange("")} aria-label="Limpar busca" className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 rounded text-gray-400 hover:text-gray-700">
          <X className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
}

export type ViewMode = "lista" | "cards";

export function ViewToggle({ value, onChange }: { value: ViewMode; onChange: (v: ViewMode) => void }) {
  const opt = (v: ViewMode, label: string, Icon: typeof List) => (
    <button
      onClick={() => onChange(v)}
      className={cx(
        "inline-flex items-center gap-1.5 h-9 px-3 text-xs font-semibold transition-colors",
        value === v ? "bg-brand-950 text-white" : "bg-white text-gray-600 hover:bg-gray-50",
      )}
    >
      <Icon className="w-3.5 h-3.5 shrink-0" /> {label}
    </button>
  );
  return (
    <div className="inline-flex rounded-lg border border-gray-300 overflow-hidden shrink-0">
      {opt("lista", "Lista", List)}
      {opt("cards", "Cards", LayoutGrid)}
    </div>
  );
}

/** Filtros lado a lado; em telas estreitas quebram linha em vez de comprimir. */
export function FilterBar({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-wrap lg:flex-nowrap items-center gap-2 px-5 py-3 bg-white border-b border-gray-100 shrink-0">{children}</div>
  );
}

/** Select compacto para a FilterBar: largura pelo conteúdo, altura igual à SearchBar. */
export function FilterSelect({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cx(
        "h-9 w-auto max-w-[200px] shrink-0 rounded-lg border border-gray-300 bg-white pl-3 pr-8 text-sm text-gray-700 cursor-pointer",
        "focus:outline-none focus:ring-2 focus:ring-brand-600/30 focus:border-brand-700",
        className,
      )}
      {...rest}
    >
      {children}
    </select>
  );
}

/* ─────────────────────────────── Pagination ─────────────────────────────── */

export function Pagination({
  pagina, tamanho, total, onPagina, onTamanho,
}: { pagina: number; tamanho: number; total: number; onPagina: (p: number) => void; onTamanho?: (t: number) => void }) {
  const totalPaginas = Math.max(1, Math.ceil(total / tamanho));
  const de = total === 0 ? 0 : pagina * tamanho + 1;
  const ate = Math.min(total, (pagina + 1) * tamanho);
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 bg-white border-t border-gray-100 text-xs text-gray-500 shrink-0">
      <span>{de}–{ate} de {total}</span>
      <div className="flex items-center gap-3">
        {onTamanho && (
          <label className="flex items-center gap-1.5">
            Por página
            <select
              value={tamanho}
              onChange={(e) => onTamanho(Number(e.target.value))}
              className="h-7 rounded-lg border border-gray-300 bg-white px-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-brand-600/30"
            >
              {[10, 20, 50, 100].map((n) => <option key={n}>{n}</option>)}
            </select>
          </label>
        )}
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" disabled={pagina === 0} onClick={() => onPagina(pagina - 1)} aria-label="Página anterior">
            <ChevronLeft className="w-3 h-3" />
          </Button>
          <span className="px-1 font-medium text-gray-700">{pagina + 1} / {totalPaginas}</span>
          <Button variant="ghost" size="sm" disabled={pagina + 1 >= totalPaginas} onClick={() => onPagina(pagina + 1)} aria-label="Próxima página">
            <ChevronRight className="w-3 h-3" />
          </Button>
        </div>
      </div>
    </div>
  );
}

/* ─────────────────────────────── PageHeader ─────────────────────────────── */

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5 bg-brand-950 text-white shrink-0">
      <div>
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="text-brand-300 text-xs mt-0.5">{subtitle}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  );
}

/* ─────────────────────────── DetailField / DetailSection ─────────────────────────── */

export function DetailField({ label, value, mono, colSpan = 1 }: { label: string; value: ReactNode; mono?: boolean; colSpan?: 1 | 2 }) {
  const vazio = value === null || value === undefined || value === "";
  return (
    <div className={cx("min-w-0", colSpan === 2 && "md:col-span-2")}>
      <dt className="text-[11px] font-semibold uppercase tracking-wide text-gray-400">{label}</dt>
      <dd className={cx("mt-0.5 text-sm break-words", vazio ? "text-gray-300" : "text-gray-800", mono && "font-mono text-xs")}>
        {vazio ? "—" : value}
      </dd>
    </div>
  );
}

export function DetailSection({ number, title, children }: { number: number; title: string; children: ReactNode }) {
  return (
    <section className="mb-6 last:mb-0">
      <div className="flex items-center gap-2 mb-3">
        <span className="w-5 h-5 rounded-full bg-brand-100 text-brand-800 text-[11px] font-bold flex items-center justify-center shrink-0">{number}</span>
        <h3 className="text-sm font-bold text-brand-950">{title}</h3>
        <div className="flex-1 h-px bg-gray-100" />
      </div>
      <dl className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-3">{children}</dl>
    </section>
  );
}

/* ─────────────────────────────── Stepper ─────────────────────────────── */

export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex items-start w-full">
      {steps.map((s, i) => (
        <li key={s} className={cx("flex items-start", i < steps.length - 1 && "flex-1")}>
          <div className="flex flex-col items-center gap-1">
            <span
              className={cx(
                "w-7 h-7 rounded-full border-2 flex items-center justify-center text-xs font-bold",
                i < current && "bg-brand-800 border-brand-800 text-white",
                i === current && "border-brand-950 text-brand-950",
                i > current && "border-gray-200 text-gray-400",
              )}
            >
              {i < current ? <Check className="w-3 h-3" /> : i + 1}
            </span>
            <span className={cx("text-xs", i <= current ? "text-brand-950 font-medium" : "text-gray-400")}>{s}</span>
          </div>
          {i < steps.length - 1 && <div className={cx("flex-1 h-px mt-3.5 mx-2", i < current ? "bg-brand-700" : "bg-gray-200")} />}
        </li>
      ))}
    </ol>
  );
}

/* ─────────────────────── Avatar / Spinner / EmptyState ─────────────────────── */

export function Avatar({ text, className }: { text: string; className?: string }) {
  const iniciais = text.replace(/[^\p{L}\p{N} ]/gu, " ").trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join("").toUpperCase() || "?";
  return (
    <span className={cx("w-8 h-8 rounded-full bg-brand-950 text-brand-400 font-bold flex items-center justify-center text-xs shrink-0", className)}>
      {iniciais}
    </span>
  );
}

export function Spinner({ label = "Carregando…" }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-16 text-sm text-gray-400">
      <Loader2 className="w-5 h-5 animate-spin text-brand-700" /> {label}
    </div>
  );
}

export function EmptyState({ icon, title, description, action }: { icon: ReactNode; title: string; description?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      <div className="w-14 h-14 rounded-full bg-brand-50 text-brand-700 flex items-center justify-center mb-3">{icon}</div>
      <p className="font-semibold text-brand-950">{title}</p>
      {description && <p className="text-sm text-gray-500 mt-1 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

/* ─────────────────────────────── Toast ─────────────────────────────── */

type ToastKind = "success" | "error" | "info";
interface ToastItem { id: number; kind: ToastKind; message: string }
const ToastCtx = createContext<(kind: ToastKind, message: string) => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const seq = useRef(0);
  const push = useCallback((kind: ToastKind, message: string) => {
    const id = ++seq.current;
    setItems((xs) => [...xs, { id, kind, message }]);
    setTimeout(() => setItems((xs) => xs.filter((x) => x.id !== id)), kind === "error" ? 6000 : 3500);
  }, []);
  const ICON = { success: CheckCircle2, error: AlertCircle, info: Info };
  const COLOR = { success: "text-brand-700", error: "text-red-600", info: "text-blue-600" };
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="fixed bottom-4 right-4 z-[60] flex flex-col gap-2 w-[calc(100%-2rem)] max-w-sm">
        {items.map((t) => {
          const Icon = ICON[t.kind];
          return (
            <div key={t.id} role="status" className="flex items-start gap-2.5 bg-white rounded-xl shadow-lg border border-gray-100 px-4 py-3 text-sm animate-pop-in">
              <Icon className={cx("w-4 h-4 mt-0.5 shrink-0", COLOR[t.kind])} />
              <span className="flex-1 text-gray-700">{t.message}</span>
              <button onClick={() => setItems((xs) => xs.filter((x) => x.id !== t.id))} className="text-gray-300 hover:text-gray-600" aria-label="Fechar aviso">
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  const push = useContext(ToastCtx);
  return useMemo(() => ({
    success: (m: string) => push("success", m),
    error: (m: string) => push("error", m),
    info: (m: string) => push("info", m),
  }), [push]);
}
