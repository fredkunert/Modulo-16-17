CREATE VIEW produtos.vw_pub_itens AS
SELECT i.id, i.tenant_id, i.sku, i.nome,
    CASE i.categoria WHEN 'LICENCA' THEN 'LICENÇA' WHEN 'SERVICO' THEN 'SERVIÇO'
        WHEN 'SEGURANCA' THEN 'SEGURANÇA' WHEN 'ANTIVIRUS' THEN 'ANTIVÍRUS'
        WHEN 'MICROSOFT_365' THEN 'MICROSOFT 365' WHEN 'GOOGLE_WORKSPACE' THEN 'GOOGLE WORKSPACE'
        ELSE i.categoria END AS categoria,
    i.unidade, i.periodicidade, i.quantidade_minima,
    CASE WHEN i.unidade IN ('GB','TB','HORA') THEN 4 ELSE 0 END AS casas_decimais_quantidade,
    i.ativo, CAST('BRL' AS varchar(3)) AS moeda, p.preco_venda, i.fabricante_id, i.distribuidor_id,
    GREATEST(i.updated_at,p.updated_at) AS updated_at
FROM produtos.itens i
LEFT JOIN produtos.precificacoes p ON p.tenant_id=i.tenant_id AND p.item_id=i.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='usr_chamados') THEN
        GRANT USAGE ON SCHEMA produtos TO usr_chamados;
        GRANT SELECT ON produtos.vw_pub_itens TO usr_chamados;
    END IF;
END $$;
