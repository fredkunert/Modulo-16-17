import { useEffect, useState } from "react";

export function useDebounced<T>(value: T, ms: number): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

/** Preferência de visualização por navegador (ex.: lista/cards). Funciona mesmo sem localStorage. */
export function usePersistentState<T>(key: string, inicial: T) {
  const k = `centinela.produtos.${key}`;
  const [v, setV] = useState<T>(() => {
    try {
      const raw = localStorage.getItem(k);
      return raw === null ? inicial : (JSON.parse(raw) as T);
    } catch { return inicial; }
  });
  useEffect(() => {
    try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* armazenamento indisponível */ }
  }, [k, v]);
  return [v, setV] as const;
}
