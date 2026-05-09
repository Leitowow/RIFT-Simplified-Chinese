package dev.nohus.rift.wallet

import java.time.Instant

data class WalletJournalItem(
    val wallet: Wallet,
    val amount: Double,
    val balance: Double?,
    val context: TypeDetail?,
    val date: Instant,
    val description: String,
    val id: Long,
    val reason: String?,
    val reasonTypeDetails: List<TypeDetail>,
    val refType: String,
    val firstParty: TypeDetail?,
    val secondParty: TypeDetail?,
    val tax: Double?,
    val taxReceiver: TypeDetail?,
    val transaction: WalletTransactionItem?,
)

data class WalletTransactionItem(
    val client: TypeDetail?,
    val date: Instant,
    val isBuy: Boolean,
    val isPersonal: Boolean?,
    val location: TypeDetail?,
    val quantity: Long,
    val transactionId: Long,
    val type: TypeDetail?,
    val unitPrice: Double,
)

sealed interface Wallet {
    data class Character(val characterId: Int) : Wallet
    data class Corporation(val corporationId: Int, val divisionId: Int) : Wallet
}
