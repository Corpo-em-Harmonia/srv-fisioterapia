package com.thalia.fisioterapia.web.dto.auth;

public record LoginResponse(String token, String role, String nome) {}
