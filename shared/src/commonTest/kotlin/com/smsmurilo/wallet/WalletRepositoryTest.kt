package com.smsmurilo.wallet

import com.smsmurilo.wallet.data.WalletApi
import com.smsmurilo.wallet.data.WalletRepository
import com.smsmurilo.wallet.model.Category
import com.smsmurilo.wallet.model.Expense
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WalletRepositoryTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(rejectPosts: Boolean = false) = WalletApi.create("https://api.test/", MockEngine { req ->
        when {
            req.method == HttpMethod.Get && req.url.encodedPath.endsWith("expenses") -> respond(
                """[{"id":"a","description":"Farmácia","amountCents":3990,"category":"HEALTH","date":"2026-03-01"},
                    {"id":"b","description":"Ônibus","amountCents":450,"category":"TRANSPORT","date":"2026-03-05"}]""",
                headers = json,
            )
            req.method == HttpMethod.Get -> respond("""[{"category":"HEALTH","limitCents":10000}]""", headers = json)
            rejectPosts -> respond("""{"error":"invalid"}""", HttpStatusCode.UnprocessableEntity, json)
            else -> respond(String(req.body.toByteArray()), HttpStatusCode.Created, json)
        }
    })

    @Test
    fun refreshLoadsAndSortsNewestFirst() = runTest {
        val repo = WalletRepository(api())
        repo.refresh()
        assertEquals(listOf("b", "a"), repo.expenses.value.map { it.id })
        assertEquals(10_000, repo.budgets.value.single().limitCents)
    }

    @Test
    fun optimisticAddIsRolledBackWhenServerRejects() = runTest {
        val repo = WalletRepository(api(rejectPosts = true))
        repo.refresh()
        assertFailsWith<Exception> {
            repo.add(Expense("new", "Cinema", 5000, Category.LEISURE, "2026-03-06"))
        }
        assertTrue(repo.expenses.value.none { it.id == "new" })
    }

    @Test
    fun addKeepsServerVersion() = runTest {
        val repo = WalletRepository(api())
        repo.add(Expense("new", "Cinema", 5000, Category.LEISURE, "2026-03-06"))
        assertEquals("new", repo.expenses.value.first().id)
    }
}
