package com.thalia.fisioterapia.web.dto.auth;

import java.util.List;

public record LoginResponse(String token, String role, List<String> roles, String nome) {
	public LoginResponse(String token, String role, String nome) {
		this(token, role, List.of(role), nome);
	}
}
