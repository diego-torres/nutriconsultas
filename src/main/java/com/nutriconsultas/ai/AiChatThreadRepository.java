package com.nutriconsultas.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatThreadRepository extends JpaRepository<AiChatThread, Long> {

	Optional<AiChatThread> findByIdAndNutritionistId(Long id, String nutritionistId);

	List<AiChatThread> findByNutritionistIdOrderByUpdatedAtDesc(String nutritionistId);

}
