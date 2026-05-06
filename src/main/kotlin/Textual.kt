package fr.univ_lille.iut_info

interface TextualRangeLocated {
    var textual: TextualRange?
}

data class TextualLocation(val filename: String, val line: Int, val row: Int)

data class TextualRange(val begin: TextualLocation, val end: TextualLocation) {
    init {
        assert(begin.filename == end.filename)
    }

    val filename
        get() = begin.filename
}