package com.docgraph.backend.notification.command.domain

class NotificationPermissionDeniedException(projectId: Long) :
    RuntimeException("호출자가 프로젝트 $projectId 의 Project Admin이 아님")
