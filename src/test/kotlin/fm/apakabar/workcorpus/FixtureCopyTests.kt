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
    private val tag = "v${checkNotNull(System.getProperty("workcorpus.version")) { "the build passes no version" }}"

    private fun fetch(address: String): ByteArray {
        val request =
            HttpRequest
                .newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val answer = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(
            OK,
            answer.statusCode(),
            "$address answered ${answer.statusCode()}: if workcorpus-swift has no $tag yet, push the lead and tag it $tag",
        )
        return answer.body()
    }

    private fun leadFixtures(): Set<String> {
        val listing = fetch("$LISTING?ref=$tag").decodeToString()
        return Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(listing).map { it.groupValues[1] }.toSet()
    }

    @TestFactory
    fun `every fixture is the leading port's own at the tag of this version, byte for byte`(): List<DynamicTest> {
        val lead = leadFixtures()
        val set =
            DynamicTest.dynamicTest("the copied fixtures are the ones $tag has") {
                assertEquals(lead, Fixtures.copied(), "the fixtures differ from workcorpus-swift $tag: run `make sync-yaml`")
            }
        return listOf(set) +
            lead.sorted().map { name ->
                DynamicTest.dynamicTest(name) {
                    assertContentEquals(
                        fetch("$RAW/$tag/$DIRECTORY/$name"),
                        Fixtures.bytes(name),
                        "$name differs from workcorpus-swift $tag: run `make sync-yaml`",
                    )
                }
            }
    }

    companion object {
        private const val DIRECTORY = "Tests/WorkCorpusTests/Fixtures"
        private const val RAW = "https://raw.githubusercontent.com/apakabarlabs/workcorpus-swift"
        private const val LISTING = "https://api.github.com/repos/apakabarlabs/workcorpus-swift/contents/$DIRECTORY"
        private const val OK = 200
        private const val TIMEOUT_SECONDS = 10L
        private val CLIENT: HttpClient = HttpClient.newHttpClient()
    }
}
