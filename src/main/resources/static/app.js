const estado = { token: null, papel: null, nome: null };
const $ = (seletor) => document.querySelector(seletor);

function avisar(texto, ok = false) {
  const aviso = $('#aviso');
  aviso.textContent = texto;
  aviso.className = ok ? 'ok' : '';
  aviso.hidden = false;
}

async function api(caminho, opcoes = {}) {
  const cabecalhos = { 'Content-Type': 'application/json' };
  if (estado.token) cabecalhos.Authorization = 'Bearer ' + estado.token;
  const resposta = await fetch(caminho, { ...opcoes, headers: cabecalhos });
  if (resposta.status === 204) return null;
  const corpo = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    if (resposta.status === 401 && estado.token) sair();
    const detalhes = corpo.detalhes && corpo.detalhes.length ? ' (' + corpo.detalhes.join('; ') + ')' : '';
    throw new Error((corpo.mensagem || 'Erro ' + resposta.status) + detalhes);
  }
  return corpo;
}

function dadosDo(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function celula(texto) {
  const td = document.createElement('td');
  td.textContent = texto ?? '';
  return td;
}

function botao(texto, acao) {
  const b = document.createElement('button');
  b.type = 'button';
  b.className = 'secundario';
  b.textContent = texto;
  b.addEventListener('click', acao);
  return b;
}

function cabecalho(tabela, colunas) {
  tabela.innerHTML = '';
  const tr = document.createElement('tr');
  colunas.forEach((c) => {
    const th = document.createElement('th');
    th.textContent = c;
    tr.appendChild(th);
  });
  tabela.appendChild(tr);
}

function formatar(dataHora) {
  return new Date(dataHora).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function entrar(resposta) {
  Object.assign(estado, resposta);
  sessionStorage.setItem('sessao', JSON.stringify(resposta));
  $('#quem').textContent = resposta.nome + ' (' + resposta.papel.toLowerCase() + ')';
  $('#sessao').hidden = false;
  $('#tela-login').hidden = true;
  $('#tela-app').hidden = false;
  carregarRecursos();
  carregarReservas();
}

function sair() {
  Object.assign(estado, { token: null, papel: null, nome: null });
  sessionStorage.removeItem('sessao');
  $('#sessao').hidden = true;
  $('#tela-login').hidden = false;
  $('#tela-app').hidden = true;
}

async function carregarRecursos() {
  const filtro = new URLSearchParams();
  Object.entries(dadosDo($('#form-filtro'))).forEach(([chave, valor]) => {
    if (valor) filtro.append(chave, valor);
  });
  try {
    const recursos = await api('/recursos?' + filtro);
    const tabela = $('#tabela-recursos');
    cabecalho(tabela, ['', 'Nome', 'Tipo', 'Capacidade', 'Local', 'Competência', 'Situação']);
    recursos.forEach((r) => {
      const tr = document.createElement('tr');
      const marcar = document.createElement('td');
      const caixa = document.createElement('input');
      caixa.type = 'checkbox';
      caixa.value = r.id;
      caixa.className = 'escolha';
      caixa.setAttribute('aria-label', 'Escolher ' + r.nome);
      marcar.appendChild(caixa);
      tr.appendChild(marcar);
      const situacao = r.emManutencao ? 'Em manutenção' : r.restrito ? 'Precisa de aprovação' : 'Livre';
      [r.nome, r.tipo, r.capacidade, r.localizacao, r.competencia, situacao]
        .forEach((v) => tr.appendChild(celula(v)));
      tabela.appendChild(tr);
    });
  } catch (e) {
    avisar(e.message);
  }
}

async function acaoReserva(id, acao) {
  try {
    await api('/reservas/' + id + '/' + acao, { method: 'POST' });
    avisar('Reserva atualizada.', true);
    carregarReservas();
  } catch (e) {
    avisar(e.message);
  }
}

async function carregarReservas() {
  try {
    const reservas = await api('/reservas');
    const tabela = $('#tabela-reservas');
    const gestor = estado.papel !== 'SOLICITANTE';
    cabecalho(tabela, ['Recursos', 'Início', 'Término', 'Solicitante', 'Status', 'Ações']);
    reservas.forEach((r) => {
      const tr = document.createElement('tr');
      [r.recursos.map((x) => x.nome).join(', '), formatar(r.inicio), formatar(r.fim), r.solicitante, r.status]
        .forEach((v) => tr.appendChild(celula(v)));
      const acoes = document.createElement('div');
      acoes.className = 'acoes';
      if (gestor && r.status === 'SOLICITADA') {
        acoes.append(botao('Aprovar', () => acaoReserva(r.id, 'aprovar')),
          botao('Rejeitar', () => acaoReserva(r.id, 'rejeitar')));
      }
      if (gestor && r.status === 'APROVADA') acoes.append(botao('Iniciar uso', () => acaoReserva(r.id, 'iniciar')));
      if (gestor && r.status === 'EM_USO') acoes.append(botao('Concluir', () => acaoReserva(r.id, 'concluir')));
      if (r.status === 'SOLICITADA' || r.status === 'APROVADA') {
        acoes.append(botao('Cancelar', () => acaoReserva(r.id, 'cancelar')));
      }
      const td = document.createElement('td');
      td.appendChild(acoes);
      tr.appendChild(td);
      tabela.appendChild(tr);
    });
  } catch (e) {
    avisar(e.message);
  }
}

$('#form-login').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  try {
    entrar(await api('/auth/login', { method: 'POST', body: JSON.stringify(dadosDo(ev.target)) }));
    $('#aviso').hidden = true;
  } catch (e) {
    avisar(e.message);
  }
});

$('#form-cadastro').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  try {
    await api('/auth/cadastro', { method: 'POST', body: JSON.stringify(dadosDo(ev.target)) });
    avisar('Conta criada. Agora é só entrar.', true);
    ev.target.reset();
  } catch (e) {
    avisar(e.message);
  }
});

$('#form-filtro').addEventListener('submit', (ev) => {
  ev.preventDefault();
  carregarRecursos();
});

$('#form-reserva').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const recursoIds = [...document.querySelectorAll('.escolha:checked')].map((c) => Number(c.value));
  if (!recursoIds.length) {
    avisar('Escolha pelo menos um recurso na tabela.');
    return;
  }
  const dados = dadosDo(ev.target);
  try {
    const reserva = await api('/reservas', { method: 'POST', body: JSON.stringify({ ...dados, recursoIds }) });
    avisar(reserva.status === 'SOLICITADA' ? 'Reserva enviada para aprovação.' : 'Reserva confirmada.', true);
    carregarReservas();
  } catch (e) {
    avisar(e.message);
  }
});

$('#sair').addEventListener('click', sair);

try {
  const salva = JSON.parse(sessionStorage.getItem('sessao'));
  if (salva && salva.token) entrar(salva);
} catch {
  sessionStorage.removeItem('sessao');
}
