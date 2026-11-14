// Teste de carga com k6. Roda pelo workflow "Teste de carga" ou localmente:
//   k6 run -e BASE_URL=http://localhost:8080 -e ADMIN_EMAIL=... -e ADMIN_SENHA=... perf/carga.js
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const USUARIOS = 50;
const ITERACOES = 5;

const criadas = new Counter('reservas_criadas');
const conflitos = new Counter('reservas_conflito');
const inesperadas = new Counter('respostas_inesperadas');

export const options = {
  setupTimeout: '120s',
  scenarios: {
    consulta: { executor: 'per-vu-iterations', vus: USUARIOS, iterations: ITERACOES, exec: 'consultar' },
    disputa: { executor: 'per-vu-iterations', vus: USUARIOS, iterations: ITERACOES, exec: 'reservar' },
  },
  thresholds: {
    'http_req_duration{cenario:consulta}': ['p(95)<300'],
    'http_req_duration{cenario:reserva}': ['p(95)<500'],
    'http_req_failed{cenario:consulta}': ['rate<0.01'],
    'http_req_failed{cenario:reserva}': ['rate<0.01'],
    // um horario por iteracao, disputado pelos 50 usuarios: exatamente uma reserva por horario
    reservas_criadas: [`count==${ITERACOES}`],
    respostas_inesperadas: ['count==0'],
  },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function login(email, senha) {
  const r = http.post(`${BASE}/auth/login`, JSON.stringify({ email, senha }), { headers: JSON_HEADERS });
  if (r.status !== 200) throw new Error(`login de ${email} falhou: ${r.status} ${r.body}`);
  return r.json('token');
}

export function setup() {
  const admin = login(__ENV.ADMIN_EMAIL, __ENV.ADMIN_SENHA);
  const auth = { headers: { ...JSON_HEADERS, Authorization: `Bearer ${admin}` } };
  const sala = http.post(`${BASE}/recursos`, JSON.stringify({
    nome: 'Sala da carga', tipo: 'SALA', capacidade: 40, localizacao: 'Bloco K',
  }), auth).json('id');
  for (let i = 0; i < 20; i++) {
    http.post(`${BASE}/recursos`, JSON.stringify({ nome: `Sala extra ${i}`, tipo: 'SALA', capacidade: 20 + i }), auth);
  }

  const tokens = [];
  for (let i = 0; i < USUARIOS; i++) {
    const email = `carga${i}@teste.local`;
    http.post(`${BASE}/auth/cadastro`, JSON.stringify({ nome: `Carga ${i}`, email, senha: 'carga123' }),
      { headers: JSON_HEADERS });
    tokens.push(login(email, 'carga123'));
  }
  return { sala, tokens };
}

function cabecalhos(dados, cenario) {
  const token = dados.tokens[(exec.vu.idInTest - 1) % USUARIOS];
  return { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` }, tags: { cenario } };
}

function horario(iteracao) {
  const dia = new Date();
  dia.setUTCDate(dia.getUTCDate() + 30 + iteracao);
  const data = dia.toISOString().slice(0, 10);
  return { inicio: `${data}T08:00`, fim: `${data}T10:00` };
}

export function consultar(dados) {
  const { inicio, fim } = horario(exec.vu.iterationInScenario);
  const r = http.get(`${BASE}/recursos?tipo=SALA&livreDe=${inicio}&livreAte=${fim}`, cabecalhos(dados, 'consulta'));
  if (!check(r, { 'consulta 200': (x) => x.status === 200 })) inesperadas.add(1);
}

export function reservar(dados) {
  const opcoes = cabecalhos(dados, 'reserva');
  opcoes.responseCallback = http.expectedStatuses(201, 409);
  const corpo = { recursoIds: [dados.sala], finalidade: 'carga', ...horario(exec.vu.iterationInScenario) };
  const r = http.post(`${BASE}/reservas`, JSON.stringify(corpo), opcoes);
  if (r.status === 201) criadas.add(1);
  else if (r.status === 409) conflitos.add(1);
  else inesperadas.add(1);
  check(r, { 'reserva 201 ou 409': (x) => x.status === 201 || x.status === 409 });
}
