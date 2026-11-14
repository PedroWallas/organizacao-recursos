# Organização de Recursos

Sistema para reservar salas, professores e materiais sem conflito de horário.
Trabalho final da disciplina Qualidade de Software (TADS, Senac, 2026.2).

Aluno: Pedro Wallas Santana da Silva

## O que o sistema faz

- Login com token JWT e três perfis: solicitante, responsável e administrador.
- Cadastro e pesquisa de recursos por tipo, capacidade, localização, competência e horário livre.
- Reserva de um ou mais recursos no mesmo horário (ex.: sala + professor + projetor).
- Bloqueio de sobreposição, inclusive da agenda do professor, e de pedidos simultâneos para o mesmo horário.
- Recurso restrito só vale depois de aprovado por responsável ou administrador.
- Recurso em manutenção não pode ser reservado.
- Retirada e devolução de material, histórico de cada mudança e aviso por webhook.
- Relatório de uso por recurso: reservas, horas e conflitos evitados.
- Tela web simples em `http://localhost:8080`.

## Pré-requisitos

- Java 21
- Docker (para o banco local e para os testes)

## Como subir

1. Suba o PostgreSQL:

   ```bash
   docker compose up -d db
   ```

2. Rode a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows (PowerShell ou cmd), use `mvnw.cmd spring-boot:run`.

3. Confira se subiu:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

   Resposta esperada: `{"status":"UP"}`.

A tela fica em http://localhost:8080 e a documentação interativa da API em http://localhost:8080/swagger-ui.html.

### Primeiro acesso

Na primeira subida é criado um administrador:

- e-mail: `admin@recursos.local`
- senha: `admin123`

Esses valores são só para a máquina local. Em outro ambiente defina `ADMIN_EMAIL`, `ADMIN_SENHA` e `JWT_SEGREDO`.

O cadastro público (`POST /auth/cadastro`) sempre cria **solicitante**. Responsáveis e outros administradores são criados pelo administrador em `POST /usuarios`.

### Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/recursos` | Banco |
| `DB_USUARIO` / `DB_SENHA` | `recursos` / `recursos` | Banco |
| `JWT_SEGREDO` | valor de desenvolvimento | Assinatura do token (mínimo 32 caracteres) |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | `admin@recursos.local` / `admin123` | Administrador inicial |
| `NOTIFICACAO_URL` | vazio | Webhook de avisos. Vazio = aviso só no log |

## Como rodar os testes

```bash
./mvnw -B verify
```

A suíte sobe um PostgreSQL de verdade com Testcontainers, então o Docker precisa estar rodando. O banco do `docker compose` não é usado nos testes.

O build falha se a cobertura ficar abaixo de **80% de linhas** ou **70% de branches**. Relatório em `target/site/jacoco/index.html`.

## Exemplo rápido com curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@recursos.local","senha":"admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -X POST localhost:8080/recursos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Sala 101","tipo":"SALA","capacidade":40,"localizacao":"Bloco A"}'

curl -X POST localhost:8080/reservas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"recursoIds":[1],"inicio":"2026-12-01T08:00","fim":"2026-12-01T10:00","finalidade":"Aula de Java"}'
```

## Perfis

| Perfil | Pode |
|---|---|
| SOLICITANTE | Pesquisar recursos; criar, alterar e cancelar as próprias reservas; ver o histórico delas |
| RESPONSAVEL | Tudo do solicitante em qualquer reserva; aprovar, rejeitar, iniciar, concluir, marcar ausência; retirada e devolução de material; relatório |
| ADMINISTRADOR | Tudo do responsável; cadastrar recursos e usuários |

## Ciclo de vida da reserva

```
SOLICITADA --aprovar--> APROVADA --iniciar--> EM_USO --concluir--> CONCLUIDA
    |                       |
    +--rejeitar--> REJEITADA +--nao-compareceu--> NAO_COMPARECEU
    +--cancelar--> CANCELADA <--cancelar--+
```

- Reserva só com recursos livres já nasce APROVADA. Se tiver algum recurso restrito, nasce SOLICITADA.
- Reserva EM_USO não pode ser cancelada.
- Não dá para concluir com material retirado e não devolvido.
- Toda mudança grava uma linha no histórico (quem, quando, de/para) na mesma transação.

## Endpoints

Todas as rotas, menos as públicas, pedem `Authorization: Bearer <token>`. Erros voltam como `{"status", "mensagem", "detalhes"}`.

| Método | Caminho | Quem | O que faz |
|---|---|---|---|
| POST | `/auth/cadastro` | público | Cria conta de solicitante |
| POST | `/auth/login` | público | Devolve o token |
| GET | `/actuator/health` | público | Saúde da aplicação |
| GET | `/recursos` | logado | Pesquisa. Filtros: `tipo`, `capacidadeMinima`, `localizacao`, `competencia`, `livreDe` + `livreAte` |
| GET | `/recursos/{id}` | logado | Detalhe |
| POST | `/recursos` | admin | Cria recurso |
| PUT | `/recursos/{id}` | admin | Atualiza (inclusive manutenção) |
| DELETE | `/recursos/{id}` | admin | Exclui se não tiver reservas |
| POST | `/reservas` | logado | Cria reserva |
| GET | `/reservas` | logado | Solicitante vê as suas; gestores veem todas |
| GET | `/reservas/{id}` | dono ou gestor | Detalhe |
| PUT | `/reservas/{id}` | dono ou gestor | Altera horário/recursos (só SOLICITADA ou APROVADA) |
| GET | `/reservas/{id}/historico` | dono ou gestor | Auditoria |
| POST | `/reservas/{id}/cancelar` | dono ou gestor | Cancela |
| POST | `/reservas/{id}/aprovar` | gestor | Aprova |
| POST | `/reservas/{id}/rejeitar` | gestor | Rejeita |
| POST | `/reservas/{id}/iniciar` | gestor | Marca em uso |
| POST | `/reservas/{id}/concluir` | gestor | Conclui |
| POST | `/reservas/{id}/nao-compareceu` | gestor | Registra ausência |
| POST | `/reservas/{id}/retiradas` | gestor | Retira material (`{"materialId": 3}`) |
| POST | `/reservas/{id}/devolucoes` | gestor | Devolve material |
| GET | `/reservas/{id}/retiradas` | gestor | Lista retiradas |
| GET | `/relatorios/utilizacao` | gestor | Uso por recurso |
| GET/POST | `/usuarios` | admin | Lista e cria usuários com qualquer perfil |

Códigos usados: 400 dados inválidos, 401 sem login, 403 sem permissão, 404 não existe, 409 conflito de horário ou transição inválida, 422 regra de negócio (ex.: recurso em manutenção).

## Documentação do projeto

| Documento | Caminho |
|---|---|
| Arquitetura e diagramas | [docs/arquitetura.md](docs/arquitetura.md) |
| Matriz de rastreabilidade | [docs/rtm.md](docs/rtm.md) |
| Plano de testes | [docs/testes/plano.md](docs/testes/plano.md) |
| Relatório de testes | [docs/testes/relatorio.md](docs/testes/relatorio.md) |
| Teste de carga | [plano](docs/performance/plano.md) e resultados ([01](docs/performance/resultado-01.md), [02](docs/performance/resultado-02.md), [03](docs/performance/resultado-03.md), [04](docs/performance/resultado-04.md)) |
| Defeitos | [docs/defeitos/](docs/defeitos/) |
| Relatório de qualidade | [docs/relatorio-qualidade.md](docs/relatorio-qualidade.md) |
| Checklist da entrega | [docs/entrega/checklist.md](docs/entrega/checklist.md) |
| Pacote de evidências | [docs/evidencias/README.md](docs/evidencias/README.md) |
| Decisão de qualidade | [docs/decisao-qualidade.md](docs/decisao-qualidade.md) |
| Mudanças por versão | [CHANGELOG.md](CHANGELOG.md) |
