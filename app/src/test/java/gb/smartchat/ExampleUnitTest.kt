package gb.smartchat

import gb.smartchat.entity.Mention
import gb.smartchat.entity.User
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun getMentions() {
        val text = "@111 gf @222 @1@11cqawdaf@22@222 asdaf @11@1     "
        val users = listOf("111", "222").map { User(0, it, it, it) }
        val mentions = mutableListOf<Mention>()
        users.forEach { user ->
            if (user.name != null) {
                val targetText = "@${user.name}"
                val lengthUtf8 = targetText.encodeToByteArray().size
                var startIndex = 0

                var s = text
                while (s.contains(targetText, true)) {
                    val offset = text.indexOf(targetText, startIndex, true)
                    val offsetUtf8 = text.substring(0, offset).encodeToByteArray().size
                    mentions += Mention(
                        userId = user.id,
                        offsetUtf8 = offsetUtf8,
                        lengthUtf8 = lengthUtf8
                    )
                    println("while: $mentions")
                    startIndex = offset + targetText.length
                    println("startIndex: $startIndex")
                    s = text.substring(startIndex)
                }
            }
        }
        println("complete: $mentions")
    }

    //п - 0,1
    //р - 2,3
    //и - 4,5
    //в - 6,7

    @Test
    fun getAsd() {
        val string = "Привет"
        val offset = string.indexOf("вет")
        val offsetUtf8 = string.substring(offset).encodeToByteArray().size
        val lengthUtf8 = "вет".encodeToByteArray().size
        println("offsetUtf8: $offsetUtf8, lengthUtf8: $lengthUtf8")

        val mentionUtf8 = ByteArray(lengthUtf8)
        string.encodeToByteArray().copyInto(
            destination = mentionUtf8,
            startIndex = offsetUtf8,
            endIndex = offsetUtf8 + lengthUtf8
        )
        val mention = mentionUtf8.decodeToString()
        println(mention)

        val mentionOffset = string.indexOf(mention)
        val mentionLength = mention.length
        println("mentionOffset: $mentionOffset, mentionLength: $mentionLength")

    }
}
