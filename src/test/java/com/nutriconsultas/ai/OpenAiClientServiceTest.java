package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiClientServiceTest {

	private AiProperties properties;

	private MockRestServiceServer mockServer;

	private OpenAiClientService service;

	@BeforeEach
	void setUp() {
		properties = new AiProperties();
		properties.setEnabled(true);
		properties.getOpenai().setApiKey("sk-test-key");
		properties.getOpenai().setModel("gpt-test");
		properties.getOpenai().setBaseUrl("https://api.openai.com");
		final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(properties.getOpenai().getBaseUrl());
		mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		service = new OpenAiClientServiceImpl(properties, restClientBuilder.build(), new AiAuditLogger());
	}

	@Test
	void isAvailableWhenOperational() {
		assertThat(service.isAvailable()).isTrue();
	}

	@Test
	void chatCompletionReturnsAssistantMessage() {
		final String responseJson = """
				{
				  "id": "chatcmpl-test",
				  "choices": [{
				    "message": {
				      "role": "assistant",
				      "content": "Hola, ¿en qué puedo ayudarte?"
				    },
				    "finish_reason": "stop"
				  }],
				  "usage": {
				    "prompt_tokens": 10,
				    "completion_tokens": 8,
				    "total_tokens": 18
				  }
				}
				""";
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer sk-test-key"))
			.andExpect(content().json("""
					{
					  "model": "gpt-test",
					  "messages": [
					    {"role":"system","content":"Eres un asistente."},
					    {"role":"user","content":"Hola"}
					  ],
					  "store": false
					}
					""", false))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		final OpenAiChatCompletionResponse response = service.chatCompletion(new OpenAiChatCompletionRequest(
				List.of(OpenAiChatMessage.system("Eres un asistente."), OpenAiChatMessage.user("Hola")), List.of()));

		assertThat(response.id()).isEqualTo("chatcmpl-test");
		assertThat(response.content()).isEqualTo("Hola, ¿en qué puedo ayudarte?");
		assertThat(response.finishReason()).isEqualTo("stop");
		assertThat(response.usage().totalTokens()).isEqualTo(18);
		mockServer.verify();
	}

	@Test
	void chatCompletionSerializesAssistantToolCallsInRequest() {
		final String responseJson = """
				{
				  "id": "chatcmpl-echo",
				  "choices": [{
				    "message": {
				      "role": "assistant",
				      "content": "Listo."
				    },
				    "finish_reason": "stop"
				  }]
				}
				""";
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().json("""
					{
					  "model": "gpt-test",
					  "messages": [
					    {
					      "role": "assistant",
					      "tool_calls": [{
					        "id": "call_abc",
					        "type": "function",
					        "function": {
					          "name": "search_food_catalog",
					          "arguments": "{\\"query\\":\\"avena\\"}"
					        }
					      }]
					    }
					  ],
					  "store": false
					}
					""", false))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		service.chatCompletion(new OpenAiChatCompletionRequest(
				List.of(OpenAiChatMessage.assistantWithToolCalls(null,
						List.of(new OpenAiToolCall("call_abc", "search_food_catalog", "{\"query\":\"avena\"}")))),
				List.of()));

		mockServer.verify();
	}

	@Test
	void chatCompletionReturnsToolCalls() {
		final String responseJson = """
				{
				  "id": "chatcmpl-tools",
				  "choices": [{
				    "message": {
				      "role": "assistant",
				      "content": null,
				      "tool_calls": [{
				        "id": "call_1",
				        "type": "function",
				        "function": {
				          "name": "search_food_catalog",
				          "arguments": "{\\"query\\":\\"avena\\"}"
				        }
				      }]
				    },
				    "finish_reason": "tool_calls"
				  }]
				}
				""";
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		final OpenAiChatCompletionResponse response = service.chatCompletion(new OpenAiChatCompletionRequest(
				List.of(OpenAiChatMessage.user("Busca avena")), List.of(new OpenAiToolDefinition("search_food_catalog",
						"Busca alimentos", Map.of("type", "object", "properties", Map.of())))));

		assertThat(response.hasToolCalls()).isTrue();
		assertThat(response.toolCalls()).hasSize(1);
		assertThat(response.toolCalls().get(0).name()).isEqualTo("search_food_catalog");
		mockServer.verify();
	}

	@Test
	void chatCompletionWhenNotConfiguredThrows() {
		properties.setEnabled(false);
		assertThatThrownBy(() -> service
			.chatCompletion(new OpenAiChatCompletionRequest(List.of(OpenAiChatMessage.user("Hola")), List.of())))
			.isInstanceOf(OpenAiClientException.class)
			.extracting(ex -> ((OpenAiClientException) ex).getKind())
			.isEqualTo(OpenAiClientException.ErrorKind.NOT_CONFIGURED);
	}

	@Test
	void chatCompletionMapsAuthFailure() {
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("""
					{"error":{"message":"Invalid API key","type":"invalid_request_error"}}
					"""));

		assertThatThrownBy(() -> service
			.chatCompletion(new OpenAiChatCompletionRequest(List.of(OpenAiChatMessage.user("Hola")), List.of())))
			.isInstanceOf(OpenAiClientException.class)
			.satisfies(ex -> {
				final OpenAiClientException openAiEx = (OpenAiClientException) ex;
				assertThat(openAiEx.getKind()).isEqualTo(OpenAiClientException.ErrorKind.AUTH);
				assertThat(openAiEx.getUserMessage()).contains("autenticar");
			});
	}

	@Test
	void chatCompletionMapsRateLimit() {
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
			.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("""
					{"error":{"message":"Rate limit exceeded","type":"rate_limit_error"}}
					"""));

		assertThatThrownBy(() -> service
			.chatCompletion(new OpenAiChatCompletionRequest(List.of(OpenAiChatMessage.user("Hola")), List.of())))
			.isInstanceOf(OpenAiClientException.class)
			.extracting(ex -> ((OpenAiClientException) ex).getKind())
			.isEqualTo(OpenAiClientException.ErrorKind.RATE_LIMIT);
	}

	@Test
	void chatCompletionSerializesOptionalParameters() {
		mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions")).andExpect(content().json("""
				{
				  "model": "gpt-test",
				  "messages": [{"role":"user","content":"Clasifica"}],
				  "store": false,
				  "temperature": 0.0,
				  "max_tokens": 200,
				  "response_format": {"type":"json_object"}
				}
				""", false)).andRespond(withSuccess("""
				{
				  "id": "chatcmpl-json",
				  "choices": [{
				    "message": {"role":"assistant","content":"{\\"decision\\":\\"ALLOW\\"}"},
				    "finish_reason": "stop"
				  }]
				}
				""", MediaType.APPLICATION_JSON));

		service.chatCompletion(new OpenAiChatCompletionRequest(List.of(OpenAiChatMessage.user("Clasifica")), List.of(),
				OpenAiCompletionParameters.scopeClassifier(200)));

		mockServer.verify();
	}

}
