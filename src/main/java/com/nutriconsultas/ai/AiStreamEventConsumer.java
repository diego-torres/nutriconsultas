package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Callbacks for streaming AI orchestration events to nutritionist clients (#435).
 */
public interface AiStreamEventConsumer {

	default boolean isCancelled() {
		return false;
	}

	default void throwIfCancelled() {
		if (isCancelled()) {
			throw new AiStreamCancelledException();
		}
	}

	void onStatus(String phase, @Nullable String message);

	void onDelta(String contentDelta);

	void onComplete(AiOrchestrationResult result);

}
