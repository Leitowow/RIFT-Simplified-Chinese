package dev.nohus.rift.database.local

import dev.nohus.rift.network.esi.models.ContextIdType
import org.jetbrains.exposed.sql.Table

object Characters2 : Table() {
    val name = varchar("name", 37)
    val characterId = integer("characterId").nullable()
    val status = enumeration("status", CharacterStatus::class)
    val checkTimestamp = long("checkTimestamp")
    override val primaryKey = PrimaryKey(name)
}

enum class CharacterStatus {
    Active,
    Inactive,
    Dormant,
    DoesNotExists,
}

object CharacterAffiliations : Table() {
    val characterId = integer("characterId")
    val corporationId = integer("corporationId")
    val allianceId = integer("allianceId").nullable()
    val factionId = integer("factionId").nullable()
    val checkTimestamp = long("checkTimestamp")
    override val primaryKey = PrimaryKey(characterId)
}

object WalletJournalEntries : Table() {
    val amount = double("amount").nullable()
    val balance = double("balance").nullable()
    val contextId = long("contextId").nullable()
    val contextIdType = enumerationByName("contextIdType", 25, ContextIdType::class).nullable()
    val date = long("date")
    val description = text("description")
    val id = long("id")
    val reason = text("reason").nullable()
    val refType = text("refType")
    val firstPartyId = long("firstPartyId").nullable()
    val secondPartyId = long("secondPartyId").nullable()
    val tax = double("tax").nullable()
    val taxReceiverId = long("taxReceiverId").nullable()

    val isCorporationWallet = bool("isCorporationWallet")
    val characterId = integer("characterId").nullable()
    val corporationId = integer("corporationId").nullable()
    val divisionId = integer("divisionId").nullable()
    override val primaryKey = PrimaryKey(id)
}

object WalletTransactions : Table() {
    val clientId = long("clientId")
    val date = long("date")
    val isBuy = bool("isBuy")
    val isPersonal = bool("isPersonal").nullable()
    val journalRefId = long("journalRefId")
    val locationId = long("locationId")
    val quantity = long("quantity")
    val transactionId = long("transactionId")
    val typeId = long("typeId")
    val unitPrice = double("unitPrice")
    override val primaryKey = PrimaryKey(transactionId)
}
