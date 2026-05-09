package dev.nohus.rift.sso.scopes

object ScopeGroups {
    val readContacts = ScopeGroup(
        name = "Read contacts",
        reasons = listOf(
            "Needed to show hostiles in red and friendlies in blue based on standings",
            "Needed to show contacts in the Contacts window",
        ),
        scopes = listOf(
            EsiScope.Characters.ReadContacts,
            EsiScope.Corporations.ReadContacts,
            EsiScope.Alliances.ReadContacts,
        ),
    )
    val writeContacts = ScopeGroup(
        name = "Write contacts",
        reasons = listOf(
            "Needed to add and update contacts",
        ),
        scopes = listOf(
            EsiScope.Characters.WriteContacts,
        ),
    )
    val readClones = ScopeGroup(
        name = "Read clones",
        reasons = listOf(
            "Needed to show clones and implants in the Characters window",
        ),
        scopes = listOf(
            EsiScope.Clones.ReadClones,
            EsiScope.Clones.ReadImplants,
        ),
    )
    val readOnlineStatus = ScopeGroup(
        name = "Read online status",
        reasons = listOf(
            "Needed to know when a character is online",
        ),
        scopes = listOf(
            EsiScope.Locations.ReadOnline,
        ),
        isRequired = true,
    )
    val readCharacterLocation = ScopeGroup(
        name = "Read character location",
        reasons = listOf(
            "Needed to trigger distance-based alerts",
            "Needed to set autopilot destinations",
            "Needed to show distances in the Assets, Intel Reports, Intel Feed, and other windows",
            "Needed to update information when you move to a new system",
            "Needed to show characters in the Map window",
            "Needed to show jump ranges in the Map window",
            "Needed to show your location in the Characters window",
        ),
        scopes = listOf(
            EsiScope.Locations.ReadLocation,
        ),
    )
    val readCurrentShip = ScopeGroup(
        name = "Read current ship",
        reasons = listOf(
            "Needed to show your ship in the Characters window",
        ),
        scopes = listOf(
            EsiScope.Locations.ReadShipType,
        ),
    )
    val readPlanetaryIndustryColonies = ScopeGroup(
        name = "Read Planetary Industry colonies",
        reasons = listOf(
            "Needed to show colony details in the Planetary Industry window",
            "Needed to show your planets in the Map window",
        ),
        scopes = listOf(
            EsiScope.Planets.ManagePlanets,
        ),
    )
    val readWallet = ScopeGroup(
        name = "Read wallet",
        reasons = listOf(
            "Needed to show your wallet details in the Wallets window",
            "Needed to show your wallet balances in the Characters window",
        ),
        scopes = listOf(
            EsiScope.Wallet.ReadCharacterWallet,
        ),
    )
    val readCorporationWallet = ScopeGroup(
        name = "Read corporation wallet",
        reasons = listOf(
            "Needed to show your corporation wallet details in the Wallets window",
        ),
        scopes = listOf(
            EsiScope.Wallet.ReadCorporationWallets,
            EsiScope.Corporations.ReadDivisions,
        ),
    )
    val readLoyaltyPoints = ScopeGroup(
        name = "Read loyalty points",
        reasons = listOf(
            "Needed to show your loyalty point balances in the Wallets window",
        ),
        scopes = listOf(
            EsiScope.Characters.ReadLoyalty,
        ),
    )
    val readStructures = ScopeGroup(
        name = "Read structures",
        reasons = listOf(
            "Needed to show asset locations in the Assets window",
            "Needed to search in the Contacts window and others",
            "Needed to show character location in the Characters window",
            "Needed to show clone locations in the Characters window",
        ),
        scopes = listOf(
            EsiScope.Search.SearchStructures,
            EsiScope.Universe.ReadStructures,
        ),
    )
    val updateAutopilot = ScopeGroup(
        name = "Update autopilot",
        reasons = listOf(
            "Needed to set autopilot destinations",
        ),
        scopes = listOf(
            EsiScope.Ui.WriteWaypoint,
        ),
    )
    val openWindow = ScopeGroup(
        name = "Open in-game windows",
        reasons = listOf(
            "Needed to open Show Info windows and similar when you request it",
        ),
        scopes = listOf(
            EsiScope.Ui.OpenWindow,
        ),
    )
    val readAssets = ScopeGroup(
        name = "Read assets",
        reasons = listOf(
            "Needed to show assets in the Assets window",
            "Needed to show assets in the Map window",
        ),
        scopes = listOf(
            EsiScope.Assets.ReadAssets,
        ),
    )
    val readCorporationAssets = ScopeGroup(
        name = "Read corporation assets",
        reasons = listOf(
            "Needed to show corporation assets in the Assets window",
        ),
        scopes = listOf(
            EsiScope.Assets.ReadCorporationAssets,
        ),
    )
    val readProjects = ScopeGroup(
        name = "Read corporation projects",
        reasons = listOf(
            "Needed to show corporation projects in the Opportunities window",
        ),
        scopes = listOf(
            EsiScope.Corporations.ReadProjects,
        ),
    )
    val readJobs = ScopeGroup(
        name = "Read freelance jobs",
        reasons = listOf(
            "Needed to show freelance jobs your participate in in the Opportunities window",
            "Needed to show freelance jobs created by your corporation in the Opportunities window",
        ),
        scopes = listOf(
            EsiScope.Characters.ReadFreelanceJobs,
            EsiScope.Corporations.ReadFreelanceJobs,
        ),
    )
    val readRoles = ScopeGroup(
        name = "Read corporation roles",
        reasons = listOf(
            "Needed to show corporation assets in the Assets window",
            "Needed to show corporation wallet details in the Wallets window",
            "Needed to show corporation freelance jobs in the Opportunities window",
        ),
        scopes = listOf(
            EsiScope.Characters.ReadCorporationRoles,
        ),
    )

    val all = listOf(
        readContacts,
        writeContacts,
        readClones,
        readOnlineStatus,
        readCharacterLocation,
        readCurrentShip,
        readPlanetaryIndustryColonies,
        readWallet,
        readCorporationWallet,
        readLoyaltyPoints,
        readStructures,
        updateAutopilot,
        openWindow,
        readAssets,
        readCorporationAssets,
        readProjects,
        readJobs,
        readRoles,
    )

    fun getByIds(scopes: List<String>): List<ScopeGroup> {
        return all.filter { group ->
            val ids = group.scopes.map { it.id }
            scopes.containsAll(ids)
        }
    }
}
