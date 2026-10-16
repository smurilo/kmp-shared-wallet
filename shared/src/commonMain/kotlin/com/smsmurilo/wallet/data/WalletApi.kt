package com.smsmurilo.wallet.data

import com.smsmurilo.wallet.model.Budget
import com.smsmurilo.wallet.model.Expense
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Same networking code runs on Android (OkHttp engine) and iOS (Darwin/NSURLSession engine). */
class WalletApi(private val client: HttpClient) {

    suspend fun expenses(): List<Expense> = client.get("v1/expenses").body()

    suspend fun budgets(): List<Budget> = client.get("v1/budgets").body()

    suspend fun add(expense: Expense): Expense = client.post("v1/expenses") {
        contentType(ContentType.Application.Json)
        setBody(expense)
    }.body()

    companion object {
        fun create(baseUrl: String, engine: io.ktor.client.engine.HttpClientEngine = platformEngine()) = WalletApi(
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) { requestTimeoutMillis = 15_000 }
                defaultRequest { url(baseUrl) }
            },
        )
    }
}

expect fun platformEngine(): io.ktor.client.engine.HttpClientEngine
