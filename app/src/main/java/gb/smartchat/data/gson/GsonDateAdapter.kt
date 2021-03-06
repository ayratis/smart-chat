package gb.smartchat.data.gson

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class GsonDateAdapter : JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

    override fun serialize(
        src: ZonedDateTime,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.toInstant().epochSecond)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(json.asLong), ZoneId.systemDefault())
    }
}
