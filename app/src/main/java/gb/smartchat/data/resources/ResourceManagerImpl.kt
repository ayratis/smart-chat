package gb.smartchat.data.resources

import android.content.res.Resources

class ResourceManagerImpl(private val resources: Resources) : ResourceManager {

    override fun getString(stringRes: Int): String {
        return resources.getString(stringRes)
    }
}
