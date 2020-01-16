package io.ontola.apex.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ontola.apex.model.Documents
import io.ontola.apex.model.Properties
import io.ontola.apex.model.Resources
import io.ontola.rdf.dsl.iri
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object DatabaseFactory {
    fun init(testing: Boolean) {
        if (testing){
            Database.connect(testingConfig())
        } else {
            Database.connect(pgConfig())
//            Database.connect(
//                "jdbc:postgresql://localhost:5432/ori",
//                driver = "org.postgresql.Driver",
//                user = "ori_postgres_user",
//                password = ""
//            )
        }

        if (testing) {
            transaction {
                SchemaUtils.create(Documents, Resources, Properties)

                val bobDoc = Documents.insert {
//                    it[id] = 5 // UUID.fromString("6bd19870-141b-42ea-8540-d9023715ef9f")
                    it[iri] = "http://localhost:8000/people/bob".iri().stringValue()
                } get Documents.id

                val bob = Resources.insert {
                    it[document] = bobDoc
                    it[iri] = "http://localhost:8000/people/bob".iri().stringValue()
                } get Resources.id

                Properties.insert {
                    it[resource] = bob
                    it[predicate] = "https://schema.org/name"
                    it[string] = "Bob's document"
                }
                Properties.insert {
                    it[resource] = bob
                    it[predicate] = "https://schema.org/description"
                    it[string] = "An electronic document about bob"
                }

                val bobCard = Resources.insert {
                    it[document] = bobDoc
                    it[iri] = "http://localhost:8000/people/bob#me".iri().stringValue()
                } get Resources.id

                Properties.insert {
                    it[resource] = bobCard
                    it[predicate] = "https://schema.org/name"
                    it[string] = "Bob"
                }
                Properties.insert {
                    it[resource] = bobCard
                    it[predicate] = "https://schema.org/description"
                    it[string] = "A typical computer person"
                }

                val bobMeta = Resources.insert {
                    it[document] = bobDoc
                    it[iri] = "http://localhost:8000/people/bob#meta".iri().stringValue()
                } get Resources.id

                Properties.insert {
                    it[resource] = bobMeta
                    it[predicate] = "https://schema.org/name"
                    it[string] = "About Bob"
                }
                Properties.insert {
                    it[resource] = bobMeta
                    it[predicate] = "https://schema.org/description"
                    it[string] = "Whatever metadata we have"
                }

                val aliceDoc = Documents.insert {
                    it[id] = 5 // UUID.fromString("6bd19870-141b-42ea-8540-d9023715ef9f")
                    it[iri] = "http://localhost:8000/people/bob".iri().stringValue()
                } get Documents.id

                val alice = Resources.insert {
                    it[document] = aliceDoc
                    it[iri] = "http://localhost:8000/people/alice".iri().stringValue()
                } get Resources.id

                Properties.insert {
                    it[resource] = bob
                    it[predicate] = "https://schema.org/friend"
//                    it[node] = bob
                }
            }
        }
    }

    private fun pgConfig(): HikariDataSource {
        val props = Properties()

        props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
        props.setProperty("dataSource.user", System.getenv("POSTGRES_USERNAME") ?: "ori_postgres_user")
        props.setProperty("dataSource.password", System.getenv("POSTGRES_PASSWORD") ?: "")
        props.setProperty("dataSource.databaseName", System.getenv("ORI_API_POSTGRESQL_DATABASE") ?: "ori")
        props.setProperty("dataSource.serverName", System.getenv("ORI_API_POSTGRESQL_ADDRESS") ?: "localhost")
        val config = HikariConfig(props)

        return HikariDataSource(config)
    }

    /**
     * In-memory testing database
     */
    private fun testingConfig(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.h2.Driver"
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.dataSourceProperties["DATABASE_TO_UPPER"] = "false"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(
        block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.Default) { block() }
}
