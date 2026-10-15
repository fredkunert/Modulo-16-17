package ProjetoIntegrador.distribuidor.service;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.seguranca.ContextoUsuario;
import ProjetoIntegrador.distribuidor.dto.*;
import ProjetoIntegrador.distribuidor.model.Distribuidor;
import ProjetoIntegrador.distribuidor.repository.DistribuidorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
@Service
@Transactional
public class DistribuidorService {
    private final DistribuidorRepository repo;
    private final ContextoUsuario contexto;
    public DistribuidorService(DistribuidorRepository repo,ContextoUsuario contexto) { this.repo=repo; this.contexto=contexto; }
    public DistribuidorResponse criar(DistribuidorCriacaoRequest request) {
        var p=new Distribuidor(Patch.nome(request.nome(),"nome"),request.ativo()==null || request.ativo());
        p.registrarCriacao(contexto.tenant(),contexto.autor());
        return DistribuidorResponse.from(repo.saveAndFlush(p));
    }
    @Transactional(readOnly=true)
    public DistribuidorResponse buscar(UUID id) {
        return DistribuidorResponse.from(repo.findByIdAndTenantId(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado));
    }
    @Transactional(readOnly=true)
    public PaginaResponse<DistribuidorResponse> listar(Pageable pageable) {
        return PaginaResponse.from(repo.findByTenantId(contexto.tenant(),pageable).map(DistribuidorResponse::from));
    }
    public DistribuidorResponse atualizar(UUID id,DistribuidorAtualizacaoRequest request) {
        var p=repo.bloquear(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado);
        var campos=request.campos();
        if(campos.containsKey("nome")) p.setNome(Patch.nome(Patch.texto(campos,"nome",false),"nome"));
        if(campos.containsKey("ativo")) p.setAtivo(Patch.bool(campos,"ativo"));
        p.registrarAlteracao(contexto.autor());
        return DistribuidorResponse.from(repo.saveAndFlush(p));
    }
}
