package com.docgraph.backend.workspace.command.domain

class WorkspaceNotFoundException(val workspaceId: Long) :
    RuntimeException("workspace not found: $workspaceId")

class WorkspacePermissionDeniedException(val workspaceId: Long, val userId: Long) :
    RuntimeException("member $userId has no permission on workspace $workspaceId")

class WorkspaceMemberDuplicateException(val workspaceId: Long, val userId: Long) :
    RuntimeException("member $userId already in workspace $workspaceId")

class WorkspaceMemberNotFoundException(val workspaceId: Long, val memberId: Long) :
    RuntimeException("member $memberId not in workspace $workspaceId")

class WorkspaceInviteeNotFoundException(val email: String) :
    RuntimeException("invitee email not registered: $email")

class WorkspaceSelfInviteException(val workspaceId: Long) :
    RuntimeException("cannot invite workspace creator: $workspaceId")

class WorkspaceCreatorRemovalException(val workspaceId: Long) :
    RuntimeException("workspace creator cannot be removed: $workspaceId")
