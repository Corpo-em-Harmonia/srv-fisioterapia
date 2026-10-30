package com.thalia.fisioterapia.security;

/**
 * Expressões de @PreAuthorize reaproveitadas pelos controllers. Antes só /api/usuarios tinha
 * regra por perfil — qualquer usuário logado (inclusive PACIENTE) acessava sessões, avaliações
 * e leads de todo mundo, que são dados de saúde (sensíveis pela LGPD).
 */
public final class Perfis {

    /** Toda a equipe da clínica. */
    public static final String EQUIPE = "hasAnyRole('ADMIN','RECEPCIONISTA','FISIOTERAPEUTA')";

    /** Recepção: leads, agenda e cadastro. */
    public static final String RECEPCAO = "hasAnyRole('ADMIN','RECEPCIONISTA')";

    /** Área clínica: prontuário, avaliação e evolução — só quem atende. */
    public static final String CLINICO = "hasAnyRole('ADMIN','FISIOTERAPEUTA')";

    /** Equipe ou paciente; o service limita o paciente aos próprios dados. */
    public static final String EQUIPE_OU_PACIENTE = "hasAnyRole('ADMIN','RECEPCIONISTA','FISIOTERAPEUTA','PACIENTE')";

    private Perfis() {}
}
