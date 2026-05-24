package com.docgraph.backend.auth.query.application

import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!acceptance")
class FindUserIdByEmailQueryHandler(
    private val repository: AuthQueryRepository,
) : FindUserIdByEmailQuery {
    override fun find(email: String): Long? =
        repository.findUserIdByEmail(email)
}
