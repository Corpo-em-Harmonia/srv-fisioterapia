package com.thalia.fisioterapia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/fisioterapia_test"
})
class FisioterapiaApplicationTests {

	@Test
	void contextLoads() {
	}

}
