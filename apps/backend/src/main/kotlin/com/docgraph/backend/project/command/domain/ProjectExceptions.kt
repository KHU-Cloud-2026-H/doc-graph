package com.docgraph.backend.project.command.domain

class ProjectNotFoundException(val projectId: Long) :
    RuntimeException("project not found: $projectId")

class ProjectWorkspaceNotFoundException(val workspaceId: Long) :
    RuntimeException("workspace not found: $workspaceId")

class ProjectPermissionDeniedException(val workspaceId: Long, val userId: Long) :
    RuntimeException("user $userId has no permission on workspace $workspaceId")

class ProjectDuplicateException(val workspaceId: Long, val notionRootPageId: String) :
    RuntimeException("project for root page $notionRootPageId already exists in workspace $workspaceId")

class ProjectMemberNotFoundException(val projectId: Long, val memberId: Long) :
    RuntimeException("project_member $memberId not in project $projectId")

class ProjectMemberDuplicateException(val projectId: Long, val workspaceMemberId: Long) :
    RuntimeException("workspace_member $workspaceMemberId already in project $projectId")

class ProjectSelfMemberModificationException(val projectId: Long, val memberId: Long) :
    RuntimeException("member $memberId cannot modify own role/membership in project $projectId")

class ProjectWorkspaceMemberNotFoundException(val workspaceId: Long, val workspaceMemberId: Long) :
    RuntimeException("workspace_member $workspaceMemberId not in workspace $workspaceId")

class CategoryNotFoundException(val projectId: Long, val categoryId: Long) :
    RuntimeException("category $categoryId not in project $projectId")

class CategoryDuplicateException(val projectId: Long, val notionPageId: String) :
    RuntimeException("category for page $notionPageId already exists in project $projectId")
