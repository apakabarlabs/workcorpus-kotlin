package fm.apakabar.workcorpus

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

internal open class NumberCheckedSerializer<T>(
    private val generated: KSerializer<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor get() = generated.descriptor

    override fun deserialize(decoder: Decoder): T {
        if (decoder !is JsonDecoder) return generated.deserialize(decoder)
        val element = decoder.decodeJsonElement()
        refuseInexactNumbers(element, descriptor, place = "")
        return decoder.json.decodeFromJsonElement(generated, element)
    }

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = generated.serialize(encoder, value)
}

internal fun wholeNumber(element: JsonElement): Int? {
    val primitive = element as? JsonPrimitive
    if (primitive == null || primitive is JsonNull || primitive.isString) return null
    val value = primitive.content.toBigDecimalOrNull()?.stripTrailingZeros() ?: return null
    val negativeZero = value.signum() == 0 && primitive.content.startsWith("-")
    if (negativeZero || value.scale() > 0 || value !in INT_RANGE) return null
    return value.intValueExact()
}

private val INT_RANGE = BigDecimal(Int.MIN_VALUE)..BigDecimal(Int.MAX_VALUE)

private fun refuseInexactNumbers(
    element: JsonElement,
    descriptor: SerialDescriptor,
    place: String,
) {
    if (descriptor.serialName == WholeNumberSerializer.descriptor.serialName) {
        if (wholeNumber(element) == null) throw WorkCorpus.WorkShapeError.InvalidNumber(place)
        return
    }
    when (descriptor.kind) {
        StructureKind.CLASS ->
            (element as? JsonObject)?.let { fields ->
                for (index in 0 until descriptor.elementsCount) {
                    val name = descriptor.getElementName(index)
                    fields[name]?.let { refuseInexactNumbers(it, descriptor.getElementDescriptor(index), within(place, name)) }
                }
            }
        StructureKind.LIST ->
            (element as? JsonArray)?.forEachIndexed { index, item ->
                refuseInexactNumbers(item, descriptor.getElementDescriptor(0), "$place[$index]")
            }
        StructureKind.MAP ->
            (element as? JsonObject)?.forEach { (key, value) ->
                refuseInexactNumbers(value, descriptor.getElementDescriptor(1), within(place, key))
            }
        else -> Unit
    }
}

private fun within(
    place: String,
    name: String,
): String = if (place.isEmpty()) name else "$place.$name"
