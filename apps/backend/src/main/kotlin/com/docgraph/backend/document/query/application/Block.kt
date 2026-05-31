package com.docgraph.backend.document.query.application

data class Block(
    val blockId: String,
    val parentBlockId: String?,
    val type: String,
    val text: String?,
    val previousText: String?,
    val order: Int,
)
