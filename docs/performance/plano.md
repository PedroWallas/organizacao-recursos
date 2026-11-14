# Plano do teste de carga

## Requisitos

| Id | Cenário | Carga | Meta |
|---|---|---|---|
| RNF-01 | Consulta de disponibilidade (`GET /recursos?tipo=SALA&livreDe=...&livreAte=...`) | 50 usuários, 5 consultas cada | p95 ≤ 300 ms; erro ≤ 1% |
| RNF-02 | Criação de reserva disputada (`POST /reservas`) | 50 usuários disputando a mesma sala no mesmo horário, 5 horários | p95 ≤ 500 ms; exatamente 1 reserva (201) por horário; demais 409; nenhum 5xx |

O 409 no RNF-02 é o comportamento certo (horário já ocupado), por isso não conta como erro.

## Ferramenta e ambiente

- **k6**, script em [`perf/carga.js`](../../perf/carga.js).
- Roda no GitHub Actions pelo workflow manual **Teste de carga** ([`.github/workflows/carga.yml`](../../.github/workflows/carga.yml)), num runner `ubuntu-latest` (2 vCPU, 7 GB), com:
  - PostgreSQL 16 como serviço do job;
  - aplicação empacotada (`java -jar`) na mesma máquina;
  - k6 na mesma máquina.
- Rodar no CI deixa o teste reproduzível por qualquer pessoa do repositório e guarda a saída como artifact.

Limite conhecido: k6, aplicação e banco dividem a mesma máquina, então os tempos medidos incluem essa disputa por CPU. Não é um ambiente de produção.

## Massa de dados (função `setup` do script)

- 1 sala disputada ("Sala da carga") + 20 salas extras, criadas pelo administrador.
- 50 solicitantes cadastrados pela API, cada um com seu token.
- Horários: um dia diferente por iteração (hoje + 30 a 34 dias), 8h às 10h.
- Aquecimento (incluído a partir da rodada 04): 200 consultas e 200 tentativas de reserva numa sala separada, em horários que não se misturam com os medidos. Essas requisições ficam fora das métricas dos cenários. Motivo: a aplicação sobe do zero a cada execução no Actions, e sem aquecimento o p95 mede a compilação JIT da partida a frio, não o regime normal. As rodadas 01 a 03 foram sem aquecimento.

## Modelo de carga

- Dois cenários em paralelo, `per-vu-iterations`: 50 usuários × 5 iterações cada.
- Total esperado: 250 consultas + 250 tentativas de reserva (sem contar o setup).
- Na iteração *n*, os 50 usuários disputam o mesmo horário *n*. Resultado esperado: 5 reservas criadas e 245 conflitos.

## Critérios de aprovação (thresholds do k6)

```
http_req_duration{cenario:consulta}  p(95) < 300
http_req_duration{cenario:reserva}   p(95) < 500
http_req_failed{cenario:consulta}    rate < 0.01
http_req_failed{cenario:reserva}     rate < 0.01   (201 e 409 são esperados)
reservas_criadas                     count == 5
respostas_inesperadas                count == 0
```

Se algum threshold falhar, o k6 sai com erro e o job fica vermelho.

## Como repetir

No GitHub: Actions → Teste de carga → Run workflow. A saída completa fica no artifact `carga-<commit>`.

Localmente (com a aplicação no ar):

```bash
k6 run -e BASE_URL=http://localhost:8080 -e ADMIN_EMAIL=admin@recursos.local -e ADMIN_SENHA=admin123 perf/carga.js
```

## Fora de escopo

Teste de resistência (horas de carga), pico acima de 50 usuários e escalonamento em várias instâncias.
