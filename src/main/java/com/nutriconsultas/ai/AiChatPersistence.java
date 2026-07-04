package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared chat persistence collaborators for {@link AiOrchestrationServiceImpl}.
 */
@Component
public class AiChatPersistence {

	private final AiChatThreadRepository threads;

	private final AiChatMessageRepository messages;

	private final TransactionTemplate transactions;

	public AiChatPersistence(final AiChatThreadRepository threads, final AiChatMessageRepository messages,
			final TransactionTemplate transactions) {
		this.threads = threads;
		this.messages = messages;
		this.transactions = transactions;
	}

	public AiChatThreadRepository getThreadRepository() {
		return threads;
	}

	public AiChatMessageRepository getMessageRepository() {
		return messages;
	}

	public TransactionTemplate getTransactionTemplate() {
		return transactions;
	}

}
