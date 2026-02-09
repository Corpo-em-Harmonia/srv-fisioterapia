package com.thalia.fisioterapia.domain.lead;

public enum LeadStatus {


    NOVO,        // formulário chegou
    CONTATADO,   // recepção falou com paciente
    AGENDADO,    // avaliação marcada
    CONVERTIDO,  // virou paciente (compareceu)
    PERDIDO
}
