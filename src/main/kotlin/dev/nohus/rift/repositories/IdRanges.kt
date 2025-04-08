package dev.nohus.rift.repositories

object IdRanges {
    val faction = 500_000..599_999
    val npcCorporation = 1_000_000..1_999_999
    val npcAgent = 3_000_000..3_999_999
    val region = 10_000_000..19_999_999
    val knownSpaceRegion = 10_000_000..10_999_999
    val wormholeRegion = 11_000_000..11_999_999
    val abyssalRegion = 12_000_000..12_999_999
    val voidRegion = 14_000_000..14_999_999
    val solarSystem = 30_000_000..39_999_999
    val knownSpaceSystem = 30_000_000..30_999_999
    val wormholeSystem = 31_000_000..31_999_999
    val abyssalSystem = 32_000_000..32_999_999
    val voidSystem = 34_000_000..34_999_999
    val playerCorporation = 98_000_000..98_999_999
    val playerAlliance = 99_000_000..99_999_999

    fun isNpcAgent(id: Int) = id in npcAgent
    fun isFaction(id: Int) = id in faction
}
