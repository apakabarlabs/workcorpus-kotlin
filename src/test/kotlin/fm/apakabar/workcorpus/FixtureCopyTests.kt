package fm.apakabar.workcorpus

import org.junit.jupiter.api.DynamicTest
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
        assertEquals(OK, answer.statusCode(), "$address answered ${answer.statusCode()}")
        return answer.body()
    }

    @TestFactory
    fun `every shared fixture is the leading port's own, byte for byte`(): List<DynamicTest> =
        Fixtures.shared.map { name ->
            DynamicTest.dynamicTest(name) {
                assertContentEquals(
                    fetch(name),
                    Fixtures.bytes(name),
                    "$name differs from the leading port: run `make sync-yaml`",
                )
            }
        }

    companion object {
        private const val LEAD =
            "https://raw.githubusercontent.com/apakabarlabs/workcorpus-swift/main/Tests/WorkCorpusTests/Fixtures"
        private const val OK = 200
        private const val TIMEOUT_SECONDS = 10L
        private val CLIENT: HttpClient = HttpClient.newHttpClient()
    }
}
