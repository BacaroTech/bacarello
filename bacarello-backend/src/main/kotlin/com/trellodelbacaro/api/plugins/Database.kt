package com.trellodelbacaro.api.plugins

import com.trellodelbacaro.infrastructure.persistence.BoardMembers
import com.trellodelbacaro.infrastructure.persistence.Boards
import com.trellodelbacaro.infrastructure.persistence.Cards
import com.trellodelbacaro.infrastructure.persistence.ChecklistItems
import com.trellodelbacaro.infrastructure.persistence.Lists
import com.trellodelbacaro.infrastructure.persistence.Users
import com.trellodelbacaro.infrastructure.persistence.Workspaces
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configura la connessione al database leggendo le variabili d'ambiente.
 * Default: H2 in-memory (sviluppo locale senza Docker).
 * Con DB_URL=jdbc:postgresql://... usa PostgreSQL (vedi docker-compose.yml).
 */
fun Application.configureDatabase(): Database {
    val url = System.getenv("DB_URL") ?: "jdbc:h2:mem:bacarello;DB_CLOSE_DELAY=-1;"
    val driver = System.getenv("DB_DRIVER")
        ?: if (url.startsWith("jdbc:postgresql")) "org.postgresql.Driver" else "org.h2.Driver"
    val user = System.getenv("DB_USER") ?: "root"
    val password = System.getenv("DB_PASSWORD") ?: ""

    val database = Database.connect(url = url, driver = driver, user = user, password = password)
    transaction(database) {
        SchemaUtils.create(Users, Workspaces, Boards, BoardMembers, Lists, Cards, ChecklistItems)
    }
    log.info("Database connesso: $url")
    return database
}
