package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Serializable
data class WorkCase(
    @SerialName("case") val name: String,
    @SerialName("fixture") val source: String,
    val replace: String? = null,
    val with: String? = null,
    val refused: String? = null,
    val threshold: Int? = null,
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
                val document = shared.document()
                val work =
                    try {
                        shared.read(document)
                    } catch (refusal: WorkCorpus.WorkShapeError) {
                        return@dynamicTest assertEquals(shared.refused, refusal.message)
                    } catch (refusal: WorkCorpus.CorpusError) {
                        return@dynamicTest assertEquals(shared.refused, refusal.message)
                    } catch (refusal: WorkCorpus.WorkError) {
                        return@dynamicTest assertEquals(shared.refused, refusal.message)
                    }
                assertNull(shared.refused, "read, though the case expects: ${shared.refused}")
                shared.threshold?.let { assertEquals(it, work.difficultWords.scoreThreshold) }
            }
        }
}
