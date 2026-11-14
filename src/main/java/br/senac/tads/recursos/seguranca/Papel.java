package br.senac.tads.recursos.seguranca;

public enum Papel {
    SOLICITANTE,
    RESPONSAVEL,
    ADMINISTRADOR;

    /** Responsavel e administrador aprovam, operam reservas e veem relatorios. */
    public boolean isGestor() {
        return this != SOLICITANTE;
    }
}
