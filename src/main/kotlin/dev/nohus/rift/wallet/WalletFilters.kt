package dev.nohus.rift.wallet

import dev.nohus.rift.wallet.WalletRepository.Character
import dev.nohus.rift.wallet.WalletRepository.Corporation
import java.time.Duration

data class WalletFilters(
    val direction: TransferDirection? = null,
    val walletTypes: List<WalletType> = emptyList(),
    val referenceTypes: List<String> = emptyList(),
    val timeSpan: Duration = Duration.ZERO,
    val party: TypeDetail? = null,
    val search: String? = null,
)

data class AvailableWalletFilters(
    val characters: List<Character> = emptyList(),
    val corporations: List<Corporation> = emptyList(),
    val referenceTypes: List<String> = emptyList(),
)

enum class TransferDirection {
    Income,
    Expense,
}

sealed interface WalletType {
    data object Character : WalletType
    data class SpecificCharacter(val characterId: Int) : WalletType
    data class SpecificCorporation(val corporationId: Int) : WalletType
    data class SpecificCorporationDivision(val corporationId: Int, val divisionId: Int) : WalletType
}
