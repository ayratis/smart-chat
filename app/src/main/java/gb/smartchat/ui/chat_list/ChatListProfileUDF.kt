package gb.smartchat.ui.chat_list

import gb.smartchat.entity.StoreInfo
import gb.smartchat.entity.UserProfile
import gb.smartchat.ui._global.BaseStore

object ChatListProfileUDF {
    sealed class State {
        object Empty : State()
        object Loading : State()
        data class Success(val userProfile: UserProfile) : State()
        data class Error(val error: Throwable) : State()
    }

    sealed class Action {
        object Refresh : Action()
        data class ProfileSuccess(val userProfile: UserProfile) : Action()
        data class ProfileError(val error: Throwable) : Action()
        object CreateChat : Action()
        data class AvatarChanged(val url: String) : Action()

    }

    sealed class SideEffect {
        object LoadUserProfile : SideEffect()
        data class ShowProfileLoadError(val error: Throwable) : SideEffect()
        data class NavToCreateChat(
            val storeInfo: StoreInfo?,
            val userProfile: UserProfile
        ) : SideEffect()
    }

    class Store : BaseStore<State, Action, SideEffect>(State.Empty) {
        override fun reduce(
            state: State,
            action: Action,
            sideEffectListener: (SideEffect) -> Unit
        ): State {
            when (action) {
                Action.Refresh -> {
                    return when (state) {
                        is State.Empty,
                        is State.Error -> {
                            sideEffectListener.invoke(SideEffect.LoadUserProfile)
                            State.Loading
                        }
                        is State.Loading,
                        is State.Success -> {
                            state
                        }
                    }
                }
                is Action.ProfileSuccess -> {
                    return State.Success(action.userProfile)
                }
                is Action.ProfileError -> {
                    sideEffectListener(SideEffect.ShowProfileLoadError(action.error))
                    return State.Error(action.error)
                }
                Action.CreateChat -> {
                    if (state is State.Success) {
                        sideEffectListener.invoke(
                            SideEffect.NavToCreateChat(
                                null, //todo
                                state.userProfile
                            )
                        )
                    }
                    return state
                }
                is Action.AvatarChanged -> {
                    if (state is State.Success) {
                        return state.copy(
                            userProfile = state.userProfile.copy(
                                avatar = action.url
                            )
                        )
                    }
                    return state
                }
            }
        }
    }
}
