package com.docgraph.backend.document.query.interfaces.api

import com.docgraph.backend.common.PageResponse
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Document")
class DocumentQueryController {

    @GetMapping("/projects/{id}/documents")
    @Operation(summary = "문서 목록")
    fun list(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<DocumentSummary>> {
        TODO()
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "문서 상세")
    fun get(@PathVariable id: Long): ResponseEntity<DocumentDetail> {
        TODO()
    }
}
