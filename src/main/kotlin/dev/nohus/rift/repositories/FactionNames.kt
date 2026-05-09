package dev.nohus.rift.repositories

object FactionNames {
    private val names = mapOf(
        500001 to "Caldari State",
        500002 to "Minmatar Republic",
        500003 to "Amarr Empire",
        500004 to "Gallente Federation",
        500005 to "Jove Empire",
        500006 to "CONCORD Assembly",
        500007 to "Ammatar Mandate",
        500008 to "Khanid Kingdom",
        500009 to "The Syndicate",
        500010 to "Guristas Pirates",
        500011 to "Angel Cartel",
        500012 to "Blood Raider Covenant",
        500013 to "EverMore",
        500014 to "ORE",
        500015 to "Thukker Tribe",
        500016 to "Servant Sisters of EVE",
        500017 to "The Society of Conscious Thought",
        500018 to "Mordu's Legion Command",
        500019 to "Sansha's Nation",
        500020 to "Serpentis",
        500026 to "Triglavian Collective",
        500028 to "Association for Interdisciplinary Research",
        500029 to "Deathless Circle",
    )

    operator fun get(factionId: Int) = names[factionId] ?: "Unknown Faction"
}
