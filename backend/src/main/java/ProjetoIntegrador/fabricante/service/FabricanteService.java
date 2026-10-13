package ProjetoIntegrador.fabricante.service;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.seguranca.ContextoUsuario;
import ProjetoIntegrador.fabricante.dto.*;
import ProjetoIntegrador.fabricante.model.Fabricante;
import ProjetoIntegrador.fabricante.repository.FabricanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
@Service
@Transactional
public class FabricanteService {
    private final FabricanteRepository repo;
    private final ContextoUsuario contexto;
    public FabricanteService(FabricanteRepository repo,ContextoUsuario contexto) { this.repo=repo; this.contexto=contexto; }
    public FabricanteResponse criar(FabricanteCriacaoRequest request) {
        var p=new Fabricante(Patch.nome(request.nome(),"nome"),request.ativo()==null || request.ativo());
        p.registrarCriacao(contexto.tenant(),contexto.autor());
        return FabricanteResponse.from(repo.saveAndFlush(p));
    }
    @Transactional(readOnly=true)
    public FabricanteResponse buscar(UUID id) {
        return FabricanteResponse.from(repo.findByIdAndTenantId(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado));
    }
    @Transactional(readOnly=true)
    public PaginaResponse<FabricanteResponse> listar(Pageable pageable) {
        return PaginaResponse.from(repo.findByTenantId(contexto.tenant(),pageable).map(FabricanteResponse::from));
    }
    public FabricanteResponse atualizar(UUID id,FabricanteAtualizacaoRequest request) {
        var p=repo.bloquear(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado);
        var campos=request.campos();
        if(campos.containsKey("nome")) p.setNome(Patch.nome(Patch.texto(campos,"nome",false),"nome"));
        if(campos.containsKey("ativo")) p.setAtivo(Patch.bool(campos,"ativo"));
        p.registrarAlteracao(contexto.autor());
        return FabricanteResponse.from(repo.saveAndFlush(p));
    }
}
