package com.nutriconsultas.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

	List<AiChatMessage> findByThreadIdOrderByCreatedAtAscIdAsc(Long threadId);

	@Query("SELECT m FROM AiChatMessage m JOIN m.thread t WHERE m.id = :messageId AND t.nutritionistId = :nutritionistId")
	Optional<AiChatMessage> findByIdAndThreadNutritionistId(@Param("messageId") Long messageId,
			@Param("nutritionistId") String nutritionistId);

	@Modifying
	@Query("DELETE FROM AiChatMessage m WHERE m.thread.id = :threadId AND m.id >= :fromMessageId")
	int deleteByThreadIdAndIdGreaterThanEqual(@Param("threadId") Long threadId,
			@Param("fromMessageId") Long fromMessageId);

}
