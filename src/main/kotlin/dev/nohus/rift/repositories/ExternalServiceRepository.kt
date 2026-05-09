package dev.nohus.rift.repositories

import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.menu_anoikis
import dev.nohus.rift.generated.resources.menu_dotlan
import dev.nohus.rift.generated.resources.menu_everef
import dev.nohus.rift.generated.resources.menu_evewho
import dev.nohus.rift.generated.resources.menu_newedenencyclopedia
import dev.nohus.rift.generated.resources.menu_uniwiki
import dev.nohus.rift.generated.resources.menu_zkillboard
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.ExternalService
import dev.nohus.rift.settings.persistence.ExternalService.Anoikis
import dev.nohus.rift.settings.persistence.ExternalService.Dotlan
import dev.nohus.rift.settings.persistence.ExternalService.EveRef
import dev.nohus.rift.settings.persistence.ExternalService.EveWho
import dev.nohus.rift.settings.persistence.ExternalService.NewEdenEncyclopedia
import dev.nohus.rift.settings.persistence.ExternalService.UniWiki
import dev.nohus.rift.settings.persistence.ExternalService.ZKillboard
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.koin.core.annotation.Single

@Single
class ExternalServiceRepository(
    private val settings: Settings,
) {

    private data class ServiceItem(
        val service: ExternalService,
        val url: String,
    )

    fun getCharacterMenuItems(characterId: Int): List<ContextMenuItem.TextItem> {
        return getCharacterItems(characterId).toMenuItems()
    }

    fun openCharacterPreferredService(characterId: Int) {
        val items = getCharacterItems(characterId)
        val preferredService = getPreferredService(listOf(ZKillboard, EveWho))
        items.firstOrNull { it.service == preferredService }?.url?.toURIOrNull()?.openBrowser()
    }

    private fun getCharacterItems(characterId: Int): List<ServiceItem> {
        return listOf(
            ServiceItem(ZKillboard, "https://zkillboard.com/character/$characterId/"),
            ServiceItem(EveWho, "https://evewho.com/character/$characterId"),
        )
    }

    fun getCorporationMenuItems(corporationId: Int): List<ContextMenuItem.TextItem> {
        return getCorporationItems(corporationId).toMenuItems()
    }

    fun openCorporationPreferredService(corporationId: Int) {
        val items = getCorporationItems(corporationId)
        val preferredService = getPreferredService(listOf(ZKillboard, EveWho))
        items.firstOrNull { it.service == preferredService }?.url?.toURIOrNull()?.openBrowser()
    }

    private fun getCorporationItems(corporationId: Int): List<ServiceItem> {
        return listOf(
            ServiceItem(ZKillboard, "https://zkillboard.com/corporation/$corporationId/"),
            ServiceItem(EveWho, "https://evewho.com/corporation/$corporationId"),
        )
    }

    fun getAllianceMenuItems(allianceId: Int): List<ContextMenuItem.TextItem> {
        return getAllianceItems(allianceId).toMenuItems()
    }

    fun openAlliancePreferredService(allianceId: Int) {
        val items = getAllianceItems(allianceId)
        val preferredService = getPreferredService(listOf(ZKillboard, EveWho))
        items.firstOrNull { it.service == preferredService }?.url?.toURIOrNull()?.openBrowser()
    }

    private fun getAllianceItems(allianceId: Int): List<ServiceItem> {
        return listOf(
            ServiceItem(ZKillboard, "https://zkillboard.com/alliance/$allianceId/"),
            ServiceItem(EveWho, "https://evewho.com/alliance/$allianceId"),
        )
    }

    fun getShipMenuItems(type: Type): List<ContextMenuItem.TextItem> {
        return getShipItems(type).toMenuItems()
    }

    fun openShipPreferredService(type: Type) {
        val items = getShipItems(type)
        val preferredService = getPreferredService(listOf(UniWiki, EveRef, ZKillboard, NewEdenEncyclopedia))
        items.firstOrNull { it.service == preferredService }?.url?.toURIOrNull()?.openBrowser()
    }

    private fun getShipItems(type: Type): List<ServiceItem> {
        return listOf(
            ServiceItem(UniWiki, "https://wiki.eveuniversity.org/${type.name.replace(' ', '_')}"),
            ServiceItem(EveRef, "https://everef.net/types/${type.id}"),
            ServiceItem(ZKillboard, "https://zkillboard.com/ship/${type.id}/"),
            ServiceItem(NewEdenEncyclopedia, "https://newedenencyclopedia.net/type/${type.id}"),
        )
    }

    fun getTypeMenuItems(type: Type): List<ContextMenuItem.TextItem> {
        return getTypeItems(type).toMenuItems()
    }

    fun openTypePreferredService(type: Type) {
        val items = getTypeItems(type)
        val preferredService = getPreferredService(listOf(EveRef, ZKillboard, NewEdenEncyclopedia))
        items.firstOrNull { it.service == preferredService }?.url?.toURIOrNull()?.openBrowser()
    }

    private fun getTypeItems(type: Type): List<ServiceItem> {
        return listOf(
            ServiceItem(EveRef, "https://everef.net/type/${type.id}"),
            ServiceItem(ZKillboard, "https://zkillboard.com/item/${type.id}/"),
            ServiceItem(NewEdenEncyclopedia, "https://newedenencyclopedia.net/type/${type.id}"),
        )
    }

    fun getSystemMenuItems(system: String, systemId: Int, isWormholeSpace: Boolean): List<ContextMenuItem.TextItem> {
        return getSystemItems(system, systemId, isWormholeSpace).toMenuItems()
    }

    private fun getSystemItems(system: String, systemId: Int, isWormholeSpace: Boolean): List<ServiceItem> {
        return buildList {
            if (isWormholeSpace) {
                add(ServiceItem(Anoikis, "https://anoik.is/systems/$system"))
            }
            add(ServiceItem(Dotlan, "https://evemaps.dotlan.net/system/$system"))
            add(ServiceItem(ZKillboard, "https://zkillboard.com/system/$systemId/"))
        }
    }

    private fun List<ServiceItem>.toMenuItems(): List<ContextMenuItem.TextItem> {
        val among = map { it.service }
        return map { it.toMenuItem(among) }
    }

    private fun ServiceItem.toMenuItem(among: List<ExternalService>): ContextMenuItem.TextItem {
        return ContextMenuItem.TextItem(
            text = when (service) {
                EveWho -> "EveWho"
                ZKillboard -> "zKillboard"
                UniWiki -> "UniWiki"
                EveRef -> "EVE Ref"
                NewEdenEncyclopedia -> "New Eden Encyclopedia"
                Dotlan -> "Dotlan"
                Anoikis -> "Anoikis"
            },
            iconResource = when (service) {
                EveWho -> Res.drawable.menu_evewho
                ZKillboard -> Res.drawable.menu_zkillboard
                UniWiki -> Res.drawable.menu_uniwiki
                EveRef -> Res.drawable.menu_everef
                NewEdenEncyclopedia -> Res.drawable.menu_newedenencyclopedia
                Dotlan -> Res.drawable.menu_dotlan
                Anoikis -> Res.drawable.menu_anoikis
            },
            onClick = {
                setPreferredService(service, among)
                url.toURIOrNull()?.openBrowser()
            },
        )
    }

    private fun getPreferredService(services: List<ExternalService>): ExternalService {
        return settings.preferredExternalServices.firstOrNull { it in services }
            ?: services.first().also { setPreferredService(it, services) }
    }

    private fun setPreferredService(service: ExternalService, among: List<ExternalService>) {
        val preferred = settings.preferredExternalServices
        val firstIndex = among.mapNotNull { preferred.indexOf(it).takeIf { i -> i >= 0 } }.minOfOrNull { it }
        settings.preferredExternalServices = preferred.toMutableList().apply {
            remove(service)
            if (firstIndex != null) {
                add(firstIndex, service)
            } else {
                add(service)
            }
        }
    }
}
