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
        val text = "hi @111 and @234"
        val users = listOf("111", "234").map { User(0, it, it, it) }
        val mentions = mutableListOf<Mention>()
        users.forEach { user ->
            if (user.name != null) {
                val targetText = "@${user.name}"
                var startIndex = 0
                var s = text
                while (s.contains(targetText, true)) {
                    val offset = text.indexOf(targetText, startIndex, true)
                    mentions += Mention(
                        userId = user.id,
                        offset = offset,
                        length = targetText.length
                    )
                    println("while: $mentions")
                    startIndex = offset + targetText.length - 1
                    s = s.substring(startIndex)
                }
            }
        }
        println("complete: $mentions")
    }
}
