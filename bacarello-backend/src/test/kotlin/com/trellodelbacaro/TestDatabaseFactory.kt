package com.trellodelbacaro

import com.trellodelbacaro.infrastructure.persistence.BoardMembers
import com.trellodelbacaro.infrastructure.persistence.Boards
import com.trellodelbacaro.infrastructure.persistence.Cards
import com.trellodelbacaro.infrastructure.persistence.ChecklistItems
import com.trellodelbacaro.infrastructure.persistence.Lists
import com.trellodelbacaro.infrastructure.persistence.Users
import com.trellodelbacaro.infrastructure.persistence.Workspaces
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabaseFactory {
    fun create(): Database {
        val db = Database.connect(
            url = "jdbc:h2:mem:test_${System.nanoTime()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
        transaction(db) {
            SchemaUtils.create(Users, Workspaces, Boards, BoardMembers, Lists, Cards, ChecklistItems)
        }
        return db
    }
}
