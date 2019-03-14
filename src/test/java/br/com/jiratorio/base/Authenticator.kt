package br.com.jiratorio.base

import br.com.jiratorio.factory.entity.AccountFactory
import io.restassured.http.Header
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Authenticator(val accountFactory: AccountFactory) {

    fun <T> withDefaultUser(supplier: () -> T?) =
            this.withUser(accountFactory.defaultUserName(), supplier)

    fun <T> withUser(username: String, supplier: () -> T?): T? {
        val oldContext = TestSecurityContextHolder.getContext()

        TestSecurityContextHolder.clearContext()
        val principal = accountFactory.buildUser(username)
        TestSecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken(principal,
                principal.password, principal.authorities))

        val result = supplier()

        TestSecurityContextHolder.clearContext()
        TestSecurityContextHolder.setContext(oldContext)

        return result
    }

    fun defaultUserHeader() =
            Header("X-Auth-Token", accountFactory.defaultUserToken())

    fun defaultUserName() = accountFactory.defaultUserName()
}
