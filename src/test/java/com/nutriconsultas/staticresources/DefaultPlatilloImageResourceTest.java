package com.nutriconsultas.staticresources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DefaultPlatilloImageResourceTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void defaultPlatilloImageExistsInStaticResources() throws Exception {
		assertThat(new ClassPathResource("static/sbadmin/img/plato-vacio.jpg").exists()).isTrue();
	}

	@Test
	void servesDefaultPlatilloImageAtCanonicalPath() throws Exception {
		mockMvc.perform(get("/sbadmin/img/plato-vacio.jpg"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
	}

}
