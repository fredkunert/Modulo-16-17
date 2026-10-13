package ProjetoIntegrador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authlocal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS produtos")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class AutenticacaoLocalTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    String token(String tenant, String perfil) throws Exception {
        var result = mvc.perform(post("/api/produtos/dev/token")
                        .contentType("application/json")
                        .content(mapper.writeValueAsBytes(java.util.Map.of("tenantId", tenant, "perfil", perfil))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray()).at("/data/accessToken").asText();
    }

    @Test void tokenRealPermiteCadastrarConsultarEIsolaOutroTenant() throws Exception {
        String token = token("local-a", "catalogo");
        var created = mvc.perform(post("/api/produtos/itens")
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("""
                                {"sku":"%s","nome":"Teste local","categoria":"SOFTWARE",
                                 "unidade":"UNIDADE","periodicidade":"UNICA","quantidadeMinima":"1"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/produtos/itens/")))
                .andReturn();
        String id = mapper.readTree(created.getResponse().getContentAsByteArray()).at("/data/id").asText();
        mvc.perform(get("/api/produtos/itens/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/produtos/itens/" + id)
                        .header("Authorization", "Bearer " + token("local-b", "catalogo")))
                .andExpect(status().isNotFound());
    }

    @Test void perfilResumoNaoPermiteCadastroCompleto() throws Exception {
        String token = token("local-a", "resumo");
        mvc.perform(get("/api/produtos/busca").param("q", "teste")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/produtos/itens").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test void tokenContinuaObrigatorioEAssinaturaEConferida() throws Exception {
        mvc.perform(get("/api/produtos/itens")).andExpect(status().isUnauthorized());
        String valido = token("local-a", "catalogo");
        String[] partes = valido.split("\\.");
        partes[2] = (partes[2].startsWith("A") ? "B" : "A") + partes[2].substring(1);
        mvc.perform(get("/api/produtos/itens").header("Authorization", "Bearer " + String.join(".", partes)))
                .andExpect(status().isUnauthorized());
    }

    @Test void rejeitaEmissaoRemotaMesmoComHeaderForjado() throws Exception {
        mvc.perform(post("/api/produtos/dev/token").with(request -> {
                    request.setRemoteAddr("192.168.1.10"); return request;
                }).header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isForbidden());
    }

    @Test void emiteComPadroesEValidaPerfil() throws Exception {
        mvc.perform(post("/api/produtos/dev/token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tenantId").value("tenant-local"));
        mvc.perform(post("/api/produtos/dev/token").contentType("application/json")
                        .content("{\"perfil\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }
}
