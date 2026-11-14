
package br.com.stormaid;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.listener.direct.auto-startup=false",
		"spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration"
})
@AutoConfigureMockMvc
class StormaidApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OpenAiEmbeddingModel openAiEmbeddingModel;

	@MockBean
	private OpenAiChatModel openAiChatModel;

	@MockBean
	private VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldRejectUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/v1/dependentes"))
				.andExpect(status().isForbidden());
	}
}