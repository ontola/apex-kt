package io.ontola.apex

import org.jetbrains.exposed.sql.Database

fun getDatabaseConnection(): Database {
    return Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", driver = "org.h2.Driver")
}
