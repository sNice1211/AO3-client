package net.pythonsden.ao3_.data.model

data class EpubMetadata(
    val title: String,
    val author: String,
    val seriesName: String?,
    val seriesPart: Int?,
    val summary: String? = null,
    val tags: List<String> = emptyList(),
    val chapterCount: Int = 0
)
