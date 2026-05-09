package dev.nohus.rift.wallet

import androidx.compose.ui.graphics.Color

sealed class TransactionGroup(
    val name: String,
    val color: Color,
) {
    data object CorporationAlliance : TransactionGroup(name = "Corp and Alliance", color = Color(0xFF5c59d8))
    data object AgentsAndMissions : TransactionGroup(name = "Agents and Missions", color = Color(0xFFe68348))
    data object Trade : TransactionGroup(name = "Trade", color = Color(0xFF31c4a1))
    data object Bounty : TransactionGroup(name = "Bounties", color = Color(0xFFe53a3a))
    data object Industry : TransactionGroup(name = "Industry", color = Color(0xFF3de53a))
    data object Transfer : TransactionGroup(name = "Transfers", color = Color(0xFFe6e048))
    data object Misc : TransactionGroup(name = "Miscellaneous", color = Color(0xFF808080))
    data object HypernetRelay : TransactionGroup(name = "Hypernet", color = Color(0xFF3aa8e5))

    companion object {
        fun byReferenceType(referenceType: String) = when (referenceType) {
            "acceleration_gate_fee" -> Misc
            "advertisement_listing_fee" -> CorporationAlliance
            "agent_donation" -> AgentsAndMissions
            "agent_location_services" -> AgentsAndMissions
            "agent_miscellaneous" -> AgentsAndMissions
            "agent_mission_collateral_paid" -> AgentsAndMissions
            "agent_mission_collateral_refunded" -> AgentsAndMissions
            "agent_mission_reward" -> AgentsAndMissions
            "agent_mission_reward_corporation_tax" -> CorporationAlliance
            "agent_mission_time_bonus_reward" -> AgentsAndMissions
            "agent_mission_time_bonus_reward_corporation_tax" -> CorporationAlliance
            "agent_security_services" -> AgentsAndMissions
            "agent_services_rendered" -> AgentsAndMissions
            "agents_preward" -> AgentsAndMissions
            "air_career_program_reward" -> AgentsAndMissions
            "alliance_maintainance_fee" -> CorporationAlliance
            "alliance_registration_fee" -> CorporationAlliance
            "allignment_based_gate_toll" -> Misc
            "asset_safety_recovery_tax" -> Misc
            "bounty" -> Bounty
            "bounty_prize" -> Bounty
            "bounty_prize_corporation_tax" -> Bounty
            "bounty_prizes" -> Bounty
            "bounty_reimbursement" -> Bounty
            "bounty_surcharge" -> Bounty
            "brokers_fee" -> Trade
            "clone_activation" -> Misc
            "clone_transfer" -> Misc
            "contraband_fine" -> Misc
            "contract_auction_bid" -> Trade
            "contract_auction_bid_corp" -> Trade
            "contract_auction_bid_refund" -> Trade
            "contract_auction_sold" -> Trade
            "contract_brokers_fee" -> Trade
            "contract_brokers_fee_corp" -> Trade
            "contract_collateral" -> Trade
            "contract_collateral_deposited_corp" -> Trade
            "contract_collateral_payout" -> Trade
            "contract_collateral_refund" -> Trade
            "contract_deposit" -> Trade
            "contract_deposit_corp" -> Trade
            "contract_deposit_refund" -> Trade
            "contract_deposit_sales_tax" -> Trade
            "contract_price" -> Trade
            "contract_price_payment_corp" -> Trade
            "contract_reversal" -> Trade
            "contract_reward" -> Trade
            "contract_reward_deposited" -> Trade
            "contract_reward_deposited_corp" -> Trade
            "contract_reward_refund" -> Trade
            "contract_sales_tax" -> Trade
            "copying" -> Industry
            "corporate_reward_payout" -> CorporationAlliance
            "corporate_reward_tax" -> CorporationAlliance
            "corporation_account_withdrawal" -> CorporationAlliance
            "corporation_bulk_payment" -> CorporationAlliance
            "corporation_dividend_payment" -> CorporationAlliance
            "corporation_liquidation" -> CorporationAlliance
            "corporation_logo_change_cost" -> CorporationAlliance
            "corporation_payment" -> CorporationAlliance
            "corporation_registration_fee" -> CorporationAlliance
            "cosmetic_market_component_item_purchase" -> Trade
            "cosmetic_market_skin_purchase" -> Trade
            "cosmetic_market_skin_sale" -> Trade
            "cosmetic_market_skin_sale_broker_fee" -> Trade
            "cosmetic_market_skin_sale_tax" -> Trade
            "cosmetic_market_skin_transaction" -> Trade
            "courier_mission_escrow" -> AgentsAndMissions
            "cspa" -> Misc
            "cspaofflinerefund" -> Misc
            "daily_challenge_reward" -> AgentsAndMissions
            "daily_goal_payouts" -> AgentsAndMissions
            "daily_goal_payouts_tax" -> AgentsAndMissions
            "datacore_fee" -> Industry
            "dna_modification_fee" -> Misc
            "docking_fee" -> Misc
            "duel_wager_escrow" -> Misc
            "duel_wager_payment" -> Misc
            "duel_wager_refund" -> Misc
            "ess_escrow_transfer" -> Bounty
            "external_trade_delivery" -> Trade
            "external_trade_freeze" -> Trade
            "external_trade_thaw" -> Trade
            "factory_slot_rental_fee" -> Industry
            "flux_payout" -> HypernetRelay
            "flux_tax" -> HypernetRelay
            "flux_ticket_repayment" -> HypernetRelay
            "flux_ticket_sale" -> HypernetRelay
            "freelance_jobs_broadcasting_fee" -> Misc
            "freelance_jobs_duration_fee" -> Misc
            "freelance_jobs_escrow_refund" -> Misc
            "freelance_jobs_reward" -> Misc
            "freelance_jobs_reward_corporation_tax" -> Misc
            "freelance_jobs_reward_escrow" -> Misc
            "gm_cash_transfer" -> Misc
            "gm_plex_fee_refund" -> Misc
            "industry_job_tax" -> Industry
            "infrastructure_hub_maintenance" -> CorporationAlliance
            "inheritance" -> Misc
            "insurance" -> Misc
            "insurgency_corruption_contribution_reward" -> Misc
            "insurgency_suppression_contribution_reward" -> Misc
            "item_trader_payment" -> Trade
            "jump_clone_activation_fee" -> Misc
            "jump_clone_installation_fee" -> Misc
            "kill_right_fee" -> Misc
            "lp_store" -> Trade
            "manufacturing" -> Industry
            "market_escrow" -> Trade
            "market_fine_paid" -> Trade
            "market_provider_tax" -> Trade
            "market_transaction" -> Trade
            "medal_creation" -> CorporationAlliance
            "medal_issued" -> CorporationAlliance
            "milestone_reward_payment" -> Misc
            "mission_completion" -> AgentsAndMissions
            "mission_cost" -> AgentsAndMissions
            "mission_expiration" -> AgentsAndMissions
            "mission_reward" -> AgentsAndMissions
            "office_rental_fee" -> CorporationAlliance
            "operation_bonus" -> Misc
            "opportunity_reward" -> Misc
            "planetary_construction" -> Industry
            "planetary_export_tax" -> Industry
            "planetary_import_tax" -> Industry
            "player_donation" -> Transfer
            "player_trading" -> Trade
            "project_discovery_reward" -> Misc
            "project_discovery_tax" -> Misc
            "project_payouts" -> Misc
            "reaction" -> Industry
            "redeemed_isk_token" -> Misc
            "release_of_impounded_property" -> Misc
            "repair_bill" -> Misc
            "reprocessing_tax" -> Misc
            "researching_material_productivity" -> Industry
            "researching_technology" -> Industry
            "researching_time_productivity" -> Industry
            "resource_wars_reward" -> Misc
            "reverse_engineering" -> Industry
            "season_challenge_reward" -> Misc
            "security_processing_fee" -> Misc
            "shares" -> CorporationAlliance
            "skill_purchase" -> Trade
            "skyhook_claim_fee" -> CorporationAlliance
            "sovereignity_bill" -> CorporationAlliance
            "store_purchase" -> Trade
            "store_purchase_refund" -> Trade
            "structure_gate_jump" -> Misc
            "transaction_tax" -> Trade
            "under_construction" -> Misc
            "upkeep_adjustment_fee" -> Misc
            "war_ally_contract" -> CorporationAlliance
            "war_fee" -> CorporationAlliance
            "war_fee_surrender" -> CorporationAlliance
            else -> Misc
        }
    }
}

fun getReferenceTypeName(referenceType: String): String {
    return referenceType
        .split('_')
        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
        .replace("Ess", "ESS")
}
