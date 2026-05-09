package dev.nohus.rift.network.requests

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.compose.theme.EveColors

/**
 * Feature to which this request is attributed
 */
sealed class Originator(val name: String, val color: Color) {
    data object Assets : Originator("Assets", EveColors.burnishedGold)
    data object LocalCharacters : Originator("Character Tracking", EveColors.primaryBlue)
    data object Clones : Originator("Clones", EveColors.smokeBlue)
    data object Contacts : Originator("Contacts", EveColors.ultramarineBlue)
    data object CorporationProjects : Originator("Corporation Projects", EveColors.airTurquoise)
    data object FreelanceJobs : Originator("Freelance Jobs", EveColors.paragonBlue)
    data object Fleets : Originator("Fleets", EveColors.auraPurple)
    data object Autopilot : Originator("Autopilot", EveColors.destinationYellow)
    data object GameUi : Originator("Game UI", EveColors.cryoBlue)
    data object PlanetaryIndustry : Originator("Planetary Industry", EveColors.warningOrange)
    data object JumpBridgeSearch : Originator("Jump Bridge Search", EveColors.copperOxideGreen)
    data object Map : Originator("Map", EveColors.sandYellow)
    data object Wallets : Originator("Wallets", EveColors.evermarkGreen)
    data object Patrons : Originator("Patrons", EveColors.coalBlack)
    data object Alerts : Originator("Alerts", EveColors.duskyOrange)
    data object ChatLogs : Originator("Chat Logs Parsing", EveColors.iceWhite)
    data object Killmails : Originator("Killmails", EveColors.cherryRed)
    data object Pings : Originator("Jabber Pings", EveColors.white)
    data object DataPreloading : Originator("Data Preloading", EveColors.matteBlack)
    data object UiImage : Originator("UI Image", EveColors.matteBlack)
}
