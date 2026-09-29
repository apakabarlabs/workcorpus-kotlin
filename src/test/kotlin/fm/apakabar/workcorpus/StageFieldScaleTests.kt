package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StageFieldScaleTests {
    private val scale = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0)

    @Test
    fun `the configured bounds are exclusive`() {
        assertEquals(StageFieldScale.Band.UNTOUCHED, scale.band(0.0))
        assertEquals(StageFieldScale.Band.BEGUN, scale.band(0.001))
        assertEquals(StageFieldScale.Band.MOST, scale.band(0.5))
        assertEquals(StageFieldScale.Band.WHOLE, scale.band(1.0))
    }
}
