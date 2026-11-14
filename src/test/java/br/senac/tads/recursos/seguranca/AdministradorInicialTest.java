package br.senac.tads.recursos.seguranca;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class AdministradorInicialTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioService service = mock(UsuarioService.class);

    @Test
    void criaAdministradorQuandoNaoExiste() {
        when(usuarios.existsByEmailIgnoreCase("adm@escola.com")).thenReturn(false);

        new AdministradorInicial(usuarios, service, "adm@escola.com", "senha123").run(null);

        verify(service).criar("Administrador", "adm@escola.com", "senha123", Papel.ADMINISTRADOR);
    }

    @Test
    void naoDuplicaAdministradorExistente() {
        when(usuarios.existsByEmailIgnoreCase("adm@escola.com")).thenReturn(true);

        new AdministradorInicial(usuarios, service, "adm@escola.com", "senha123").run(null);

        verify(service, never()).criar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void naoFazNadaSemConfiguracao() {
        new AdministradorInicial(usuarios, service, "", "").run(null);
        new AdministradorInicial(usuarios, service, "adm@escola.com", "").run(null);

        verify(service, never()).criar(anyString(), anyString(), anyString(), any());
    }
}
