package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@Serializable
data class WorkCase(
    @SerialName("case") val name: String,
    @SerialName("fixture") val source: String,
    val replace: String? = null,
    val with: String? = null,
    val error: String? = null,
    val refused: String? = null,
    val read: WorkReadingCase? = null,
) {
    fun document(): String {
        val base = if (source == "json-book") WorkCases.shared().jsonBook else Fixtures.text(source)
        if (replace == null) return base
        assertEquals(2, base.split(replace).size, "$replace is not in $source once")
        return base.replace(replace, with.orEmpty())
    }

    fun read(document: String): Work =
        when (source) {
            "work" -> WorkCorpus.decodeWork(document)
            "book-with-listening" -> WorkCorpus.decodeWorkFromBook(document)
            "json-book" -> decodeJsonBook(document)
            else -> error("A shared case names $source, which is no fixture a work is read from.")
        }

    private fun decodeJsonBook(document: String): Work {
        val work = Json.decodeFromString(Work.serializer(), document)
        WorkCorpus.validate(work.pieces)
        WorkCorpus.validateConfiguration(work)
        return work
    }
}

@Serializable
data class WorkReadingCase(
    val numbers: List<Int>? = null,
    val titles: List<String>? = null,
    val lines: List<String>? = null,
    val parts: List<List<Int>>? = null,
    val free: List<Int>? = null,
    val bands: List<Double>? = null,
    val threshold: Int? = null,
    val language: String? = null,
    val cuts: Map<String, List<Int>>? = null,
    val shortTitles: List<String>? = null,
    val summaries: List<String>? = null,
) {
    fun check(work: Work) {
        language?.let { assertEquals(it, work.language) }
        cuts?.let { assertEquals(it, work.pieces.firstOrNull()?.cutSizes) }
        shortTitles?.let { assertEquals(it, work.parts.map { part -> part.shortTitle }) }
        summaries?.let { assertEquals(it, work.parts.map { part -> part.summary }) }
        numbers?.let { assertEquals(it, work.pieces.map { piece -> piece.number }) }
        titles?.let { assertEquals(it, work.pieces.map { piece -> piece.title }) }
        lines?.let { assertEquals(it, work.pieces.firstOrNull()?.lines) }
        parts?.let { assertEquals(it, work.parts.map { part -> listOf(part.first, part.last) }) }
        free?.let { assertEquals(it, work.free) }
        bands?.let {
            val scale = work.stageField
            assertEquals(it, listOf(scale.untouchedBelow, scale.begunBelow, scale.mostBelow))
        }
        threshold?.let { assertEquals(it, work.difficultWords.scoreThreshold) }
    }
}

@Serializable
data class WorkCases(
    @SerialName("json-book") val jsonBook: String,
    val cases: List<WorkCase>,
) {
    companion object {
        fun shared(): WorkCases = Yaml.default.decodeFromString(serializer(), Fixtures.text("work-cases"))
    }
}

class WorkCaseTests {
    @TestFactory
    fun `a work is read, or refused, as the shared cases say`(): List<DynamicTest> =
        WorkCases.shared().cases.map { shared ->
            DynamicTest.dynamicTest(shared.name) {
                assertEquals(shared.refused == null, shared.error == null, "a refusal names its error")
                assertNotEquals(shared.refused == null, shared.read == null, "a case is read or refused")
                val document = shared.document()
                val work =
                    try {
                        shared.read(document)
                    } catch (refusal: WorkCorpus.WorkShapeError) {
                        return@dynamicTest expect(refusal, shared)
                    } catch (refusal: WorkCorpus.CorpusError) {
                        return@dynamicTest expect(refusal, shared)
                    } catch (refusal: WorkCorpus.WorkError) {
                        return@dynamicTest expect(refusal, shared)
                    }
                assertNull(shared.refused, "read, though the case expects: ${shared.refused}")
                shared.read?.check(work)
            }
        }

    private fun expect(
        refusal: Exception,
        shared: WorkCase,
    ) {
        assertEquals(shared.error, refusal.javaClass.simpleName.replaceFirstChar { it.lowercase() })
        assertEquals(shared.refused, refusal.message)
    }
}
