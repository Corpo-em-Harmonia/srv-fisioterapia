package com.thalia.fisioterapia.domain.lead;

public enum LeadAcao {

    REGISTRAR_CONTATO,        // ligou/mandou whatsapp
    AGENDAR_AVALIACAO,        // marcou horário
    CONFIRMAR_COMPARECIMENTO, // paciente veio
    FALTOU,                   // não veio
    CANCELAR                  // desistiu // fisio terminou
}
