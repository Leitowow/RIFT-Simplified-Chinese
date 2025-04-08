package dev.nohus.rift.database.local

import org.jetbrains.exposed.sql.Table

object Characters2 : Table() {
    val name = varchar("name", 37)
    val characterId = integer("characterId").nullable()
    val status = enumeration("status", CharacterStatus::class)
    val checkTimestamp = long("checkTimestamp")
    override val primaryKey = PrimaryKey(name)
}

enum class CharacterStatus {
    Active, Inactive, Dormant, DoesNotExists
}
