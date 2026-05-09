package dev.nohus.rift.sso.scopes

sealed class EsiScope(val id: String) {
    abstract class Alliances(id: String) : EsiScope(id) {
        object ReadContacts : Alliances("esi-alliances.read_contacts.v1")
    }
    abstract class Corporations(id: String) : EsiScope(id) {
        object ReadContacts : Corporations("esi-corporations.read_contacts.v1")
        object ReadProjects : Corporations("esi-corporations.read_projects.v1")
        object ReadDivisions : Corporations("esi-corporations.read_divisions.v1")
        object ReadFreelanceJobs : Corporations("esi-corporations.read_freelance_jobs.v1")
    }
    abstract class Characters(id: String) : EsiScope(id) {
        object ReadContacts : Characters("esi-characters.read_contacts.v1")
        object WriteContacts : Characters("esi-characters.write_contacts.v1")
        object ReadCorporationRoles : Characters("esi-characters.read_corporation_roles.v1")
        object ReadLoyalty : Characters("esi-characters.read_loyalty.v1")
        object ReadFreelanceJobs : Characters("esi-characters.read_freelance_jobs.v1")
    }
    abstract class Clones(id: String) : EsiScope(id) {
        object ReadClones : Clones("esi-clones.read_clones.v1")
        object ReadImplants : Clones("esi-clones.read_implants.v1")
    }
    abstract class Locations(id: String) : EsiScope(id) {
        object ReadOnline : Locations("esi-location.read_online.v1")
        object ReadLocation : Locations("esi-location.read_location.v1")
        object ReadShipType : Locations("esi-location.read_ship_type.v1")
    }
    abstract class Planets(id: String) : EsiScope(id) {
        object ManagePlanets : Planets("esi-planets.manage_planets.v1")
    }
    abstract class Wallet(id: String) : EsiScope(id) {
        object ReadCharacterWallet : Wallet("esi-wallet.read_character_wallet.v1")
        object ReadCorporationWallets : Wallet("esi-wallet.read_corporation_wallets.v1")
    }
    abstract class Search(id: String) : EsiScope(id) {
        object SearchStructures : Search("esi-search.search_structures.v1")
    }
    abstract class Universe(id: String) : EsiScope(id) {
        object ReadStructures : Universe("esi-universe.read_structures.v1")
    }
    abstract class Ui(id: String) : EsiScope(id) {
        object WriteWaypoint : Ui("esi-ui.write_waypoint.v1")
        object OpenWindow : Ui("esi-ui.open_window.v1")
    }
    abstract class Assets(id: String) : EsiScope(id) {
        object ReadAssets : Assets("esi-assets.read_assets.v1")
        object ReadCorporationAssets : Assets("esi-assets.read_corporation_assets.v1")
    }
    abstract class Fleets(id: String) : EsiScope(id) {
        object ReadFleet : Fleets("esi-fleets.read_fleet.v1")
    }
}
