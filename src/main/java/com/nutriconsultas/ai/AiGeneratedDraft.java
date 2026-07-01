package com.nutriconsultas.ai;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "ai_generated_draft")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "thread")
@ToString(exclude = "thread")
public class AiGeneratedDraft {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "thread_id", nullable = false)
	@JsonBackReference
	private AiChatThread thread;

	@Enumerated(EnumType.STRING)
	@Column(name = "draft_type", nullable = false, length = 20)
	private AiDraftType draftType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AiDraftStatus status = AiDraftStatus.DRAFT;

	@Lob
	@Column(name = "json_payload", nullable = false)
	private String jsonPayload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (status == null) {
			status = AiDraftStatus.DRAFT;
		}
	}

}
