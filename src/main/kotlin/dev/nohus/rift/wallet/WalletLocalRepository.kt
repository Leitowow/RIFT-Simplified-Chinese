package dev.nohus.rift.wallet

import dev.nohus.rift.database.local.LocalDatabase
import dev.nohus.rift.database.local.WalletJournalEntries
import dev.nohus.rift.database.local.WalletTransactions
import dev.nohus.rift.network.esi.models.WalletJournalEntry
import dev.nohus.rift.network.esi.models.WalletTransaction
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import java.time.Instant

/**
 * Stores wallet transactions in the local database
 */
@Single
class WalletLocalRepository(
    private val localDatabase: LocalDatabase,
) {

    suspend fun loadJournalEntries(): Map<Wallet, List<WalletJournalEntry>> {
        return localDatabase.transaction {
            WalletJournalEntries.selectAll().mapNotNull {
                val wallet = if (it[WalletJournalEntries.isCorporationWallet]) {
                    Wallet.Corporation(
                        corporationId = it[WalletJournalEntries.corporationId] ?: return@mapNotNull null,
                        divisionId = it[WalletJournalEntries.divisionId] ?: return@mapNotNull null,
                    )
                } else {
                    Wallet.Character(
                        characterId = it[WalletJournalEntries.characterId] ?: return@mapNotNull null,
                    )
                }

                wallet to WalletJournalEntry(
                    amount = it[WalletJournalEntries.amount],
                    balance = it[WalletJournalEntries.balance],
                    contextId = it[WalletJournalEntries.contextId],
                    contextIdType = it[WalletJournalEntries.contextIdType],
                    date = Instant.ofEpochMilli(it[WalletJournalEntries.date]),
                    description = it[WalletJournalEntries.description],
                    id = it[WalletJournalEntries.id],
                    reason = it[WalletJournalEntries.reason],
                    refType = it[WalletJournalEntries.refType],
                    firstPartyId = it[WalletJournalEntries.firstPartyId],
                    secondPartyId = it[WalletJournalEntries.secondPartyId],
                    tax = it[WalletJournalEntries.tax],
                    taxReceiverId = it[WalletJournalEntries.taxReceiverId],
                )
            }
        }.groupBy({ it.first }, { it.second })
    }

    suspend fun loadTransactions(): List<WalletTransaction> {
        return localDatabase.transaction {
            WalletTransactions.selectAll().map {
                WalletTransaction(
                    clientId = it[WalletTransactions.clientId],
                    date = Instant.ofEpochMilli(it[WalletTransactions.date]),
                    isBuy = it[WalletTransactions.isBuy],
                    isPersonal = it[WalletTransactions.isPersonal],
                    journalRefId = it[WalletTransactions.journalRefId],
                    locationId = it[WalletTransactions.locationId],
                    quantity = it[WalletTransactions.quantity],
                    transactionId = it[WalletTransactions.transactionId],
                    typeId = it[WalletTransactions.typeId],
                    unitPrice = it[WalletTransactions.unitPrice],
                )
            }
        }
    }

    suspend fun save(wallet: Wallet, items: List<WalletJournalEntry>) {
        localDatabase.transaction {
            WalletJournalEntries.batchUpsert(items) {
                this[WalletJournalEntries.amount] = it.amount
                this[WalletJournalEntries.balance] = it.balance
                this[WalletJournalEntries.contextId] = it.contextId
                this[WalletJournalEntries.contextIdType] = it.contextIdType
                this[WalletJournalEntries.date] = it.date.toEpochMilli()
                this[WalletJournalEntries.description] = it.description
                this[WalletJournalEntries.id] = it.id
                this[WalletJournalEntries.reason] = it.reason
                this[WalletJournalEntries.refType] = it.refType
                this[WalletJournalEntries.firstPartyId] = it.firstPartyId
                this[WalletJournalEntries.secondPartyId] = it.secondPartyId
                this[WalletJournalEntries.tax] = it.tax
                this[WalletJournalEntries.taxReceiverId] = it.taxReceiverId

                when (wallet) {
                    is Wallet.Character -> {
                        this[WalletJournalEntries.isCorporationWallet] = false
                        this[WalletJournalEntries.characterId] = wallet.characterId
                    }
                    is Wallet.Corporation -> {
                        this[WalletJournalEntries.isCorporationWallet] = true
                        this[WalletJournalEntries.corporationId] = wallet.corporationId
                        this[WalletJournalEntries.divisionId] = wallet.divisionId
                    }
                }
            }
        }
    }

    suspend fun save(items: List<WalletTransaction>) {
        localDatabase.transaction {
            WalletTransactions.batchUpsert(items) {
                this[WalletTransactions.clientId] = it.clientId
                this[WalletTransactions.date] = it.date.toEpochMilli()
                this[WalletTransactions.isBuy] = it.isBuy
                this[WalletTransactions.isPersonal] = it.isPersonal
                this[WalletTransactions.journalRefId] = it.journalRefId
                this[WalletTransactions.locationId] = it.locationId
                this[WalletTransactions.quantity] = it.quantity
                this[WalletTransactions.transactionId] = it.transactionId
                this[WalletTransactions.typeId] = it.typeId
                this[WalletTransactions.unitPrice] = it.unitPrice
            }
        }
    }
}
