package ProjetoIntegrador.item.service;
import ProjetoIntegrador.comum.*;
import ProjetoIntegrador.seguranca.ContextoUsuario;
import ProjetoIntegrador.item.dto.*;
import ProjetoIntegrador.item.model.*;
import ProjetoIntegrador.item.repository.ItemRepository;
import ProjetoIntegrador.item.specification.ItemSpecification;
import ProjetoIntegrador.fabricante.repository.FabricanteRepository;
import ProjetoIntegrador.distribuidor.repository.DistribuidorRepository;
import ProjetoIntegrador.precificacao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@Transactional
public class ItemService {
    private final ItemRepository repo;
    private final FabricanteRepository fabricantes;
    private final DistribuidorRepository distribuidores;
    private final PrecoPublicadoRepository precos;
    private final ContextoUsuario contexto;
    public ItemService(ItemRepository repo,FabricanteRepository fabricantes,DistribuidorRepository distribuidores,
                       PrecoPublicadoRepository precos,ContextoUsuario contexto) {
        this.repo=repo; this.fabricantes=fabricantes; this.distribuidores=distribuidores; this.precos=precos; this.contexto=contexto;
    }
    public ItemResponse criar(ItemCriacaoRequest r) {
        String tenant=contexto.tenant();
        Item i=new Item();
        i.registrarCriacao(tenant,contexto.autor());
        i.setSku(sku(r.sku())); i.setNome(Patch.nome(r.nome(),"nome"));
        i.setCategoria(r.categoria()); i.setDescricao(r.descricao()); i.setUnidade(r.unidade());
        i.setPeriodicidade(r.periodicidade()); i.setQuantidadeMinima(Quantidade.ler(r.quantidadeMinima()));
        i.setAtivo(r.ativo()==null || r.ativo());
        Quantidade.validar(i.getQuantidadeMinima(),i.getUnidade());
        validarVinculos(i,r.fabricanteId(),r.distribuidorId());
        i.setFabricanteId(r.fabricanteId()); i.setDistribuidorId(r.distribuidorId());
        if(repo.existsByTenantIdAndSkuIgnoreCase(tenant,i.getSku())) throw duplicado();
        return ItemResponse.from(repo.saveAndFlush(i),null);
    }
    public ItemResponse atualizar(UUID id,ItemAtualizacaoRequest r) {
        Item i=encontrar(id); var c=r.campos();
        UUID fabricante=c.containsKey("fabricanteId")?Patch.uuid(c,"fabricanteId"):i.getFabricanteId();
        UUID distribuidor=c.containsKey("distribuidorId")?Patch.uuid(c,"distribuidorId"):i.getDistribuidorId();
        validarVinculos(i,fabricante,distribuidor);
        i.setFabricanteId(fabricante); i.setDistribuidorId(distribuidor);
        if(c.containsKey("sku")) i.setSku(sku(Patch.texto(c,"sku",false)));
        if(c.containsKey("nome")) i.setNome(Patch.nome(Patch.texto(c,"nome",false),"nome"));
        if(c.containsKey("categoria")) i.setCategoria(Categoria.from(Patch.texto(c,"categoria",false)));
        if(c.containsKey("descricao")) i.setDescricao(Patch.texto(c,"descricao",true));
        if(c.containsKey("unidade")) i.setUnidade(Patch.enumeracao(c,"unidade",UnidadeComercial.class));
        if(c.containsKey("periodicidade")) i.setPeriodicidade(Patch.enumeracao(c,"periodicidade",Periodicidade.class));
        if(c.containsKey("quantidadeMinima")) i.setQuantidadeMinima(Quantidade.ler(Patch.texto(c,"quantidadeMinima",false)));
        if(c.containsKey("ativo")) i.setAtivo(Patch.bool(c,"ativo"));
        Quantidade.validar(i.getQuantidadeMinima(),i.getUnidade());
        if(repo.existsByTenantIdAndSkuIgnoreCaseAndIdNot(i.getTenantId(),i.getSku(),i.getId())) throw duplicado();
        i.registrarAlteracao(contexto.autor());
        return ItemResponse.from(repo.saveAndFlush(i),precos(List.of(i)).get(i.getId()));
    }
    @Transactional(readOnly=true)
    public ItemResponse buscar(UUID id) {
        Item i=encontrar(id); return ItemResponse.from(i,precos(List.of(i)).get(id));
    }
    @Transactional(readOnly=true)
    public PaginaResponse<ItemResponse> listar(String q,Categoria categoria,UUID fabricante,UUID distribuidor,Boolean ativo,Pageable pageable) {
        var page=repo.findAll(ItemSpecification.filtros(contexto.tenant(),q,categoria,fabricante,distribuidor,ativo),pageable);
        var valores=precos(page.getContent());
        return PaginaResponse.from(page.map(i -> ItemResponse.from(i,valores.get(i.getId()))));
    }
    @Transactional(readOnly=true)
    public ItemResumoResponse resumo(UUID id) {
        Item i=encontrar(id); return ItemResumoResponse.from(i,precos(List.of(i)).get(id));
    }
    @Transactional(readOnly=true)
    public ItemResumoLoteResponse lote(String entrada) {
        String[] partes=entrada.split(",",-1);
        if(partes.length<1 || partes.length>100) throw RegraException.formato("ids","Informe de 1 a 100 UUIDs distintos.");
        List<UUID> ids=new ArrayList<>(); Set<UUID> unicos=new HashSet<>();
        for(String parte:partes) {
            UUID id;
            try { id=UUID.fromString(parte); if(!id.toString().equalsIgnoreCase(parte)) throw new IllegalArgumentException(); }
            catch(IllegalArgumentException e) { throw RegraException.formato("ids","UUID inválido."); }
            if(!unicos.add(id)) throw RegraException.formato("ids","IDs repetidos não são permitidos.");
            ids.add(id);
        }
        var itens=repo.findByTenantIdAndIdIn(contexto.tenant(),ids);
        var porId=itens.stream().collect(Collectors.toMap(Item::getId,Function.identity()));
        var valores=precos(itens);
        List<ItemResumoResponse> encontrados=new ArrayList<>(); List<UUID> faltantes=new ArrayList<>();
        for(UUID id:ids) {
            Item i=porId.get(id);
            if(i==null) faltantes.add(id); else encontrados.add(ItemResumoResponse.from(i,valores.get(id)));
        }
        return new ItemResumoLoteResponse(encontrados,faltantes);
    }
    @Transactional(readOnly=true)
    public List<BuscaResultadoResponse> busca(String q) {
        if(q==null || q.isBlank() || q.strip().length()<2) throw RegraException.formato("q","Informe ao menos dois caracteres não brancos.");
        return repo.findAll(ItemSpecification.filtros(contexto.tenant(),q,null,null,null,true),
            PageRequest.of(0,5,Sort.by("nome","id"))).map(BuscaResultadoResponse::from).getContent();
    }
    private Item encontrar(UUID id) { return repo.findByIdAndTenantId(id,contexto.tenant()).orElseThrow(RegraException::naoEncontrado); }
    private Map<UUID,PrecoPublicado> precos(List<Item> itens) {
        if(itens.isEmpty()) return Map.of();
        return precos.findByTenantIdAndItemIdIn(contexto.tenant(),itens.stream().map(Item::getId).toList())
            .stream().collect(Collectors.toMap(PrecoPublicado::getItemId,Function.identity()));
    }
    private void validarVinculos(Item i,UUID fabricante,UUID distribuidor) {
        if(fabricante!=null && !fabricante.equals(i.getFabricanteId())) {
            var f=fabricantes.bloquear(fabricante,i.getTenantId()).orElseThrow(RegraException::naoEncontrado);
            if(!f.isAtivo()) throw new RegraException(422,"fabricanteId","PARCEIRO_INATIVO","Fabricante inativo não pode receber novo vínculo.");
        }
        if(distribuidor!=null && !distribuidor.equals(i.getDistribuidorId())) {
            var d=distribuidores.bloquear(distribuidor,i.getTenantId()).orElseThrow(RegraException::naoEncontrado);
            if(!d.isAtivo()) throw new RegraException(422,"distribuidorId","PARCEIRO_INATIVO","Distribuidor inativo não pode receber novo vínculo.");
        }
    }
    private String sku(String valor) {
        String sku=Patch.nome(valor,"sku").toUpperCase(Locale.ROOT);
        if(sku.length()>64) throw RegraException.formato("sku","SKU deve ter até 64 caracteres.");
        return sku;
    }
    private RegraException duplicado() { return new RegraException(409,"sku","SKU_DUPLICADO","SKU já cadastrado neste tenant."); }
}
