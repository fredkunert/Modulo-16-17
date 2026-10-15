# Precificação — documentação (BE2-06)

## Comportamento

A precificação de um item guarda seis valores comerciais — custo, preço,
margem, markup, impostos e comissão — todos preenchidos manualmente pelo
funcionário, de forma **independente**. Isso é uma decisão confirmada na
Especificação do Catálogo e da Precificação (v0.2, seção 5.1), não um
detalhe de implementação:

- Alterar um campo nunca recalcula, limpa ou sobrescreve outro.
- Salvar `custo = 120` sem tocar em `preço` mantém o preço exatamente como
  estava.
- Margem e markup podem divergir matematicamente do par custo/preço — o
  servidor não valida essa conta e não bloqueia o salvamento por isso.
- Campo ausente/`null` é ausente — não vira `0`. Isso importa porque `0` e
  "não preenchido" têm significados de negócio diferentes (ex.: comissão
  zerada de propósito vs. comissão ainda não definida).

## Endpoints

| Método | Rota | Permissão |
|---|---|---|
| `GET` | `/api/produtos/itens/{itemId}/precificacao` | `produtos.precificacao.ver` |
| `PUT` | `/api/produtos/itens/{itemId}/precificacao` | `produtos.precificacao.editar` |
| `GET` | `/api/produtos/itens/{itemId}/precificacao/historico` | `produtos.precificacao.ver` |

Contrato completo em `contratos/precificacao.yaml`.

## Validação de formato e precisão (BE2-02)

O servidor valida **formato**, nunca **consistência matemática**. `PrecificacaoRequest`
tem `@Digits` batendo com a precisão de cada coluna do banco (custo/preço até
2 casas, margem/markup/impostos/comissão até 4) e `@Pattern` nos campos de
tipo (`PERCENTUAL`/`FATOR`/`MONETARIO`) e na moeda (3 letras maiúsculas).
Um valor fora do formato volta `400` com `campo`/`codigo`/`detalhe` — nunca
um erro cru de banco. Isso não contradiz "sem fórmula obrigatória": validar
que `8.5555555` tem casas demais é formato; validar que markup bate com
custo/preço seria a tal fórmula que a especificação proíbe.

### Regras que vêm do banco compartilhado

A tabela `produtos.precificacoes` é compartilhada com o serviço de catálogo e
tem `CHECK`s próprios. Por isso, além do formato, a API devolve **422** (com o
`campo` problemático em `errors`) quando:

- `markupValor`/`markupTipo`, `impostosValor`/`impostosTipo` ou
  `comissaoValor`/`comissaoTipo` vêm só pela metade — valor e tipo andam
  juntos, ou os dois ficam em branco;
- `impostosTipo`/`comissaoTipo` é `MONETARIO` e o valor tem mais de 2 casas.

Nenhuma dessas regras compara um campo com outro nem recalcula nada. No banco,
`MONETARIO` é gravado como `VALOR` (tradução em `TipoValorConverter`); a API
continua falando `MONETARIO`.

## Proteção de campos sensíveis (BE2-06)

Custo, margem, markup e comissão são **rentabilidade interna** — diferente de
preço e impostos, que quem cota um item pro cliente precisa ver. Duas
permissões:

- `produtos.precificacao.ver` — dá acesso ao endpoint.
- `produtos.precificacao.ver_valores_sensiveis` — extra, junto com a de cima,
  libera os quatro campos sensíveis (ver `CamposSensiveis.NOMES`). Sem ela,
  eles voltam `null` — tanto no `GET`/`PUT` quanto nas linhas do histórico
  que alteraram algum desses campos (a linha aparece, provando que algo
  mudou, mas `valorAnterior`/`valorNovo` vêm ocultos).

Isso é uma **proposta**, não algo que já estava definido em algum contrato:
o Modelo de Dados do Grupo 2 diz que o perfil TÉCNICO "não vê margem nem
valor de contrato", mas não especifica um nome de permissão. Confirmar com o
time antes de considerar isso fechado.

## Valores decimais como número, não texto

Ajuste da Especificação (não era assim na v0.2 original): **todo campo
decimal na API tem que ser JSON number, nunca string.** No `GET`/`PUT`
principal isso já era o caso — `custo`, `preço`, `margem` etc. sempre foram
`BigDecimal`. O ponto cego era o **histórico**: como uma linha pode registrar
tanto um campo decimal (`custo`) quanto um campo de texto (`markupTipo`), eu
guardava os dois como `String` na tabela — e isso vazava pro JSON como
`"valorAnterior": "100.00"` em vez de `"valorAnterior": 100.00`.

Corrigido: `PrecificacaoHistoricoResponse` converte de volta pra
`BigDecimal` quando o campo alterado é um dos seis campos decimais
(`custo`, `preco`, `margemPercentual`, `markupValor`, `impostosValor`,
`comissaoValor`), e mantém como texto só pros três campos de tipo
(`markupTipo`, `impostosTipo`, `comissaoTipo`) e `moeda`. No banco continua
tudo como texto — a conversão é só na saída da API.

## Histórico paginado (BE2-03)

`GET .../historico?pagina=0&tamanho=20` — mesmo formato de paginação do
Contrato de Integração (máximo 100 por página). Sem isso, um item editado
com frequência acumularia um histórico sem fim numa única resposta.

## Concorrência (BE2-04)

A tabela `precificacoes` tem uma coluna `versao` (`@Version` no JPA — lock
otimista). Funciona sem o cliente precisar enviar nada:

1. Duas pessoas abrem a tela de precificação do mesmo item, cada uma lê a
   versão atual.
2. A primeira salva — a versão no banco avança.
3. A segunda tenta salvar em cima da versão antiga: o `UPDATE` não encontra
   mais nenhuma linha com aquela versão, e o Hibernate lança
   `ObjectOptimisticLockingFailureException`.
4. O `GlobalExceptionHandler` traduz isso para **`409`**, com
   `errors[0].codigo = "CONFLITO_CONCORRENCIA"`.

O cliente (front) deve, ao receber esse 409, reconsultar o `GET` e deixar o
usuário decidir se refaz a edição em cima do valor mais recente — não deve
tentar de novo automaticamente sem mostrar isso pra pessoa.

## O que os testes cobrem

`PrecificacaoServiceTest`:
- item de outro tenant / inexistente → `NaoEncontradoException` (tanto em
  `buscar` quanto em `historico`);
- item nunca precificado → devolve objeto vazio, não erro;
- salvar grava os valores exatamente como vieram, mesmo divergindo
  matematicamente entre si;
- alterar um campo não contamina os outros nem gera histórico deles;
- campo `null` no request apaga o valor anterior sem virar zero;
- histórico devolve página vinda do repositório;
- **isolamento entre tenants**: mesmo `itemId`, tenant diferente do dono →
  trata como inexistente, e a consulta nem chega a perguntar pela
  precificação (prova que o corte acontece na checagem do item, não depois).

`GlobalExceptionHandlerTest`:
- conflito de concorrência vira `409` com o código certo;
- item não encontrado vira `404`.

`PrecificacaoControllerTest`:
- os três endpoints delegam pro service certo e devolvem o envelope padrão;
- com `produtos.precificacao.ver_valores_sensiveis`, os quatro campos
  sensíveis aparecem; sem ela, vêm `null` (preço continua visível nos dois
  casos);
- histórico pagina certo e limita `tamanho` a 100;
- linha de histórico de campo sensível vem com valores ocultos quando falta
  a permissão extra; linha de campo não sensível continua visível.

## Rodando os testes

```bash
mvn test
```

Os testes são de unidade (Mockito) — não precisam de Postgres nem do serviço
de catálogo no ar.
