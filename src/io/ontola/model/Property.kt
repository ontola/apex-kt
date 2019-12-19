package io.ontola.apex.io.ontola.model

import org.jetbrains.exposed.dao.UUIDTable

object Property : UUIDTable() {
    val boolean = bool("boolean")
//    val dateTime = datetime("datetime")
    val integer = long("integer")
    val string = varchar("string", 100_000)
    val text = text("text")
    val linked_node_id = reference("linked_node_id", Node)

//    t.datetime "created_at", null: false
//    t.datetime "updated_at", null: false
//    t.uuid "edge_id", null: false
//    t.string "predicate", null: false
//    t.text "text"
//    t.integer "order", default: 0, null: false
//    t.index ["edge_id"], name: "index_properties_on_edge_id"
}
