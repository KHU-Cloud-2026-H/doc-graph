package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.document.query.application.Block

sealed interface ConflictDetectionInput {
    val targetBlocks: List<Block>
    val criterion: String
}

/** 엣지 첫 검증(초기 동기화·제안 수락·수동 엣지): source 전체와 target 전체의 현 정합성. */
data class FirstValidationInput(
    val sourceBlocks: List<Block>,
    override val targetBlocks: List<Block>,
    override val criterion: String,
) : ConflictDetectionInput

/** 재검증(기존 엣지의 source 변경): 변경 전·후 비교로 변화가 target에 반영됐는지. */
data class RevalidationInput(
    val sourceBeforeBlocks: List<Block>,
    val sourceAfterBlocks: List<Block>,
    override val targetBlocks: List<Block>,
    override val criterion: String,
) : ConflictDetectionInput
