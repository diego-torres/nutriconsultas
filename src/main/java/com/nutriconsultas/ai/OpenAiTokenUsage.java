package com.nutriconsultas.ai;

/**
 * Token usage from an OpenAI completion (#366).
 */
public record OpenAiTokenUsage(int promptTokens, int completionTokens, int totalTokens) {

}
