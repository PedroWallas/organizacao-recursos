package br.senac.tads.recursos.recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Qualquer coisa que pode ser reservada: sala, professor ou material.
 * Campos que nao se aplicam ao tipo ficam nulos (ex.: capacidade de um professor).
 */
@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecurso tipo;

    private Integer capacidade;

    @Column(length = 120)
    private String localizacao;

    @Column(length = 120)
    private String competencia;

    /** Recurso restrito exige aprovacao de responsavel ou administrador. */
    @Column(nullable = false)
    private boolean restrito;

    @Column(name = "em_manutencao", nullable = false)
    private boolean emManutencao;

    protected Recurso() {
    }

    public Recurso(String nome, TipoRecurso tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public void atualizar(String nome, TipoRecurso tipo, Integer capacidade, String localizacao, String competencia,
            boolean restrito, boolean emManutencao) {
        this.nome = nome;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
        this.competencia = competencia;
        this.restrito = restrito;
        this.emManutencao = emManutencao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public String getCompetencia() {
        return competencia;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public boolean isEmManutencao() {
        return emManutencao;
    }
}
