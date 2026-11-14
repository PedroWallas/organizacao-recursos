# language: pt
Funcionalidade: Aprovação de recursos restritos
  Para que laboratórios e equipamentos sensíveis não sejam usados sem controle
  Como responsável pelos recursos
  Quero que reservas de recursos restritos só valham depois da minha aprovação

  Contexto:
    Dado o recurso restrito "Laboratório de Redes"
    E a solicitante "ana@escola.com"

  Cenário: Reserva de recurso restrito fica aguardando aprovação
    Quando ela reserva "Laboratório de Redes" amanhã das 8h às 10h
    Então a reserva fica "SOLICITADA"

  Cenário: Responsável aprova a reserva
    Dado que ela reservou "Laboratório de Redes" amanhã das 8h às 10h
    Quando o responsável aprova a reserva
    Então a reserva fica "APROVADA"

  Cenário: Solicitante não consegue aprovar a própria reserva
    Dado que ela reservou "Laboratório de Redes" amanhã das 8h às 10h
    Quando a própria solicitante tenta aprovar a reserva
    Então a resposta é 403
    E a reserva continua "SOLICITADA"
