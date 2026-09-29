package com.thalia.fisioterapia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;

@SpringBootTest(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/fisioterapia_test"
})
class FisioterapiaApplicationTests {

	@MockBean
	private UsuarioRepository usuarioRepository;

	@Test
	void contextLoads() {
	}

}
