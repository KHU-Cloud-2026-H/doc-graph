package com.docgraph.backend.validation.command.domain

class ConflictNotFoundException(val conflictId: Long) :
    RuntimeException("conflict not found: $conflictId")

class IllegalConflictStateException(val conflictId: Long, reason: String) :
    RuntimeException("conflict $conflictId: $reason")