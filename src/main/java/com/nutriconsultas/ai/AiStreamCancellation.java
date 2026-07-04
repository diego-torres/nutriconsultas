package com.nutriconsultas.ai;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks client-initiated cancellation for AI chat SSE streams (#436).
 */
public final class AiStreamCancellation {

	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	public void cancel() {
		cancelled.set(true);
	}

	public boolean isCancelled() {
		return cancelled.get();
	}

	public void throwIfCancelled() {
		if (cancelled.get()) {
			throw new AiStreamCancelledException();
		}
	}

}
