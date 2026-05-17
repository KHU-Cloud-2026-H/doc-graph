package com.docgraph.backend.workspace.command.interfaces.event

import com.docgraph.backend.auth.command.domain.NotionWorkspaceAuthorizedEvent
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommand
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommandHandler
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NotionWorkspaceAuthorizedEventListener(
    private val registerHandler: RegisterWorkspaceCommandHandler,
) {
    @EventListener
    fun on(event: NotionWorkspaceAuthorizedEvent) {
        registerHandler.handle(
            RegisterWorkspaceCommand(
                ownerUserId = event.ownerUserId,
                notionWorkspaceId = event.notionWorkspaceId,
                notionWorkspaceName = event.notionWorkspaceName,
                notionBotId = event.notionBotId,
            ),
        )
    }
}