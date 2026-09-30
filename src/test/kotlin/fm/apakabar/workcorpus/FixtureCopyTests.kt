package fm.apakabar.workcorpus

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FixtureCopyTests {
    private fun fetch(name: String): ByteArray {
        val address = "$LEAD/$name"
        val request =
            HttpRequest
                .newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val answer = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(
            OK,
            answer.statusCode(),
            "$address answered ${answer.statusCode()}: push workcorpus-swift if $name is new there, " +
                "or run `make sync-yaml` in workcorpus-swift if it is gone",
        )
        return answer.body()
    }

    @Test
    fun `every local copy is a shared fixture named in the list`() {
        assertEquals(
            SHARED.toSet(),
            Fixtures.copied(),
            "the copied fixtures differ from the list of shared ones: run `make sync-yaml` in workcorpus-swift and update the list",
        )
    }

    @TestFactory
    fun `every shared fixture is the leading port's own on main, byte for byte`(): List<DynamicTest> =
        SHARED.map { name ->
            DynamicTest.dynamicTest(name) {
                assertContentEquals(
                    fetch(name),
                    Fixtures.bytes(name),
                    "$name differs from workcorpus-swift main: run `make sync-yaml` there, or push workcorpus-swift " +
                        "if the change is there only locally",
                )
            }
        }

    companion object {
        private val SHARED = listOf("book-with-listening.yaml", "work-cases.yaml", "work.yaml")
        private const val LEAD =
            "https://raw.githubusercontent.com/apakabarlabs/workcorpus-swift/main/Tests/WorkCorpusTests/Fixtures"
        private const val OK = 200
        private const val TIMEOUT_SECONDS = 10L
        private val CLIENT: HttpClient = HttpClient.newHttpClient()
    }
}
