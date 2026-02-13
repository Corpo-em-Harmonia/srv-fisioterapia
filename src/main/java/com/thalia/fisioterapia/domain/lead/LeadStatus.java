package com.thalia.fisioterapia.domain.lead;

public enum LeadStatus {


    NOVO,        // formulário chegou
    CONTATADO,   // recepção falou com paciente
    AGENDADO,
    CONVERTIDO,  // virou paciente (compareceu)
    PERDIDO,     // desistiu // fisio terminou
}
