package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures vendored Markdown assets for #434 are present on the classpath.
 */
class AiMarkdownStaticAssetsTest {

	@Test
	void markdownVendorAndWrapperAssetsExist() {
		assertThat(new ClassPathResource("static/sbadmin/vendor/marked/marked.min.js").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/vendor/dompurify/purify.min.js").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/js/ai-markdown.js").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/js/ai-chat-stream.js").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/js/ai-chat-errors.js").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/css/ai-markdown.css").exists()).isTrue();
	}

}
