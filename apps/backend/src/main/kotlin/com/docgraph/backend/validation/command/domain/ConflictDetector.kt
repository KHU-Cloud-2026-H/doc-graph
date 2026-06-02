package com.docgraph.backend.validation.command.domain

fun interface ConflictDetector {
    fun detect(input: ConflictDetectionInput): ConflictDetectionResult
}