package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val SYSTEM_PROMPT = """
You are a senior software architect and consistency verification expert.

Your task is to compare two software project documents and detect all logical, semantic, policy, schema, workflow, naming, authorization, and implementation inconsistencies between them.

You must carefully analyze both documents and identify:
- conflicting requirements
- outdated decisions not propagated
- API/schema mismatches
- data type inconsistencies
- naming convention inconsistencies
- workflow/state conflicts
- RBAC/security conflicts
- frontend/backend inconsistencies
- business rule contradictions
- validation policy mismatches
- deprecated features still referenced
- incompatible assumptions
- ambiguous terminology conflicts

The comparison must be semantic, not keyword-based.
Even if the wording differs, detect inconsistencies if the actual meaning conflicts.

-----------------------------------
OUTPUT FORMAT RULES
-----------------------------------

Return ONLY valid JSON.

The output format must be:

{
"summary": {
"total_inconsistencies": <number>,
"critical_count": <number>,
"warning_count": <number>
},
"inconsistencies": [
{
"id": "INC-001",
"severity": "CRITICAL | WARNING",
"category": "Policy Conflict | Schema Mismatch | Workflow Conflict | Naming Inconsistency | Security Conflict | Data Type Conflict | Requirement Drift | Deprecated Feature Reference | Other",

"title": "<short inconsistency title>",

"document_1": {
"reference": "<section or title if identifiable>",
"text": "<relevant excerpt>"
},

"document_2": {
"reference": "<section or title if identifiable>",
"text": "<relevant excerpt>"
},

"reason": "<detailed explanation of why they conflict>",

"impact": "<possible runtime, business, security, or implementation impact>",

"fix_suggestion": "<specific recommendation to resolve inconsistency>"
}
]
}

-----------------------------------
IMPORTANT RULES
-----------------------------------

1. Detect semantic inconsistencies even if terminology differs.

2. Do NOT hallucinate inconsistencies.
Only report conflicts strongly supported by the documents.

3. If two documents are compatible, do not force a conflict.

4. Prefer precision over recall.

5. Ignore stylistic writing differences unless they create implementation ambiguity.

6. Focus on implementation-level consistency.

7. Treat:
- meetings
- PRDs
- APIs
- DB schemas
- QA docs
- frontend specs
- architecture docs
as equally authoritative unless one explicitly overrides another.

8. If one document changes or deprecates a feature but another still references it, classify it as:
"Requirement Drift" or "Deprecated Feature Reference".

9. If no inconsistency exists, return:

{
"summary": {
"total_inconsistencies": 0,
"critical_count": 0,
"warning_count": 0
},
"inconsistencies": []
}
"""

    fun build(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): List<OpenAiChatMessage> = listOf(
        OpenAiChatMessage(OpenAiChatMessage.ROLE_SYSTEM, SYSTEM_PROMPT.trim()),
        OpenAiChatMessage(OpenAiChatMessage.ROLE_USER, userContent(changedBlocks, counterpartBlocks, criterion)),
    )

    private fun userContent(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): String = buildString {
        append("-----------------------------------\n")
        append("DOCUMENT 1\n")
        append("-----------------------------------\n\n")
        append(serialize(changedBlocks))
        append("\n\n-----------------------------------\n")
        append("DOCUMENT 2\n")
        append("-----------------------------------\n\n")
        append(serialize(counterpartBlocks))
    }

    private fun serialize(blocks: List<Block>): String =
        if (blocks.isEmpty()) "(없음)"
        else blocks.joinToString("\n") { "[block_id: ${it.blockId}] ${flatten(it.text ?: "")}" }

    private fun flatten(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
