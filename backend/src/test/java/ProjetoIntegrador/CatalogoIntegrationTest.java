package ProjetoIntegrador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.*;
import java.util.stream.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    static final String BASE="/api/produtos";
    static final String CRIAR="produtos.item.criar", VER="produtos.item.ver", EDITAR="produtos.item.editar", RESUMO="produtos.item.ver_resumo";
    @BeforeEach void limpar() {
        jdbc.update("delete from produtos.precificacoes");
        jdbc.update("delete from produtos.itens");
        jdbc.update("delete from produtos.fabricantes");
        jdbc.update("delete from produtos.distribuidores");
    }
    RequestPostProcessor usuario(String tenant,String... permissoes) {
        return jwt().jwt(j -> j.subject("usuario-1").claim("tenant_id",tenant))
            .authorities(Arrays.stream(permissoes).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
    }
    Map<String,Object> produto(String sku) {
        var data=new LinkedHashMap<String,Object>();
        data.put("sku",sku); data.put("nome","Licença de backup"); data.put("categoria","LICENÇA");
        data.put("unidade","USUARIO"); data.put("periodicidade","MENSAL"); data.put("quantidadeMinima","1.0000");
        return data;
    }
    JsonNode criar(Map<String,Object> dados,String tenant) throws Exception {
        var result=mvc.perform(post(BASE+"/itens").with(usuario(tenant,CRIAR)).contentType("application/json")
            .content(mapper.writeValueAsBytes(dados)))
            .andExpect(status().isCreated()).andExpect(header().string("Location",startsWith(BASE+"/itens/")))
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.errors").isEmpty())
            .andExpect(jsonPath("$.data.quantidadeMinima").isString()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
    }
    String parceiro(String tipo,String tenant) throws Exception {
        var result=mvc.perform(post(BASE+"/"+tipo).with(usuario(tenant,CRIAR)).contentType("application/json")
            .content("{\"nome\":\"Parceiro\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray()).at("/data/id").asText();
    }
    @Test void criarAuditarEConsultar() throws Exception {
        var data=criar(produto("  bk-01  "),"a");
        assertThat(data.get("sku").asText()).isEqualTo("BK-01");
        assertThat(data.get("moeda").asText()).isEqualTo("BRL");
        assertThat(data.get("precoVenda").isNull()).isTrue();
        assertThat(data.get("ativo").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("select created_by from produtos.itens",String.class)).isEqualTo("usuario-1");
        assertThat(jdbc.queryForObject("select updated_at is not null from produtos.itens",Boolean.class)).isTrue();
        mvc.perform(get(BASE+"/itens/"+data.get("id").asText()).with(usuario("a",VER))).andExpect(status().isOk());
    }
    @Test void patchPreservaOmitidosELimpaNulos() throws Exception {
        String fabricante=parceiro("fabricantes","a");
        var entrada=produto("PATCH"); entrada.put("descricao","Inicial"); entrada.put("fabricanteId",fabricante);
        String id=criar(entrada,"a").get("id").asText();
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",EDITAR)).contentType("application/json").content("{\"ativo\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.sku").value("PATCH"))
            .andExpect(jsonPath("$.data.descricao").value("Inicial")).andExpect(jsonPath("$.data.fabricanteId").value(fabricante));
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",EDITAR)).contentType("application/json")
            .content("{\"descricao\":null,\"fabricanteId\":null,\"ativo\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.descricao").value(nullValue()))
            .andExpect(jsonPath("$.data.fabricanteId").value(nullValue())).andExpect(jsonPath("$.data.ativo").value(true));
    }
    @Test void inativoContinuaNoResumoEDuplicidadeIncluiInativo() throws Exception {
        var entrada=produto("ABC"); entrada.put("ativo",false);
        String id=criar(entrada,"a").get("id").asText();
        mvc.perform(get(BASE+"/itens/"+id+"/resumo").with(usuario("a",RESUMO)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.ativo").value(false));
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json")
            .content(mapper.writeValueAsBytes(produto(" abc "))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.errors[0].codigo").value("SKU_DUPLICADO"));
        criar(produto("abc"),"b");
    }
    @Test void isolamentoUsaTokenEIgnoraHeaderDoUsuario() throws Exception {
        String id=criar(produto("TENANT"),"a").get("id").asText();
        mvc.perform(get(BASE+"/itens/"+id).with(usuario("b",VER)).header("X-Tenant-Id","a")).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/itens").with(usuario("b",VER)).header("X-Empresa-Id","a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("b",EDITAR)).contentType("application/json").content("{\"ativo\":false}"))
            .andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/itens/"+id).with(usuario("a",VER)).header("X-Tenant-Id","b"))
            .andExpect(status().isOk());
    }
    @Test void permissoesNaoTemHierarquia() throws Exception {
        String id=criar(produto("PERM"),"a").get("id").asText();
        mvc.perform(get(BASE+"/itens")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get(BASE+"/itens/"+id).with(usuario("a",RESUMO))).andExpect(status().isForbidden());
        mvc.perform(get(BASE+"/itens/"+id+"/resumo").with(usuario("a",VER))).andExpect(status().isForbidden());
        mvc.perform(post(BASE+"/itens").with(usuario("a",EDITAR)).contentType("application/json").content(mapper.writeValueAsBytes(produto("X"))))
            .andExpect(status().isForbidden());
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",CRIAR)).contentType("application/json").content("{\"ativo\":false}"))
            .andExpect(status().isForbidden());
    }
    @Test void lotePreservaOrdemEEscondeTenantAlheio() throws Exception {
        String primeiro=criar(produto("A"),"a").get("id").asText();
        var inativo=produto("B"); inativo.put("ativo",false);
        String segundo=criar(inativo,"a").get("id").asText();
        String alheio=criar(produto("C"),"b").get("id").asText();
        String ausente=UUID.randomUUID().toString();
        mvc.perform(get(BASE+"/itens/resumo").param("ids",segundo+","+alheio+","+primeiro+","+ausente).with(usuario("a",RESUMO)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.itens[0].id").value(segundo))
            .andExpect(jsonPath("$.data.itens[0].ativo").value(false)).andExpect(jsonPath("$.data.itens[1].id").value(primeiro))
            .andExpect(jsonPath("$.data.idsNaoEncontrados[0]").value(alheio)).andExpect(jsonPath("$.data.idsNaoEncontrados[1]").value(ausente));
    }
    @Test void loteValidaLimitesDuplicacaoEUuid() throws Exception {
        String cem=IntStream.range(0,100).mapToObj(i->UUID.randomUUID().toString()).collect(Collectors.joining(","));
        mvc.perform(get(BASE+"/itens/resumo").param("ids",cem).with(usuario("a",RESUMO))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idsNaoEncontrados",hasSize(100)));
        String id=UUID.randomUUID().toString();
        for(String invalido:List.of("", "nao-uuid",id+","+id,cem+","+id))
            mvc.perform(get(BASE+"/itens/resumo").param("ids",invalido).with(usuario("a",RESUMO))).andExpect(status().isBadRequest());
    }
    @Test void buscaTemCincoResultadosOrdenadosSemInativosOuOutroTenant() throws Exception {
        for(int n=6;n>=0;n--) { var p=produto("BUSCA"+n); p.put("nome","Backup "+n); criar(p,"a"); }
        var p=produto("INATIVO");p.put("ativo",false);criar(p,"a");
        criar(produto("ALHEIO"),"b");
        mvc.perform(get(BASE+"/busca").param("q","backup").with(usuario("a",RESUMO)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data",hasSize(5)))
            .andExpect(jsonPath("$.data[0].titulo").value("Backup 0"))
            .andExpect(jsonPath("$.data[0].rota",startsWith("/itens/")))
            .andExpect(jsonPath("$.data[0]",aMapWithSize(4)));
        for(String invalido:List.of(""," ","a")) mvc.perform(get(BASE+"/busca").param("q",invalido).with(usuario("a",RESUMO)))
            .andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"fabricantes","distribuidores"})
    void parceirosCrudValidacaoIsolamentoEPaginacao(String tipo) throws Exception {
        String id=parceiro(tipo,"a");
        mvc.perform(get(BASE+"/"+tipo+"/"+id).with(usuario("a",VER))).andExpect(status().isOk());
        mvc.perform(get(BASE+"/"+tipo+"/"+id).with(usuario("b",VER))).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/"+tipo).with(usuario("a",VER))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(patch(BASE+"/"+tipo+"/"+id).with(usuario("a",EDITAR)).contentType("application/json").content("{\"nome\":\"Novo\",\"ativo\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.nome").value("Novo")).andExpect(jsonPath("$.data.ativo").value(false));
        for(String corpo:List.of("{}","{\"nome\":\"   \"}","{\"ativo\":null}","{\"custo\":1}"))
            mvc.perform(patch(BASE+"/"+tipo+"/"+id).with(usuario("a",EDITAR)).contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE+"/"+tipo).param("tamanho","101").with(usuario("a",VER))).andExpect(status().isBadRequest());
        mvc.perform(get(BASE+"/"+tipo).param("ordenar","tenantId,asc").with(usuario("a",VER))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"fabricantes","distribuidores"})
    void vinculosRecusamAlheioOuInativoMasPreservamAntigo(String tipo) throws Exception {
        String campo=tipo.equals("fabricantes")?"fabricanteId":"distribuidorId";
        String id=parceiro(tipo,"a");
        var p=produto("VINCULO");p.put(campo,id);
        String item=criar(p,"a").get("id").asText();
        mvc.perform(patch(BASE+"/"+tipo+"/"+id).with(usuario("a",EDITAR)).contentType("application/json").content("{\"ativo\":false}"))
            .andExpect(status().isOk());
        mvc.perform(patch(BASE+"/itens/"+item).with(usuario("a",EDITAR)).contentType("application/json").content("{\"nome\":\"Outro nome\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data."+campo).value(id));
        p.put("sku","NOVO");
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(post(BASE+"/itens").with(usuario("b",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isNotFound());
        p.put(campo,UUID.randomUUID().toString());
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"UNICA","MENSAL","TRIMESTRAL","SEMESTRAL","ANUAL"})
    void periodicidadesPercorremCadastroEResumo(String periodicidade) throws Exception {
        var p=produto(periodicidade);p.put("periodicidade",periodicidade);
        String id=criar(p,"a").get("id").asText();
        mvc.perform(get(BASE+"/itens/"+id+"/resumo").with(usuario("a",RESUMO)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.periodicidade").value(periodicidade));
    }
    @Test void validacaoEstritaEPatchRevalidaQuantidadeAoTrocarUnidade() throws Exception {
        var p=produto("QTD");p.put("unidade","GB");p.put("quantidadeMinima","1.2345");
        String id=criar(p,"a").get("id").asText();
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",EDITAR)).contentType("application/json").content("{\"unidade\":\"USUARIO\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].codigo").value("PRECISAO_EXCEDIDA"));
        for(String corpo:List.of("{}","{\"nome\":null}","{\"sku\":\" \"}","{\"precoVenda\":1}","{\"tenant_id\":\"b\"}","{\"quantidadeMinima\":1}","{\"ativo\":\"false\"}"))
            mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",EDITAR)).contentType("application/json").content(corpo)).andExpect(status().isBadRequest());
        p=produto("ZERO");p.put("quantidadeMinima","0");
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isUnprocessableEntity());
        p=produto("NUMERO");p.put("quantidadeMinima",1);
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isBadRequest());
        p=produto("FINANCEIRO");p.put("custo",10);
        mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json").content(mapper.writeValueAsBytes(p)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].codigo").value("CAMPO_NAO_PERMITIDO"));
    }
    @Test void consultaPrecosComoNumerosSemVazarSensivelENaoAlteraAoEditar() throws Exception {
        String id=criar(produto("PRECO"),"a").get("id").asText();
        jdbc.update("insert into produtos.precificacoes (id,tenant_id,item_id,preco_venda,preco_minimo,desconto_maximo,created_at,updated_at,created_by,updated_by,versao) values (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'teste','teste',0)",
            UUID.randomUUID(),"a",UUID.fromString(id),new java.math.BigDecimal("123.45"),null,new java.math.BigDecimal("12.5000"));
        mvc.perform(get(BASE+"/itens/"+id+"/resumo").with(usuario("a",RESUMO)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data",aMapWithSize(12)))
            .andExpect(jsonPath("$.data.precoVenda").isNumber()).andExpect(jsonPath("$.data.precoVenda").value(123.45))
            .andExpect(jsonPath("$.data.precoMinimo").value(nullValue())).andExpect(jsonPath("$.data.custo").doesNotExist());
        mvc.perform(patch(BASE+"/itens/"+id).with(usuario("a",EDITAR)).contentType("application/json").content("{\"nome\":\"Novo\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.precoVenda").value(123.45));
    }
    @Test void tokenDeServicoExigeHeaderEAuditaClientId() throws Exception {
        var svc=jwt().jwt(j->j.subject("svc:propostas").claim("clientId","propostas"))
            .authorities(new SimpleGrantedAuthority(CRIAR));
        mvc.perform(post(BASE+"/itens").with(svc).contentType("application/json").content(mapper.writeValueAsBytes(produto("SVC"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post(BASE+"/itens").with(svc).header("X-Tenant-Id","a").contentType("application/json")
            .content(mapper.writeValueAsBytes(produto("SVC")))).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select created_by from produtos.itens",String.class)).isEqualTo("propostas");
    }
    @Test void paginacaoEFiltros() throws Exception {
        criar(produto("FILTRO"),"a");
        mvc.perform(get(BASE+"/itens").with(usuario("a",VER)).param("q","FILTRO").param("categoria","LICENÇA").param("ativo","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.pagina").value(0)).andExpect(jsonPath("$.data.tamanho").value(20));
        mvc.perform(get(BASE+"/itens").with(usuario("a",VER)).param("categoria","OUTROS"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get(BASE+"/itens").with(usuario("a",VER)).param("tamanho","101")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE+"/itens").with(usuario("a",VER)).param("pagina","-1")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE+"/itens").with(usuario("a",VER)).param("ordenar","nome,invalido")).andExpect(status().isBadRequest());
    }
    @Test void emissorLocalNaoExisteNoPerfilNormal() throws Exception {
        mvc.perform(post(BASE+"/dev/token")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE+"/dev/token").with(usuario("a",CRIAR)))
            .andExpect(status().isNotFound());
    }

    @Test void healthPublico() throws Exception {
        mvc.perform(get(BASE+"/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("UP"));
    }
    @Test void concorrenciaNaoCriaSkuDuplicado() throws Exception {
        var inicio=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> cadastrar=()-> {
                inicio.await();
                return mvc.perform(post(BASE+"/itens").with(usuario("a",CRIAR)).contentType("application/json")
                    .content(mapper.writeValueAsBytes(produto("CONCORRENTE")))).andReturn().getResponse().getStatus();
            };
            var primeira=executor.submit(cadastrar);
            var segunda=executor.submit(cadastrar);
            inicio.countDown();
            assertThat(List.of(primeira.get(15,java.util.concurrent.TimeUnit.SECONDS),segunda.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from produtos.itens where sku='CONCORRENTE'",Integer.class)).isEqualTo(1);
    }
}
