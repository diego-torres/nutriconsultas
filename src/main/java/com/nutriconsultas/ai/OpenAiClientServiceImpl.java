package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OpenAiClientServiceImpl implements OpenAiClientService {

	private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

	private final AiProperties properties;

	private final RestClient restClient;

	public OpenAiClientServiceImpl(final AiProperties properties,
			@Qualifier("openAiRestClient") final RestClient openAiRestClient) {
		this.properties = properties;
		this.restClient = openAiRestClient;
	}

	@Override
	public boolean isAvailable() {
		return properties.isOperational();
	}

	@Override
	public OpenAiChatCompletionResponse chatCompletion(final OpenAiChatCompletionRequest request) {
		assertOperational();
		final OpenAiApiRequest apiRequest = toApiRequest(request);
		try {
			final OpenAiApiResponse response = restClient.post()
				.uri(CHAT_COMPLETIONS_PATH)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOpenai().getApiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(apiRequest)
				.retrieve()
				.body(OpenAiApiResponse.class);
			return toCompletionResponse(response);
		}
		catch (final RestClientResponseException ex) {
			if (log.isWarnEnabled()) {
				log.warn("OpenAI chat completion failed with status={}", ex.getStatusCode().value());
			}
			throw OpenAiClientErrorMapper.mapResponseException(ex);
		}
		catch (final ResourceAccessException ex) {
			if (log.isWarnEnabled()) {
				log.warn("OpenAI chat completion timed out or connection failed");
			}
			throw OpenAiClientErrorMapper.timeout(ex);
		}
	}

	private void assertOperational() {
		if (!properties.isOperational()) {
			throw OpenAiClientErrorMapper.notConfigured();
		}
	}

	private OpenAiApiRequest toApiRequest(final OpenAiChatCompletionRequest request) {
		final List<OpenAiApiMessage> messages = new ArrayList<>();
		for (final OpenAiChatMessage message : request.messages()) {
			List<OpenAiApiToolCall> apiToolCalls = null;
			if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
				apiToolCalls = new ArrayList<>();
				for (final OpenAiToolCall toolCall : message.toolCalls()) {
					apiToolCalls.add(new OpenAiApiToolCall(toolCall.id(), "function",
							new OpenAiApiFunctionCall(toolCall.name(), toolCall.argumentsJson())));
				}
			}
			messages.add(new OpenAiApiMessage(message.role(), message.content(), message.toolCallId(), message.name(),
					apiToolCalls));
		}
		final List<OpenAiApiTool> tools = new ArrayList<>();
		for (final OpenAiToolDefinition tool : request.tools()) {
			final Map<String, Object> function = new HashMap<>();
			function.put("name", tool.name());
			function.put("description", tool.description());
			function.put("parameters", tool.parameters());
			tools.add(new OpenAiApiTool("function", function));
		}
		return new OpenAiApiRequest(properties.getOpenai().getModel(), messages, tools.isEmpty() ? null : tools,
				properties.getOpenai().isStore(), request.parameters().temperature(),
				request.parameters().maxTokens(), responseFormat(request.parameters().responseFormatType()));
	}

	private static Map<String, String> responseFormat(final String type) {
		if (!org.springframework.util.StringUtils.hasText(type)) {
			return null;
		}
		return Map.of("type", type);
	}

	private OpenAiChatCompletionResponse toCompletionResponse(final OpenAiApiResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()
				|| response.choices().get(0).message() == null) {
			throw incompleteResponse();
		}
		final OpenAiApiResponse.Choice choice = response.choices().get(0);
		final OpenAiApiResponse.Message message = choice.message();
		final List<OpenAiToolCall> toolCalls = new ArrayList<>();
		if (message.toolCalls() != null) {
			for (final OpenAiApiResponse.ToolCall toolCall : message.toolCalls()) {
				if (toolCall.function() != null) {
					toolCalls.add(new OpenAiToolCall(toolCall.id(), toolCall.function().name(),
							toolCall.function().arguments()));
				}
			}
		}
		OpenAiTokenUsage usage = null;
		if (response.usage() != null) {
			usage = new OpenAiTokenUsage(response.usage().promptTokens(), response.usage().completionTokens(),
					response.usage().totalTokens());
		}
		return new OpenAiChatCompletionResponse(response.id(), message.role(), message.content(), toolCalls,
				choice.finishReason(), usage);
	}

	private OpenAiClientException incompleteResponse() {
		return new OpenAiClientException(OpenAiClientException.ErrorKind.UNKNOWN,
				org.springframework.http.HttpStatus.BAD_GATEWAY, "El servicio de IA devolvió una respuesta incompleta.",
				"OpenAI response missing choices", null);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record OpenAiApiRequest(String model, List<OpenAiApiMessage> messages, List<OpenAiApiTool> tools,
			boolean store, Double temperature, @JsonProperty("max_tokens") Integer maxTokens,
			@JsonProperty("response_format") Map<String, String> responseFormat) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record OpenAiApiMessage(String role, String content, @JsonProperty("tool_call_id") String toolCallId,
			String name, @JsonProperty("tool_calls") List<OpenAiApiToolCall> toolCalls) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record OpenAiApiToolCall(String id, String type, @JsonProperty("function") OpenAiApiFunctionCall function) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record OpenAiApiFunctionCall(String name, String arguments) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record OpenAiApiTool(String type, Map<String, Object> function) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OpenAiApiResponse(String id, List<Choice> choices, Usage usage) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		private record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		private record Message(String role, String content, @JsonProperty("tool_calls") List<ToolCall> toolCalls) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		private record ToolCall(String id, String type, FunctionCall function) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		private record FunctionCall(String name, String arguments) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		private record Usage(@JsonProperty("prompt_tokens") int promptTokens,
				@JsonProperty("completion_tokens") int completionTokens,
				@JsonProperty("total_tokens") int totalTokens) {
		}

	}

}
