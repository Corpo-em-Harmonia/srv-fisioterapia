package com.thalia.fisioterapia.domain.sessao;

import lombok.Getter;

import java.time.Instant;

@Getter
public class SessaoAlteracao {

    private final SessaoAuditoriaAcao acao;
    private final String escopo;
    private final String motivo;
    private final String usuarioId;
    private final PerfilUsuario perfil;
    private final Instant criadoEm;

    public SessaoAlteracao(
            SessaoAuditoriaAcao acao,
            String escopo,
            String motivo,
            String usuarioId,
            PerfilUsuario perfil,
            Instant criadoEm
    ) {
        this.acao = acao;
        this.escopo = escopo;
        this.motivo = motivo;
        this.usuarioId = usuarioId;
        this.perfil = perfil;
        this.criadoEm = criadoEm;
    }
}
