package com.thalia.fisioterapia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;

@SpringBootTest(properties = {
		"spring.mongodb.uri=mongodb://localhost:27017/fisioterapia_test",
		// Sem Mongo de verdade no teste: não cria índices e não usa transação
		"spring.data.mongodb.auto-index-creation=false",
		"app.mongo.transactions=false",
		// Com senha definida o AdminBootstrap não grava admin-credentials.txt (sobrescrevia o arquivo real)
		"auth.admin.password=senha-de-teste-123"
})
class FisioterapiaApplicationTests {

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void contextLoads() {
	}

}
