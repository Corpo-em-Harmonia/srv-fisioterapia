package com.thalia.fisioterapia.adapters.in.dto;


public record CriarLeadRequestDTO(
        String nome,
        String sobrenome,
        String email,
        String celular
) {}
