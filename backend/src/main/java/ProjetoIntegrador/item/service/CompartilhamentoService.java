package ProjetoIntegrador.item.service;
import ProjetoIntegrador.comum.RegraException;
import ProjetoIntegrador.item.dto.ItemPublicoResponse;
import ProjetoIntegrador.item.repository.ItemRepository;
import ProjetoIntegrador.fabricante.repository.FabricanteRepository;
import ProjetoIntegrador.distribuidor.repository.DistribuidorRepository;
import ProjetoIntegrador.precificacao.PrecoPublicadoRepository;
import ProjetoIntegrador.seguranca.ContextoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class CompartilhamentoService {
    private final ItemRepository itens;
    private final FabricanteRepository fabricantes;
    private final DistribuidorRepository distribuidores;
    private final PrecoPublicadoRepository precos;
    private final ContextoUsuario contexto;
    public CompartilhamentoService(ItemRepository itens,FabricanteRepository fabricantes,
        DistribuidorRepository distribuidores,PrecoPublicadoRepository precos,ContextoUsuario contexto) {
        this.itens=itens; this.fabricantes=fabricantes; this.distribuidores=distribuidores;
        this.precos=precos; this.contexto=contexto;
    }
    public UUID compartilhar(UUID id) {
        var item=itens.bloquearCompartilhamento(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado);
        if(item.getTokenPublico()==null) {
            item.setTokenPublico(UUID.randomUUID());
            item.registrarAlteracao(contexto.autor());
            itens.saveAndFlush(item);
        }
        return item.getTokenPublico();
    }
    public void revogar(UUID id) {
        var item=itens.bloquearCompartilhamento(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado);
        item.setTokenPublico(null);
        item.registrarAlteracao(contexto.autor());
        itens.saveAndFlush(item);
    }
    @Transactional(readOnly=true)
    public ItemPublicoResponse consultar(UUID token) {
        var i=itens.findByTokenPublico(token).orElseThrow(RegraException::naoEncontrado);
        String fabricante=i.getFabricanteId()==null?null:fabricantes.findByIdAndTenantId(i.getFabricanteId(),i.getTenantId()).map(f->f.getNome()).orElse(null);
        String distribuidor=i.getDistribuidorId()==null?null:distribuidores.findByIdAndTenantId(i.getDistribuidorId(),i.getTenantId()).map(d->d.getNome()).orElse(null);
        var preco=precos.findByTenantIdAndItemIdIn(i.getTenantId(),List.of(i.getId())).stream().findFirst().orElse(null);
        return new ItemPublicoResponse(i.getSku(),i.getNome(),i.getCategoria(),i.getDescricao(),i.getImagemUrl(),
            i.getUnidade(),i.getPeriodicidade(),i.getQuantidadeMinima().setScale(4).toPlainString(),i.isAtivo(),
            fabricante,distribuidor,"BRL",preco==null?null:preco.getPrecoVenda(),i.getCreatedAt(),i.getUpdatedAt());
    }
}
