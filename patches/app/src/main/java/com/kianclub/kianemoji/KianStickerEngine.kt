package com.kianclub.kianemoji

data class KianSticker(
    val id: String,
    val title: String,
    val assetPath: String,
    val tags: Set<String>,
    val emotions: Set<String> = emptySet(),
    val characters: Set<String> = emptySet()
)

data class StickerMatch(
    val sticker: KianSticker,
    val score: Int,
    val matchedTags: List<String>
)

object KianPersianText {
    fun normalize(input: String): String =
        input.replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('\u200c', ' ')
            .replace(Regex("[ًٌٍَُِّْـ]"), "")
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

    fun tokens(input: String): Set<String> =
        normalize(input).split(' ').filter { it.length > 1 }.toSet()
}

object KianStickerEngine {
    private val emotionTerms = mapOf(
        "شادی" to setOf("خوشحال", "شاد", "عالی", "ذوق", "خندیدم"),
        "عشق" to setOf("عاشق", "دوستت دارم", "عزیزم", "مهربون", "وفادار"),
        "غم" to setOf("ناراحتم", "دلم گرفته", "غمگین", "دلتنگ", "گریه"),
        "ترس" to setOf("ترسیدم", "میترسم", "نگران", "وحشت", "استرس"),
        "خشم" to setOf("عصبانی", "حرص", "کلافه", "اعصاب"),
        "تشویق" to setOf("آفرین", "باریکلا", "موفق", "دمت گرم", "قهرمان"),
        "بازی" to setOf("بازی", "بیا", "بدو", "توپ", "شیطونی", "بپر")
    )

    fun detectEmotions(text: String): Map<String, Int> {
        val normalized = KianPersianText.normalize(text)
        return emotionTerms.mapNotNull { (emotion, terms) ->
            val count = terms.count {
                normalized.contains(KianPersianText.normalize(it))
            }
            if (count > 0) emotion to count else null
        }.sortedByDescending { it.second }.toMap()
    }

    fun suggest(
        text: String,
        catalogue: List<KianSticker>,
        limit: Int = 5
    ): List<StickerMatch> {
        if (text.isBlank() || catalogue.isEmpty() || limit <= 0)
            return emptyList()

        val normalized = KianPersianText.normalize(text)
        val tokens = KianPersianText.tokens(text)
        val emotions = detectEmotions(text).keys

        return catalogue.mapNotNull { sticker ->
            val matched = sticker.tags.filter {
                val tag = KianPersianText.normalize(it)
                tag.isNotBlank() &&
                    (tokens.contains(tag) || normalized.contains(tag))
            }
            val emotionHits = sticker.emotions.intersect(emotions)
            val score = matched.size * 3 + emotionHits.size * 4

            if (score == 0) null
            else StickerMatch(
                sticker,
                score,
                matched + emotionHits
            )
        }.sortedByDescending { it.score }.take(limit)
    }

    fun starterCatalogue() = listOf(
        KianSticker(
            "kian_happy", "کیان خوشحال",
            "stickers/kian/happy.webp",
            setOf("خوشحال", "شاد", "عالی", "آفرین", "خنده"),
            setOf("شادی", "تشویق"), setOf("kian")
        ),
        KianSticker(
            "dog_love", "عشق سگ‌ها",
            "stickers/dogs/love.webp",
            setOf("عشق", "دوستت دارم", "وفادار", "مهربون", "قلب"),
            setOf("عشق"), setOf("dog")
        ),
        KianSticker(
            "dog_sad", "سگ دلتنگ",
            "stickers/dogs/sad.webp",
            setOf("دلتنگ", "غمگین", "تنها", "ناراحتم"),
            setOf("غم"), setOf("dog")
        ),
        KianSticker(
            "dog_play", "وقت بازی",
            "stickers/dogs/play.webp",
            setOf("بازی", "توپ", "بدو", "شیطونی", "بپر"),
            setOf("بازی"), setOf("dog")
        ),
        KianSticker(
            "dog_brave", "سگ شجاع",
            "stickers/dogs/brave.webp",
            setOf("قهرمان", "شجاع", "موفق", "آفرین"),
            setOf("تشویق"), setOf("dog")
        )
    )
}
