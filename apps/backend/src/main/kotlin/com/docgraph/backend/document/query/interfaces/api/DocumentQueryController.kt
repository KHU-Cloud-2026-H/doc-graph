package com.docgraph.backend.document.query.interfaces.api

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.document.query.application.SearchDocumentSummariesByProjectQuery
import com.docgraph.backend.web.PageResponse
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Document")
class DocumentQueryController(
    private val findDocumentByIdQuery: FindDocumentByIdQuery,
    private val searchDocumentSummariesByProjectQuery: SearchDocumentSummariesByProjectQuery,
) {

    @GetMapping("/projects/{id}/documents")
    @Operation(summary = "문서 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "프로젝트 문서 목록 page"),
    ])
    fun list(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<DocumentSummary>> {
        val result = searchDocumentSummariesByProjectQuery.search(id, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "문서 상세")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "문서 상세"),
        ApiResponse(responseCode = "404", description = "문서 없음"),
    ])
    fun get(@PathVariable id: Long): ResponseEntity<DocumentDetail> {
        val detail = findDocumentByIdQuery.find(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }
}
