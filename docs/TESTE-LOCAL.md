# Testar sem o login do outro grupo

O perfil `local` gera chaves RSA temporárias e emite tokens de desenvolvimento com validade de uma hora. As permissões e o isolamento por tenant continuam funcionando. O servidor fica acessível somente em `127.0.0.1`.

## 1. Iniciar

No PowerShell, dentro da pasta `backend`, configure seu PostgreSQL e ative o perfil:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/integrador'
$env:DB_USER = 'postgres'
$credencialBanco = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $credencialBanco).Password
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

O banco `integrador` precisa existir. Não é necessário definir nenhuma variável `JWT_*` nesse perfil. Aguarde `Started CatalogoApplication` e mantenha o terminal aberto.

## 2. Obter o token

Em outro PowerShell:

```powershell
$loginLocal = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8083/api/produtos/dev/token'
$tokenLocal = $loginLocal.data.accessToken
$headersLocal = @{ Authorization = "Bearer $tokenLocal" }
```

Ou no Postman: **POST** `http://127.0.0.1:8083/api/produtos/dev/token`, sem autenticação e sem body. Copie `data.accessToken`. Nas requisições de catálogo, selecione **Authorization > Bearer Token** e cole o token.

O token padrão representa `usuario-local`, tenant `tenant-local`, com permissões de criar, editar, consultar e ver resumo.

## 3. Cadastrar e consultar

```powershell
$produtoLocal = @{
    sku = 'TESTE-' + (Get-Date -Format 'yyyyMMddHHmmss')
    nome = 'Produto de teste'
    categoria = 'SOFTWARE'
    unidade = 'UNIDADE'
    periodicidade = 'UNICA'
    quantidadeMinima = '1.0000'
} | ConvertTo-Json

$cadastroLocal = Invoke-RestMethod -Method Post `
    -Uri 'http://127.0.0.1:8083/api/produtos/itens' `
    -Headers $headersLocal -ContentType 'application/json; charset=utf-8' -Body $produtoLocal

$cadastroLocal | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri 'http://127.0.0.1:8083/api/produtos/itens' -Headers $headersLocal |
    ConvertTo-Json -Depth 10
```

Esses cadastros são reais no banco configurado, associados ao tenant de teste. Para testar um segundo tenant ou uma permissão menor, envie um body JSON ao endpoint de token:

```json
{"tenantId":"outro-tenant-local","perfil":"resumo"}
```

Perfis disponíveis: `catalogo` (criar, editar, ver, ver resumo) e `resumo` (somente busca e resumos). O token de resumo recebe 403 ao tentar listar o cadastro completo. Tokens de outro tenant não enxergam seus itens.

## Encerrar e integrar

Use Ctrl+C no terminal do servidor. Ao reiniciar, gere outro token: as chaves anteriores são descartadas. Se passar uma hora, solicite outro token.

O endpoint de emissão só existe no perfil `local`, aceita somente conexões do próprio computador e não deve ser publicado por proxy/túnel. Use esse perfil apenas para desenvolvimento. Para integrar com o grupo, inicie **sem** `local` e configure `JWT_ISSUER`, `JWT_JWKS_URI` e `JWT_AUDIENCE`; a validação via JWKS continua sendo o padrão.
