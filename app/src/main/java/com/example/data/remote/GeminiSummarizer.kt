package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.Article
import com.example.data.model.ParsedArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiSummarizer(
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    val modelName = "gemini-3.1-flash-lite-preview"
    private val candidateModels = listOf(
        "gemini-3.1-flash-lite-preview",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash"
    )

    suspend fun summarize(article: Article, parsed: ParsedArticle?): Result<String> = withContext(Dispatchers.IO) {
        val articleTitle = parsed?.title?.takeIf { it.isNotBlank() } ?: article.title
        val paragraphsText = parsed?.paragraphs
            ?.filter {
                it.type == com.example.data.model.ParagraphType.BODY ||
                    it.type == com.example.data.model.ParagraphType.HEADING_1 ||
                    it.type == com.example.data.model.ParagraphType.HEADING_2 ||
                    it.type == com.example.data.model.ParagraphType.LIST_ITEM ||
                    it.type == com.example.data.model.ParagraphType.BLOCKQUOTE
            }
            ?.joinToString("\n") { it.text }
            ?: ""

        var articleBody = paragraphsText.ifBlank { parsed?.cleanText ?: "" }
        if (articleBody.isBlank()) {
            articleBody = article.cleanContent.ifBlank { article.snippet }
        }

        if (articleBody.isBlank()) {
            return@withContext Result.failure(Exception("Článok neobsahuje dostatok textu na vytvorenie zhrnutia."))
        }

        val trimmedBody = if (articleBody.length > 12000) articleBody.take(12000) + "..." else articleBody

        // If Gemini API key is configured in BuildConfig, call Gemini API (GEMINI-3.1-FLASH-LITE)
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val promptText = """
                Si špičkový slovenský analytik a editor správ. Vytvor výstižné a vysoko kvalitné zhrnutie tohto článku v slovenskom jazyku.
                
                TVOJA ÚLOHA:
                1. Vypichni ÚPLNÚ PODSTATU článku (hlavnú pointu, čo presne sa stalo, prečo je to dôležité a aký je kľúčový záver) a daj ju do PRVEJ ČASTI zhrnutia. Nepoužívaj prázdne frázy ako "Článok hovorí o..." – choď rovno k jadru veci.
                2. Do DRUHEJ ČASTI pridaj MAXIMÁLNE 3 doplňujúce myšlienky (kľúčové súvislosti, dôležité fakty, čísla alebo dôsledky), ktoré rozširujú prvú časť a neopakujú ju.
                
                STRIKTNÉ PRAVIDLÁ PRE FORMÁTOVANIE (POVINNÉ):
                - Nepoužívaj žiadne Markdown hviezdičky (ani **, ani *).
                - Používaj výhradne značku <b>...</b> pre nadpisy a kľúčové pojmy a odrážky '• '.
                - Dodrž presne túto dvojčlennú štruktúru:
                
                <b>📌 Podstata článku:</b>
                (1 až 2 jasné, obsažné a úderné vety s úplnou podstatou a pointou článku)
                
                <b>⚡ Doplňujúce myšlienky:</b>
                • (1. dôležitá doplňujúca myšlienka / konkrétny fakt)
                • (2. dôležitá doplňujúca myšlienka / súvislosť)
                • (3. dôležitá doplňujúca myšlienka / dôsledok – uveď max. 3 odrážky!)
                
                Názov článku: $articleTitle
                Zdroj: ${article.sourceName}
                
                Text článku:
                $trimmedBody
            """.trimIndent()

            val json = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.85)
                    put("maxOutputTokens", 550)
                })
            }

            for (candidateModel in candidateModels) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$candidateModel:generateContent?key=$apiKey"
                    val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(endpoint)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBodyStr = response.body?.string() ?: ""
                    response.close()

                    if (response.isSuccessful && responseBodyStr.isNotBlank()) {
                        val rootObj = JSONObject(responseBodyStr)
                        val candidates = rootObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val generatedText = parts.getJSONObject(0).optString("text", "")
                                if (generatedText.isNotBlank()) {
                                    return@withContext Result.success(cleanHtmlSummary(generatedText))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Intelligent local extractive summary fallback (also structured into Core Essence + max 3 supplementary thoughts)
        val localSummary = generateLocalExtractiveSummary(articleTitle, article.sourceName, articleBody)
        Result.success(cleanHtmlSummary(localSummary))
    }

    fun cleanHtmlSummary(raw: String): String {
        var text = raw.trim()
        // Convert any residual markdown bold **text** to <b>text</b>
        text = text.replace(Regex("""\*\*(.*?)\*\*"""), "<b>$1</b>")
        // Convert any residual *text* to <i>$1</i>
        text = text.replace(Regex("""\*(.*?)\*"""), "<i>$1</i>")
        // Convert markdown bullet points * or - into clean unicode bullet •
        text = text.replace(Regex("""(?m)^\s*[\*\-]\s+"""), "• ")
        // Remove any remaining stray asterisks
        text = text.replace("*", "")

        // Enforce maximum of 3 bullet points in the summary
        val lines = text.lines()
        val filteredLines = mutableListOf<String>()
        var bulletCount = 0
        for (line in lines) {
            val trimmed = line.trim()
            val isBullet = trimmed.startsWith("•") || trimmed.matches(Regex("^[1-9][.)]\\s+.*"))
            if (isBullet) {
                if (bulletCount < 3) {
                    val bulletBody = if (trimmed.startsWith("•")) {
                        trimmed.removePrefix("•").trim()
                    } else {
                        trimmed.replaceFirst(Regex("^[1-9][.)]\\s+"), "").trim()
                    }
                    filteredLines.add("• $bulletBody")
                    bulletCount++
                }
            } else {
                filteredLines.add(line)
            }
        }
        return filteredLines.joinToString("\n").trim()
    }

    fun generateLocalExtractiveSummary(title: String, source: String, body: String): String {
        val rawParagraphs = body
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("<[^>]*>"), " ")
            .split("\n")
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.length >= 30 && !isBoilerplateText(it) }

        val candidateSentences = mutableListOf<String>()
        for (para in rawParagraphs) {
            val split = para.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter {
                    it.length in 38..240 &&
                        !isBoilerplateText(it) &&
                        !it.endsWith("?")
                }
            candidateSentences.addAll(split)
        }

        val distinctSentences = candidateSentences.distinctBy { it.lowercase().take(45) }
        if (distinctSentences.isEmpty()) {
            val fallback = body.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim().take(240)
            return """
                <b>📌 Podstata článku:</b>
                ${fallback.ifBlank { title }}
            """.trimIndent()
        }

        val stopWords = setOf(
            "ako", "ale", "alebo", "ani", "áno", "aby", "ak", "až", "bez", "bol", "bola", "boli", "bolo", "bude",
            "budú", "byť", "cez", "či", "čo", "ďalšie", "dnes", "do", "ešte", "ho", "ich", "iba", "iná", "iné",
            "iný", "je", "jeho", "jej", "len", "má", "majú", "mal", "mala", "mali", "medzi", "menej", "mi", "môže",
            "môžu", "ku", "na", "nad", "nám", "nás", "než", "nie", "nich", "nim", "no", "od", "okolo", "on", "ona",
            "oni", "po", "pod", "podľa", "pokiaľ", "potom", "pre", "pred", "preto", "pretože", "pri", "proti", "sa",
            "si", "sme", "so", "sú", "svoj", "svoje", "svojich", "tak", "takže", "tam", "teda", "ten", "tento",
            "tie", "tiež", "to", "toho", "tohto", "tom", "tomto", "toto", "tu", "tú", "tým", "už", "šak", "však",
            "všetky", "všetko", "vo", "za", "zo", "že", "ktorý", "ktorá", "ktoré", "ktorí", "ktorých"
        )

        fun stemWord(word: String): String =
            if (word.length > 5) word.take(word.length - 2) else word

        fun tokenize(input: String): List<String> =
            input.lowercase()
                .replace(Regex("[^a-záäčďéíĺľňóôŕšťúýž0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length >= 3 && it !in stopWords }
                .map { stemWord(it) }

        val titleStems = tokenize(title).toSet()
        val stemFreq = mutableMapOf<String, Int>()
        distinctSentences.forEach { sentence ->
            tokenize(sentence).forEach { stem ->
                stemFreq[stem] = (stemFreq[stem] ?: 0) + 1
            }
        }

        data class ScoredSentence(
            val index: Int,
            val text: String,
            val score: Double,
            val stems: Set<String>
        )

        val scored = distinctSentences.mapIndexed { index, sentence ->
            val tokens = tokenize(sentence)
            val stems = tokens.toSet()
            val titleOverlap = stems.count { it in titleStems }.toDouble()
            val centrality = tokens.sumOf { stemFreq[it] ?: 1 }.toDouble() / tokens.size.coerceAtLeast(1)
            val positionWeight = when (index) {
                0 -> 3.4
                1 -> 2.6
                2 -> 1.9
                3 -> 1.4
                else -> 1.0 / (1.0 + index * 0.1)
            }
            val factBonus = if (sentence.contains(Regex("\\d+"))) 0.75 else 0.0
            val lengthMultiplier = when {
                sentence.length in 55..175 -> 1.15
                sentence.length in 40..210 -> 1.0
                else -> 0.82
            }
            val totalScore = ((titleOverlap * 2.8) + (centrality * 0.95) + positionWeight + factBonus) * lengthMultiplier
            ScoredSentence(index, sentence, totalScore, stems)
        }

        // 1. Core essence: Pick the strongest sentence from the lead/top-scoring sentences
        val topLeadCandidates = scored.take(5).sortedByDescending { it.score }
        val primaryEssence = topLeadCandidates.firstOrNull() ?: scored.first()
        val usedIndices = mutableSetOf(primaryEssence.index)

        // If the primary essence sentence is very short (< 95 chars), pair it with the best complementary lead sentence
        val secondaryEssence = if (primaryEssence.text.length < 95) {
            scored.take(4).firstOrNull { candidate ->
                candidate.index != primaryEssence.index &&
                    similarity(candidate.stems, primaryEssence.stems) < 0.5
            }
        } else null

        val essenceText = if (secondaryEssence != null) {
            usedIndices.add(secondaryEssence.index)
            listOf(primaryEssence, secondaryEssence)
                .sortedBy { it.index }
                .joinToString(" ") { it.text }
        } else {
            primaryEssence.text
        }

        // 2. Supplementary thoughts: Pick at most 3 distinct, non-redundant high-value sentences
        val supplementary = mutableListOf<ScoredSentence>()
        for (candidate in scored.filter { it.index !in usedIndices }.sortedByDescending { it.score }) {
            if (supplementary.size >= 3) break
            val redundantWithEssence = similarity(candidate.stems, primaryEssence.stems) > 0.6
            val redundantWithPicked = supplementary.any { picked ->
                similarity(candidate.stems, picked.stems) > 0.55
            }
            if (!redundantWithEssence && !redundantWithPicked) {
                supplementary.add(candidate)
            }
        }

        val orderedBullets = supplementary
            .sortedBy { it.index }
            .take(3)
            .joinToString("\n") { "• ${it.text}" }

        return if (orderedBullets.isNotBlank()) {
            """
            <b>📌 Podstata článku:</b>
            $essenceText
            
            <b>⚡ Doplňujúce myšlienky:</b>
            $orderedBullets
            """.trimIndent()
        } else {
            """
            <b>📌 Podstata článku:</b>
            $essenceText
            """.trimIndent()
        }
    }

    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val minSize = minOf(a.size, b.size).coerceAtLeast(1)
        return intersection / minSize
    }

    private fun isBoilerplateText(text: String): Boolean {
        val lower = text.lowercase()
        val ignored = listOf(
            "foto:", "zdroj:", "tasr", "sita", "reuters", "čítajte viac", "čítajte aj",
            "pozrite si", "odoberajte", "newsletter", "všetky práva vyhradené",
            "súhlas so spracovaním", "cookies", "reklama", "inzercia", "autor:"
        )
        return ignored.any { lower.startsWith(it) || lower.contains("čítajte viac") || lower.contains("všetky práva vyhradené") }
    }
}
