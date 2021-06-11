package gb.smartchat.library.ui.group_complete

import android.net.Uri
import gb.smartchat.library.entity.Chat
import gb.smartchat.library.entity.Contact
import gb.smartchat.library.entity.File
import gb.smartchat.library.ui._global.BaseStore

object GroupCompleteUDF {

    data class State(
        val groupName: String = "",
        val contacts: List<Contact>,
        val loading: Boolean = false,
        val avatarState: AvatarState = AvatarState.Empty
    )

    sealed class AvatarState {
        object Empty : AvatarState()
        data class Uploading(val uri: Uri) : AvatarState()
        data class UploadSuccess(val uri: Uri, val file: File) : AvatarState()
    }

    sealed class Action {
        data class GroupNameChanged(val name: String) : Action()
        data class DeleteContact(val contact: Contact) : Action()
        object CreateGroup : Action()
        data class CreateGroupSuccess(val chat: Chat) : Action()
        data class CreateGroupError(val error: Throwable) : Action()
        data class AttachAvatar(val contentUri: Uri) : Action()
        data class UploadAvatarSuccess(val contentUri: Uri, val file: File) : Action()
        data class UploadAvatarError(val contentUri: Uri, val error: Throwable) : Action()
    }

    sealed class SideEffect {
        data class CreateGroup(
            val contacts: List<Contact>,
            val groupName: String,
            val photoUrl: String?
        ) : SideEffect()

        data class ShowCreateGroupError(val error: Throwable) : SideEffect()
        data class NavigateToChat(val chat: Chat) : SideEffect()
        data class UploadAvatar(val contentUri: Uri) : SideEffect()
        data class ShowUploadAvatarError(val error: Throwable) : SideEffect()
    }

    class Store(selectedContacts: List<Contact>) :
        BaseStore<State, Action, SideEffect>(State(contacts = selectedContacts)) {

        override fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                is Action.CreateGroup -> {
                    sideEffectListener.invoke(
                        SideEffect.CreateGroup(
                            state.contacts,
                            state.groupName,
                            (state.avatarState as? AvatarState.UploadSuccess)?.file?.url
                        )
                    )
                    return state.copy(loading = true)
                }
                is Action.CreateGroupError -> {
                    sideEffectListener.invoke(SideEffect.ShowCreateGroupError(action.error))
                    return state.copy(loading = false)
                }
                is Action.CreateGroupSuccess -> {
                    sideEffectListener.invoke(SideEffect.NavigateToChat(action.chat))
                    return state.copy(loading = false)
                }
                is Action.DeleteContact -> {
                    val contacts = state.contacts.filter { it.id != action.contact.id }
                    return state.copy(contacts = contacts)
                }
                is Action.GroupNameChanged -> {
                    return state.copy(groupName = action.name)
                }
                is Action.AttachAvatar -> {
                    sideEffectListener.invoke(SideEffect.UploadAvatar(action.contentUri))
                    return state.copy(avatarState = AvatarState.Uploading(action.contentUri))
                }
                is Action.UploadAvatarError -> {
                    sideEffectListener.invoke(SideEffect.ShowUploadAvatarError(action.error))
                    return state.copy(avatarState = AvatarState.Empty)
                }
                is Action.UploadAvatarSuccess -> {
                    return state.copy(
                        avatarState = AvatarState.UploadSuccess(
                            action.contentUri,
                            action.file
                        )
                    )
                }
            }
        }
    }
}
