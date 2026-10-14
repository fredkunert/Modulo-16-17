import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import type { Parceiro } from "@/types/produtos";

/** Carrega fabricantes e distribuidores para selects e para exibir nomes a partir de IDs. */
export function useParceiros(habilitado: boolean) {
  const [fabricantes, setFabricantes] = useState<Parceiro[]>([]);
  const [distribuidores, setDistribuidores] = useState<Parceiro[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  const recarregar = useCallback(async () => {
    if (!habilitado) return;
    try {
      const [f, d] = await Promise.all([api.todosParceiros("fabricantes"), api.todosParceiros("distribuidores")]);
      setFabricantes(f);
      setDistribuidores(d);
      setErro(null);
    } catch (e) {
      setErro((e as Error).message);
    }
  }, [habilitado]);

  useEffect(() => { recarregar(); }, [recarregar]);

  const nomes = useMemo(() => {
    const m = new Map<string, Parceiro>();
    for (const p of [...fabricantes, ...distribuidores]) m.set(p.id, p);
    return m;
  }, [fabricantes, distribuidores]);

  return { fabricantes, distribuidores, porId: (id: string | null) => (id ? nomes.get(id) ?? null : null), erro, recarregar };
}
