package com.docgraph.backend.validation.command.domain

class ConflictNotFoundException(val conflictId: Long) :
    RuntimeException("conflict not found: $conflictId")

class IllegalConflictStateException(val conflictId: Long, reason: String) :
    RuntimeException("conflict $conflictId: $reason")

class ConflictFindingNotFoundException(val conflictFindingId: Long) :
    RuntimeException("conflict finding not found: $conflictFindingId")

class IllegalConflictFindingStateException(val conflictFindingId: Long, reason: String) :
    RuntimeException("conflict finding $conflictFindingId: $reason")

class StaleProposalException(val conflictFindingId: Long) :
    RuntimeException("conflict finding $conflictFindingId: target document modified since proposal review")