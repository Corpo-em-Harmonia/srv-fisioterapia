package com.thalia.fisioterapia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;

@SpringBootTest(properties = {
		"spring.mongodb.uri=mongodb://localhost:27017/fisioterapia_test"
})
class FisioterapiaApplicationTests {

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@Test
	void contextLoads() {
	}

}
