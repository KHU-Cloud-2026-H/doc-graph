package com.docgraph.backend.validation.command.domain

class ValidationPermissionDeniedException(projectId: Long) :
    RuntimeException("호출자가 프로젝트 $projectId 의 Project Admin이 아님")
