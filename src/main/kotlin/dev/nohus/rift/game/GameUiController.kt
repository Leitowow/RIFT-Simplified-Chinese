package dev.nohus.rift.game

import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.compose.text.FormattedText
import dev.nohus.rift.compose.text.FormattedTextColor.Highlighted
import dev.nohus.rift.compose.text.FormattedTextColor.Secondary
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.compose.text.toFormattedText
import dev.nohus.rift.infodialog.InfoDialogInputModel
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.NewMailRequest
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class GameUiController(
    private val esiApi: EsiApi,
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val windowManager: WindowManager,
) {

    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Opens the Show Info window in-game, only works for characters, corporations, and alliances
     */
    fun openInfoWindow(
        entityId: Int,
    ) {
        scope.launch {
            val characterId = getTargetCharacter() ?: return@launch
            esiApi.postUiOpenWindowInformation(Originator.GameUi, characterId, entityId.toLong()).handleError()
        }
    }

    /**
     * Switches the already opened Market Details window in-game to the specified type.
     * Does nothing if no Market Details window is open.
     */
    fun updateMarketDetailsWindow(
        typeId: Long,
    ) {
        scope.launch {
            val characterId = getTargetCharacter() ?: return@launch
            esiApi.postUiOpenWindowMarketDetails(Originator.GameUi, characterId, typeId).handleError()
        }
    }

    fun pushType(type: Type, category: String) {
        pushLink(GameLink.forType(type), category)
    }

    fun pushSystem(system: MapSolarSystem) {
        pushLink(GameLink.forSystem(system), "solar system")
    }

    fun pushLocation(locationId: Long, locationTypeId: Int, locationName: String) {
        pushLink(GameLink.forLocation(locationId, locationTypeId, locationName), "location")
    }

    fun pushCorporationProject(id: String, name: String) {
        pushLink(GameLink.forCorporationProject(id, name), "corporation project")
    }

    fun pushFreelanceProject(id: String, name: String) {
        pushLink(GameLink.forFreelanceJob(id, name), "freelance project")
    }

    fun pushUrl(link: String, name: String) {
        pushLink("<url=$link>$name</url>", null)
    }

    private fun pushLink(link: String, category: String?) {
        val subject = if (category != null) {
            "${category.replaceFirstChar { it.titlecase(Locale.ENGLISH) }} link from RIFT Intel Fusion Tool"
        } else {
            "Link from RIFT Intel Fusion Tool"
        }
        val body = buildString {
            append("<font size=\"30\">")
            append("→ ")
            append(link)
            append(" ←")
            append("</font><br>")
            if (category != null) {
                append("<b>Click the link to view the $category</b><br>")
            } else {
                append("<b>Click the link to view</b><br>")
            }
            append("<br><br><br><br>")
            append("<font size=\"12\" color=\"#ff4c4c4c\">You can close this window. It's only here to let you click your link.</font><br><br>")
            append("<b><font color=\"#ffC3E9FF\">R</font>IFT <font color=\"#ffC3E9FF\">I</font>ntel <font color=\"#ffC3E9FF\">F</font>usion <font color=\"#ffC3E9FF\">T</font>ool</b>")
        }
        openNewMailWindow(subject, body)
    }

    fun openNewMailWindow(
        subject: String,
        body: String,
    ) {
        scope.launch {
            val characterId = getTargetCharacter() ?: return@launch
            val request = NewMailRequest(
                body = body,
                recipients = listOf(0),
                subject = subject,
            )
            esiApi.postUiOpenWindowNewMail(Originator.GameUi, characterId, request).handleError()
        }
    }

    private fun getTargetCharacter(): Int? {
        val onlineCharacterIds = onlineCharactersRepository.onlineCharacters.value
        val targetCharacterId = activeCharacterRepository.activeCharacter.value.takeIf { it in onlineCharacterIds }
            ?: onlineCharactersRepository.onlineCharacters.value.firstOrNull()
        if (targetCharacterId == null) {
            showError(
                title = "No Character Online",
                text = "Cannot open a window in-game because you are not in-game".toFormattedText(),
            )
            return null
        }
        val targetCharacter = localCharactersRepository.characters.value
            .firstOrNull { it.characterId == targetCharacterId }
            ?: return null
        if (ScopeGroups.openWindow !in targetCharacter.scopes) {
            showError(
                title = "Insufficient Permissions",
                text = buildFormattedText {
                    appendLine("Cannot open a window in-game because you haven't allowed RIFT to do so.")
                    appendLine()
                    append("You need to reauthenticate character ")
                    withColor(Highlighted) {
                        withWeight(FontWeight.Bold) {
                            append(targetCharacter.info?.name ?: targetCharacter.characterId.toString())
                        }
                    }
                    append(" in the Characters window.")
                },
            )
            return null
        }
        return targetCharacterId
    }

    private fun Result<Unit>.handleError() {
        if (this is Result.Failure) {
            showError(
                title = "Could Not Open Window",
                text = buildFormattedText {
                    appendLine("The ESI request has failed.")
                    appendLine()
                    append("Reason: ")
                    withColor(Secondary) {
                        append(this@handleError.cause?.message ?: "Unknown")
                    }
                },
            )
        }
    }

    private fun showError(
        title: String,
        text: FormattedText,
    ) {
        val inputModel = InfoDialogInputModel(
            title = title,
            text = text,
            isWarning = true,
        )
        windowManager.onWindowOpen(RiftWindow.InfoDialog, inputModel)
    }
}
