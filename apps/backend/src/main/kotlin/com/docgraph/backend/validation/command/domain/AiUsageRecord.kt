package com.docgraph.backend.validation.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * AI 호출 1회의 사용량 관측 레코드. 성공한 검출 1건당 1행.
 * projectId는 포착 시점 귀속을 고정하기 위한 비정규화 값 — edge 삭제 후에도 이력이 보존된다.
 */
@Entity
@Table(name = "ai_usage_record")
class AiUsageRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "validation_task_id", nullable = false, unique = true)
    val validationTaskId: Long,

    @Column(name = "project_id", nullable = false)
    val projectId: Long,

    @Column(nullable = false, length = MODEL_MAX_LENGTH)
    val model: String,

    @Column(name = "prompt_tokens", nullable = false)
    val promptTokens: Int,

    @Column(name = "completion_tokens", nullable = false)
    val completionTokens: Int,

    @Column(name = "total_tokens", nullable = false)
    val totalTokens: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {
        const val MODEL_MAX_LENGTH = 100
    }
}
