package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.infrastructure.persistence.BoardMembers
import com.trellodelbacaro.infrastructure.persistence.Boards
import com.trellodelbacaro.infrastructure.persistence.Cards
import com.trellodelbacaro.infrastructure.persistence.ChecklistItems
import com.trellodelbacaro.infrastructure.persistence.Lists
import com.trellodelbacaro.infrastructure.persistence.Users
import com.trellodelbacaro.infrastructure.persistence.Workspaces
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configura la connessione al database leggendo le variabili d'ambiente.
 * - Default (nessuna env): H2 in-memory, per sviluppo locale senza Docker.
 * - Con DB_URL=jdbc:postgresql://...: PostgreSQL via pool HikariCP (vedi docker-compose.yml).
 *
 * NB: lo schema è creato con SchemaUtils.create (idempotente: crea solo le tabelle mancanti).
 * In produzione vera andrebbe sostituito con migrazioni Flyway versionate — vedi docs/backend-plan.md (P2).
 */
fun Application.configureDatabase(): Database {
    val url = System.getenv("DB_URL") ?: "jdbc:h2:mem:bacarello;DB_CLOSE_DELAY=-1;"
    val driver = System.getenv("DB_DRIVER")
        ?: if (url.startsWith("jdbc:postgresql")) "org.postgresql.Driver" else "org.h2.Driver"
    val user = System.getenv("DB_USER") ?: "root"
    val password = System.getenv("DB_PASSWORD") ?: ""

    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            this.password = password
            maximumPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 6
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
    )

    val database = Database.connect(dataSource)
    transaction(database) {
        SchemaUtils.create(Users, Workspaces, Boards, BoardMembers, Lists, Cards, ChecklistItems)
    }
    log.info("Database connesso: $url")
    return database
}
