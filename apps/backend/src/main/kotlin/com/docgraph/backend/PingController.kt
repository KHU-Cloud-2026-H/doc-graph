package com.docgraph.backend

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Hidden
class PingController {

    @GetMapping("/ping")
    fun ping(): String = "pong"
}