package fm.apakabar.workcorpus

import java.io.File

object Fixtures {
    fun bytes(name: String): ByteArray =
        checkNotNull(Fixtures::class.java.getResourceAsStream("/$name")) {
            "$name is missing: run `make sync-yaml` in workcorpus-swift"
        }.use { it.readBytes() }

    fun text(name: String): String = bytes("$name.yaml").decodeToString()

    fun work(): String = text("work")

    fun copied(): Set<String> {
        val directory =
            File(
                checkNotNull(Fixtures::class.java.getResource("/work.yaml")) {
                    "no fixtures: run `make sync-yaml` in workcorpus-swift"
                }.toURI(),
            ).parentFile
        return directory
            .listFiles { file -> file.isFile && file.name.endsWith(".yaml") }
            .orEmpty()
            .map { it.name }
            .toSet()
    }
}
