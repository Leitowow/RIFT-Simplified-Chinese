package dev.nohus.rift.repositories

import org.koin.core.annotation.Single

@Single
class RatsRepository(
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    enum class RatType {
        BloodRaiders,
        Guristas,
        SanshasNation,
        Serpentis,
        AngelCartel,
        RogueDrones,
        TriglavianCollective,
    }

    private val rats = mapOf(
        "Aridia" to RatType.BloodRaiders,
        "Black Rise" to RatType.Guristas,
        "The Bleak Lands" to RatType.BloodRaiders,
        "The Citadel" to RatType.Guristas,
        "Derelik" to RatType.SanshasNation,
        "Devoid" to RatType.SanshasNation,
        "Domain" to RatType.SanshasNation,
        "Essence" to RatType.Serpentis,
        "Everyshore" to RatType.Serpentis,
        "The Forge" to RatType.Guristas,
        "Genesis" to RatType.BloodRaiders,
        "Heimatar" to RatType.AngelCartel,
        "Kador" to RatType.BloodRaiders,
        "Khanid" to RatType.BloodRaiders,
        "Kor-Azor" to RatType.BloodRaiders,
        "Lonetrek" to RatType.Guristas,
        "Metropolis" to RatType.AngelCartel,
        "Molden Heath" to RatType.AngelCartel,
        "Placid" to RatType.Serpentis,
        "Sinq Laison" to RatType.Serpentis,
        "Solitude" to RatType.Serpentis,
        "Tash-Murkon" to RatType.SanshasNation,
        "Verge Vendor" to RatType.Serpentis,
        "Branch" to RatType.Guristas,
        "Cache" to RatType.AngelCartel,
        "Catch" to RatType.SanshasNation,
        "Cloud Ring" to RatType.Serpentis,
        "Cobalt Edge" to RatType.RogueDrones,
        "Curse" to RatType.AngelCartel,
        "Deklein" to RatType.Guristas,
        "Delve" to RatType.BloodRaiders,
        "Detorid" to RatType.AngelCartel,
        "Esoteria" to RatType.SanshasNation,
        "Etherium Reach" to RatType.RogueDrones,
        "Fade" to RatType.Serpentis,
        "Feythabolis" to RatType.AngelCartel,
        "Fountain" to RatType.Serpentis,
        "Geminate" to RatType.Guristas,
        "Great Wildlands" to RatType.AngelCartel,
        "Immensea" to RatType.AngelCartel,
        "Impass" to RatType.AngelCartel,
        "Insmother" to RatType.AngelCartel,
        "The Kalevala Expanse" to RatType.RogueDrones,
        "Malpais" to RatType.RogueDrones,
        "Oasa" to RatType.RogueDrones,
        "Omist" to RatType.AngelCartel,
        "Outer Passage" to RatType.RogueDrones,
        "Outer Ring" to RatType.Serpentis,
        "Paragon Soul" to RatType.SanshasNation,
        "Period Basis" to RatType.BloodRaiders,
        "Perrigen Falls" to RatType.RogueDrones,
        "Pochven" to RatType.TriglavianCollective,
        "Providence" to RatType.SanshasNation,
        "Pure Blind" to RatType.Guristas,
        "Querious" to RatType.BloodRaiders,
        "Scalding Pass" to RatType.AngelCartel,
        "The Spire" to RatType.RogueDrones,
        "Stain" to RatType.SanshasNation,
        "Syndicate" to RatType.Serpentis,
        "Tenal" to RatType.Guristas,
        "Tenerifis" to RatType.AngelCartel,
        "Tribute" to RatType.Guristas,
        "Vale of the Silent" to RatType.Guristas,
        "Venal" to RatType.Guristas,
        "Wicked Creek" to RatType.AngelCartel,
    ).mapKeys { (name, _) ->
        solarSystemsRepository.getRegionId(name)
    }

    fun getRats(systemId: Int): RatType? {
        val regionId = solarSystemsRepository.getRegionIdBySystemId(systemId)
        return rats[regionId]
    }
}
