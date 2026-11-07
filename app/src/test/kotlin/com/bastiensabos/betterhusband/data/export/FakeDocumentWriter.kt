package com.bastiensabos.betterhusband.data.export

import java.io.IOException

class FakeDocumentWriter(
    var failing: Boolean = false,
) : DocumentWriter {
    val written = mutableMapOf<String, String>()

    override suspend fun write(
        uri: String,
        content: String,
    ) {
        if (failing) throw IOException("disk full")
        written[uri] = content
    }
}
