package com.docgraph.backend.validation.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.validation.command.application.ConfigureProjectValidationCommand
import com.docgraph.backend.validation.command.application.ConfigureProjectValidationCommandHandler
import com.docgraph.backend.validation.command.domain.ValidationPermissionDeniedException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects")
@Tag(name = "Validation")
class ProjectValidationCommandController(
    private val configureHandler: ConfigureProjectValidationCommandHandler,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PutMapping("/{id}/validation")
    @Operation(
        summary = "프로젝트 정합성 검증 ON/OFF 설정",
        description = "OFF면 해당 프로젝트의 변경에 대해 AI 검증 작업을 생성하지 않는다(이미 대기 중인 작업·기존 충돌은 영향받지 않음). 기본값은 ON.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "설정 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun updateValidation(
        @PathVariable id: Long,
        @RequestBody request: UpdateProjectValidationRequest,
    ): ResponseEntity<Unit> {
        configureHandler.handle(
            ConfigureProjectValidationCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                enabled = request.enabled,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ValidationPermissionDeniedException::class)
    fun handleForbidden(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
}

data class UpdateProjectValidationRequest(
    @Schema(description = "정합성 검증 활성 여부 — false면 AI 검증 작업 생성 중단", example = "false")
    val enabled: Boolean,
)
