package fm.apakabar.workcorpus

object Fixtures {
    val shared = listOf("book-with-listening.yaml", "work.yaml")

    fun bytes(name: String): ByteArray =
        checkNotNull(Fixtures::class.java.getResourceAsStream("/$name")) {
            "$name is missing: run `make sync-yaml`"
        }.use { it.readBytes() }

    fun text(name: String): String = bytes("$name.yaml").decodeToString()

    fun work(): String = text("work")
}
