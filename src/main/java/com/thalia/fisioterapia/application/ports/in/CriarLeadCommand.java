package com.thalia.fisioterapia.application.ports.in;

public record CriarLeadCommand(
        String nome,
        String sobrenome,
        String email,
        String celular
) {}

