package ProjetoIntegrador.item.dto;
import ProjetoIntegrador.item.model.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Lista explícita de dados públicos: nunca serializar entidade ou DTO de precificação aqui. */
public record ItemPublicoResponse(String sku,String nome,Categoria categoria,String descricao,String imagemUrl,
    UnidadeComercial unidade,Periodicidade periodicidade,String quantidadeMinima,boolean ativo,
    String fabricante,String distribuidor,String moeda,BigDecimal precoVenda,Instant criadoEm,Instant atualizadoEm) {}
