package com.nutriconsultas.ai;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiGeneratedDraftRepository extends JpaRepository<AiGeneratedDraft, Long> {

	List<AiGeneratedDraft> findByThreadIdOrderByCreatedAtDescIdDesc(Long threadId);

	@Query("SELECT d FROM AiGeneratedDraft d JOIN d.thread t WHERE d.id = :draftId AND t.nutritionistId = :nutritionistId")
	Optional<AiGeneratedDraft> findByIdAndThreadNutritionistId(@Param("draftId") Long draftId,
			@Param("nutritionistId") String nutritionistId);

	List<AiGeneratedDraft> findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(Long threadId, AiDraftStatus status);

	List<AiGeneratedDraft> findByThreadIdAndStatusAndCreatedAtGreaterThanEqual(Long threadId, AiDraftStatus status,
			Instant createdAt);

}
