package dev.nohus.rift.contacts

import dev.nohus.rift.DataEvent
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class ContactsExternalControl(
    private val windowManager: WindowManager,
) {

    private val _event = MutableStateFlow<DataEvent<ContactsExternalControlEvent>?>(null)
    val event = _event.asStateFlow()

    sealed interface ContactsExternalControlEvent {
        data class Edit(val id: Int, val type: EntityType, val uuid: UUID = UUID.randomUUID()) : ContactsExternalControlEvent
    }

    fun editContact(id: Int, type: EntityType) {
        windowManager.onWindowOpen(RiftWindow.Contacts, ifClosed = true)
        _event.tryEmit(DataEvent(ContactsExternalControlEvent.Edit(id, type)))
    }
}
