package com.example.data.remote

import com.example.data.model.ParagraphType
import com.example.data.model.ParsedArticle
import com.example.data.model.ReaderParagraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class ArticleExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    suspend fun extract(
        url: String,
        fallbackTitle: String,
        fallbackContent: String,
        sourceName: String,
        fallbackImageUrl: String? = null
    ): ParsedArticle = withContext(Dispatchers.IO) {
        var html: String? = null
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "sk,cs;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val headerCharset = response.body?.contentType()?.charset()?.name()
                        html = decodeHtmlBytes(bytes, headerCharset)
                    }
                }
                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!html.isNullOrBlank()) {
            try {
                val parsed = parseWebHtml(html, url, sourceName, fallbackImageUrl)
                if (parsed.paragraphs.isNotEmpty()) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (fallbackContent.contains("<html", ignoreCase = true) ||
            fallbackContent.contains("<body", ignoreCase = true) ||
            fallbackContent.contains("<article", ignoreCase = true) ||
            fallbackContent.contains("<p", ignoreCase = true) ||
            fallbackContent.contains("<h2", ignoreCase = true) ||
            fallbackContent.contains("<ul", ignoreCase = true) ||
            fallbackContent.contains("<ol", ignoreCase = true)
        ) {
            try {
                val parsed = parseWebHtml(fallbackContent, url, sourceName, fallbackImageUrl)
                if (parsed.paragraphs.isNotEmpty()) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fallbackParsedArticle(fallbackTitle, fallbackContent, sourceName, url, fallbackImageUrl)
    }

    private fun decodeHtmlBytes(bytes: ByteArray, headerCharset: String?): String {
        if (!headerCharset.isNullOrBlank()) {
            try {
                return String(bytes, java.nio.charset.Charset.forName(headerCharset))
            } catch (_: Exception) {}
        }
        val asciiProbe = String(bytes, 0, minOf(bytes.size, 2048), Charsets.ISO_8859_1)
        val metaCharsetMatch = Regex("""charset\s*=\s*["']?([a-zA-Z0-9_\-]+)""", RegexOption.IGNORE_CASE).find(asciiProbe)
        val detectedCharset = metaCharsetMatch?.groupValues?.getOrNull(1)?.trim()
        if (!detectedCharset.isNullOrBlank()) {
            try {
                return String(bytes, java.nio.charset.Charset.forName(detectedCharset))
            } catch (_: Exception) {}
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun parseWebHtml(
        html: String,
        url: String,
        sourceName: String,
        fallbackHeroImage: String?
    ): ParsedArticle {
        val doc: Document = Jsoup.parse(html, url)

        // Remove clutter, inline widgets, embeds, and ad placeholders
        doc.select(
            "script, style, noscript, nav, header, footer, aside, form, iframe, button, svg, canvas, video, audio, " +
                "[role='banner'], [role='navigation'], [aria-hidden='true'], [data-nosnippet], " +
                ".advert, .advertisement, .ads, .ad-banner, .ad-box, .ad-container, .cookie-banner, " +
                ".social-share, .share-buttons, .newsletter, .comments, .related-articles, .related, " +
                ".read-more, .also-read, .inline-related, .popup, .paywall, .piano, .locked-content, " +
                ".author-box, .tags, .breadcrumbs, .twitter-tweet, .instagram-media, .tiktok-embed, .fb-post"
        ).remove()

        // Ensure <br> tags produce whitespace/line breaks instead of gluing words together
        doc.select("br").after("\n")

        val rawTitle = doc.select("meta[property='og:title']").attr("content").ifBlank {
            doc.select("h1").first()?.text() ?: doc.title()
        }
        val title = cleanArticleText(rawTitle)

        val heroImage = doc.select("meta[property='og:image']").attr("content").ifBlank {
            fallbackHeroImage ?: ""
        }.trim()

        val author = cleanArticleText(
            doc.select("meta[name='author']").attr("content").ifBlank {
                doc.select("[itemprop='author'], .author, .byline").first()?.text() ?: ""
            }
        )

        val pubDate = cleanArticleText(
            doc.select("meta[property='article:published_time']").attr("content").ifBlank {
                doc.select("time").first()?.text() ?: ""
            }
        )

        val container = findArticleContainer(doc)
        val rawParagraphs = mutableListOf<ReaderParagraph>()

        val normHeroImage = normalizeImageUrl(heroImage)
        val seenImages = mutableSetOf<String>()
        if (normHeroImage.isNotBlank()) {
            seenImages.add(normHeroImage)
        }

        // Select content block elements in document order
        val elements = container.select("h2, h3, h4, h5, p, blockquote, ul, ol, figure, picture, div.image, div.media, div.wp-block-image")
        val processedImageElements = mutableSetOf<Element>()

        for (el in elements) {
            val tagName = el.tagName().lowercase()

            // 1. Check if this element contains an image or is an image container
            val imgEl = if (tagName == "figure" || tagName == "picture" || el.hasClass("image") || el.hasClass("media") || el.hasClass("wp-block-image")) {
                el.select("img").first()
            } else if (tagName == "p" && el.select("img").isNotEmpty() && el.text().trim().length < 40) {
                // <p> containing an image and minimal or no text
                el.select("img").first()
            } else {
                null
            }

            if (imgEl != null && !processedImageElements.contains(imgEl)) {
                processedImageElements.add(imgEl)
                var imgSrc = (imgEl.attr("data-src").ifEmpty {
                    imgEl.attr("data-original").ifEmpty {
                        imgEl.attr("data-lazy-src").ifEmpty {
                            imgEl.attr("srcset").split(",").firstOrNull()?.trim()?.split(" ")?.firstOrNull() ?: ""
                        }.ifEmpty {
                            imgEl.absUrl("src").ifEmpty { imgEl.attr("src") }
                        }
                    }
                }).trim()

                if (imgSrc.startsWith("//")) {
                    imgSrc = "https:$imgSrc"
                }

                if (imgSrc.startsWith("http") && isValidArticleImage(imgSrc)) {
                    val norm = normalizeImageUrl(imgSrc)
                    if (norm.isNotBlank() && !seenImages.contains(norm)) {
                        seenImages.add(norm)
                        val rawCaption = el.select("figcaption, .caption, .credit, .photo-caption").text().trim()
                            .ifBlank { imgEl.attr("alt").trim() }
                            .ifBlank { imgEl.attr("title").trim() }
                        val caption = cleanArticleText(rawCaption)
                        rawParagraphs.add(ReaderParagraph(ParagraphType.IMAGE, "", imgSrc, caption.ifBlank { null }))
                    }
                }
                // If it was a dedicated figure/image element, do not process as text
                if (tagName == "figure" || tagName == "picture" || el.hasClass("image") || el.hasClass("media")) {
                    continue
                }
            }

            // 2. Headings, lists and text content
            when (tagName) {
                "h2" -> {
                    val text = cleanArticleText(el.text())
                    if (text.length > 2 && !isBoilerplateText(text) && !text.equals(title, ignoreCase = true)) {
                        rawParagraphs.add(ReaderParagraph(ParagraphType.HEADING_1, text))
                    }
                }
                "h3", "h4", "h5" -> {
                    val text = cleanArticleText(el.text())
                    if (text.length > 2 && !isBoilerplateText(text) && !text.equals(title, ignoreCase = true)) {
                        rawParagraphs.add(ReaderParagraph(ParagraphType.HEADING_2, text))
                    }
                }
                "p" -> {
                    // Skip if this <p> is inside a <figure>, <li>, or <blockquote> that is processed separately
                    if (el.parents().any {
                            val pt = it.tagName().lowercase()
                            pt == "figure" || pt == "li" || pt == "blockquote"
                        }
                    ) {
                        continue
                    }
                    val subLines = el.wholeText().split(Regex("\\n+")).map { cleanArticleText(it) }.filter { it.isNotBlank() }
                    if (subLines.isEmpty()) continue

                    val fullCleanText = cleanArticleText(el.text())
                    val boldText = cleanArticleText(el.select("strong, b").text())
                    val isStandaloneBoldHeading = subLines.size == 1 &&
                        boldText.isNotEmpty() &&
                        boldText == fullCleanText &&
                        fullCleanText.length in 3..95 &&
                        !fullCleanText.endsWith(".")

                    if (isStandaloneBoldHeading && !isBoilerplateText(fullCleanText)) {
                        rawParagraphs.add(ReaderParagraph(ParagraphType.HEADING_2, fullCleanText))
                    } else {
                        for (line in subLines) {
                            if (line.isNotBlank() && !isBoilerplateText(line)) {
                                rawParagraphs.add(ReaderParagraph(ParagraphType.BODY, line))
                            }
                        }
                    }
                }
                "blockquote" -> {
                    val text = cleanArticleText(el.text())
                    if (text.isNotBlank() && !isBoilerplateText(text)) {
                        rawParagraphs.add(ReaderParagraph(ParagraphType.BLOCKQUOTE, text))
                    }
                }
                "ul" -> {
                    val items = el.select("> li").ifEmpty { el.select("li") }
                    for (li in items) {
                        val text = cleanArticleText(li.text())
                            .replaceFirst(Regex("^[•▪◦\\-–—*]\\s+"), "")
                            .trim()
                        if (text.isNotBlank() && !isBoilerplateText(text)) {
                            rawParagraphs.add(ReaderParagraph(ParagraphType.LIST_ITEM, text, caption = "•"))
                        }
                    }
                }
                "ol" -> {
                    val startAttr = el.attr("start").toIntOrNull() ?: 1
                    val items = el.select("> li").ifEmpty { el.select("li") }
                    items.forEachIndexed { index, li ->
                        val text = cleanArticleText(li.text())
                            .replaceFirst(Regex("^\\d+[.)]\\s+"), "")
                            .trim()
                        if (text.isNotBlank() && !isBoilerplateText(text)) {
                            val marker = "${startAttr + index}."
                            rawParagraphs.add(ReaderParagraph(ParagraphType.LIST_ITEM, text, caption = marker))
                        }
                    }
                }
            }
        }

        // Clean, normalize and deduplicate paragraphs before returning
        val paragraphs = sanitizeParagraphs(rawParagraphs, normHeroImage)

        val allText = paragraphs.joinToString(" ") { it.text }
        val wordCount = allText.split("\\s+".toRegex()).size
        val readMinutes = maxOf(1, wordCount / 200)

        return ParsedArticle(
            title = title,
            author = author.ifBlank { null },
            publishedDate = pubDate.ifBlank { null },
            siteName = sourceName,
            sourceUrl = url,
            heroImageUrl = heroImage.ifBlank { null },
            estimatedReadMinutes = readMinutes,
            paragraphs = paragraphs,
            wordCount = wordCount
        )
    }

    private fun findArticleContainer(doc: Document): Element {
        val candidates = listOf(
            doc.select("article").first(),
            doc.select("[itemprop='articleBody']").first(),
            doc.select(".article-content, .article__content, .entry-content, .post-content, .story-body, .article-body, .c-article__content").first(),
            doc.select("main").first()
        )
        for (candidate in candidates) {
            if (candidate != null && candidate.select("p").size >= 2) {
                return candidate
            }
        }
        return doc.body()
    }

    private fun fallbackParsedArticle(
        title: String,
        content: String,
        sourceName: String,
        url: String,
        heroImageUrl: String?
    ): ParsedArticle {
        // Preserve HTML block boundaries and explicit newlines before stripping HTML tags
        val withNewlines = content
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|h1|h2|h3|h4|h5|li|blockquote)>"), "\n\n")
            .replace("\r\n", "\n")

        val doc = Jsoup.parse(withNewlines)
        doc.outputSettings().prettyPrint(false)
        val rawTextWithBreaks = doc.wholeText()

        val initialBlocks = rawTextWithBreaks
            .split(Regex("\\n\\s*\\n|\\n"))
            .map { cleanArticleText(it) }
            .filter { it.isNotBlank() && !isBoilerplateText(it) }

        // If a single block is a huge legacy flattened wall of text (> 480 chars), split it into natural 3-sentence paragraphs
        val expandedBlocks = mutableListOf<String>()
        for (block in initialBlocks) {
            if (block.length > 480 && initialBlocks.size <= 2) {
                val sentences = block.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
                if (sentences.size >= 4) {
                    val chunk = StringBuilder()
                    var count = 0
                    for (s in sentences) {
                        if (chunk.isNotEmpty()) chunk.append(" ")
                        chunk.append(s)
                        count++
                        if (count >= 3 || chunk.length >= 320) {
                            expandedBlocks.add(chunk.toString().trim())
                            chunk.clear()
                            count = 0
                        }
                    }
                    if (chunk.isNotEmpty()) {
                        expandedBlocks.add(chunk.toString().trim())
                    }
                } else {
                    expandedBlocks.add(block)
                }
            } else {
                expandedBlocks.add(block)
            }
        }

        val rawList = expandedBlocks.map { ReaderParagraph(ParagraphType.BODY, it) }
        val paragraphs = sanitizeParagraphs(rawList, heroImageUrl)
        val cleanFull = paragraphs.joinToString("\n\n") { it.text }

        return ParsedArticle(
            title = cleanArticleText(title),
            author = null,
            publishedDate = null,
            siteName = sourceName,
            sourceUrl = url,
            heroImageUrl = heroImageUrl,
            estimatedReadMinutes = maxOf(1, cleanFull.length / 800),
            paragraphs = paragraphs,
            wordCount = cleanFull.split("\\s+".toRegex()).count { it.isNotBlank() }
        )
    }

    companion object {
        /**
         * Cleans text glitches from article paragraphs and titles:
         * - Unescapes double-encoded HTML entities (&amp;nbsp;, &#8222;, &shy;, etc.)
         * - Strips soft hyphens (\u00AD) and zero-width spaces (\u200B..\u200D, \uFEFF, \u2060)
         * - Converts non-breaking spaces (\u00A0, \u202F) to normal spaces so words wrap cleanly
         * - Fixes missing space after sentence punctuation (e.g. "koniec.Začiatok" -> "koniec. Začiatok")
         * - Strips inline ad/promo placeholder phrases
         */
        fun cleanArticleText(raw: String): String {
            if (raw.isBlank()) return ""
            var text = raw

            // 1. Decode HTML entities (run twice in case of double-encoded &amp;nbsp; or &amp;quot;)
            if (text.contains('&')) {
                try {
                    text = org.jsoup.parser.Parser.unescapeEntities(text, false)
                    if (text.contains('&')) {
                        text = org.jsoup.parser.Parser.unescapeEntities(text, false)
                    }
                } catch (_: Exception) {}
            }

            // 2. Strip any stray HTML tags
            if (text.contains('<') && text.contains('>')) {
                text = text.replace(Regex("<[^>]+>"), " ")
            }

            // 3. Remove soft hyphens, zero-width spaces, BOM, replacement characters
            text = text
                .replace("\u00AD", "") // soft hyphen (causes mid-word glitchy hyphenation/breaks)
                .replace("\u200B", "") // zero-width space
                .replace("\u200C", "") // zero-width non-joiner
                .replace("\u200D", "") // zero-width joiner
                .replace("\u2060", "") // word joiner
                .replace("\uFEFF", "") // BOM
                .replace("\uFFFD", "") // replacement char

            // 4. Normalize non-breaking and thin spaces to normal space so TextView wraps words naturally
            text = text
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ')

            // 5. Strip inline ad placeholder phrases injected inside paragraphs
            text = text
                .replace(Regex("(?i)Článok pokračuje pod (video )?reklamou\\.?"), " ")
                .replace(Regex("(?i)Pokračovanie článku pod (video )?reklamou\\.?"), " ")

            // 6. Fix missing spaces between glued sentences (e.g. "povedal.Ďalší" -> "povedal. Ďalší")
            text = text.replace(
                Regex("""([a-záäčďéíĺľňóôŕšťúýž]{2,})([.!?])([A-ZÁÄČĎÉÍĹĽŇÓÔŔŠŤÚÝŽ])"""),
                "$1$2 $3"
            )
            text = text.replace(
                Regex("""([a-záäčďéíĺľňóôŕšťúýž]{2,}):([„"A-ZÁÄČĎÉÍĹĽŇÓÔŔŠŤÚÝŽ])"""),
                "$1: $2"
            )

            // 7. Collapse whitespace
            return text.replace(Regex("[ \\t\\r\\n]+"), " ").trim()
        }

        fun isValidArticleImage(url: String): Boolean {
            if (url.isBlank() || !url.startsWith("http")) return false
            val lower = url.lowercase()
            val invalidKeywords = listOf(
                "pixel", "blank", "spacer", "tracking", "transparent", "1x1",
                "avatar", "author", "logo", "icon", "badge", "button", "share",
                "spinner", "loader", "placeholder", "advert", "banner-ad", "doubleclick",
                "facebook.com", "twitter.com"
            )
            if (invalidKeywords.any { lower.contains(it) }) return false
            if (lower.endsWith(".svg") || lower.endsWith(".ico") || lower.endsWith(".gif")) return false
            return true
        }

        fun normalizeImageUrl(url: String): String {
            if (url.isBlank()) return ""
            return url.substringBefore('?')
                .substringBefore('#')
                .removePrefix("https://")
                .removePrefix("http://")
                .trimEnd('/')
                .replace(Regex("""[-_]\d{2,4}x\d{2,4}"""), "")
        }

        private fun isBoilerplateText(text: String): Boolean {
            val lower = text.lowercase().trim()
            return lower.contains("súvisiace články") ||
                lower.contains("prečítajte si tiež") ||
                lower.contains("čítajte tiež:") ||
                lower.contains("čítajte aj:") ||
                lower.contains("mohlo by vás zaujímať") ||
                lower.contains("zdieľajte tento článok") ||
                lower.contains("prihláste sa na odber") ||
                lower.contains("všetky práva vyhradené") ||
                lower.contains("autor: redakcia") ||
                lower.contains("reklamný formát") ||
                lower.contains("článok pokračuje pod") ||
                lower.contains("pokračovanie článku pod") ||
                (lower.startsWith("foto:") && text.length > 80) ||
                lower == "reklama" ||
                lower == "inzercia"
        }

        /**
         * Cleans up article paragraphs:
         * 1. Eliminates consecutive 2x and 3x duplicate text or identical images.
         * 2. Binds orphan photo credit paragraphs (e.g. "Foto: TASR") directly to the preceding image.
         * 3. Prevents duplicating the hero banner image as the first inline paragraph.
         */
        fun sanitizeParagraphs(
            paragraphs: List<ReaderParagraph>,
            heroImageUrl: String? = null
        ): List<ReaderParagraph> {
            if (paragraphs.isEmpty()) return emptyList()

            val normHero = normalizeImageUrl(heroImageUrl ?: "")
            val cleaned = mutableListOf<ReaderParagraph>()
            val seenImageUrls = mutableSetOf<String>()
            if (normHero.isNotBlank()) {
                seenImageUrls.add(normHero)
            }

            for (p in paragraphs) {
                if (p.type == ParagraphType.IMAGE) {
                    val url = p.imageUrl ?: ""
                    if (!isValidArticleImage(url)) continue
                    val norm = normalizeImageUrl(url)
                    if (norm.isBlank() || seenImageUrls.contains(norm)) continue
                    seenImageUrls.add(norm)
                    cleaned.add(p.copy(caption = p.caption?.let { cleanArticleText(it) }?.ifBlank { null }))
                } else {
                    val text = cleanArticleText(p.text)
                    if (text.isBlank() || isBoilerplateText(text)) continue

                    val prev = cleaned.lastOrNull()

                    // Check if text is identical or 95%+ duplicate of immediately preceding paragraph (2x or 3x duplicate text)
                    if (prev != null && prev.type != ParagraphType.IMAGE) {
                        val prevText = prev.text.trim()
                        if (text.equals(prevText, ignoreCase = true) ||
                            (text.length > 30 && prevText.length > 30 && (text.startsWith(prevText) || prevText.startsWith(text)))
                        ) {
                            continue // Drop consecutive duplicate text!
                        }
                    }

                    // Check if this paragraph is actually an image caption/credit for the preceding image
                    val isPhotoCredit = text.matches(Regex("""^(Foto|Fotografia|Zdroj|Autor|Source|Credit|SITA|TASR|ČTK|Profimedia|Reuters|AP)\s*[:/].*""", RegexOption.IGNORE_CASE)) ||
                            (text.length <= 50 && (text.contains("TASR", ignoreCase = true) || text.contains("SITA", ignoreCase = true) || text.contains("Profimedia", ignoreCase = true)))

                    if (isPhotoCredit && prev != null && prev.type == ParagraphType.IMAGE) {
                        if (prev.caption.isNullOrBlank()) {
                            // Attach credit to the preceding image!
                            cleaned[cleaned.size - 1] = prev.copy(caption = text)
                            continue
                        } else if (prev.caption.equals(text, ignoreCase = true) || prev.caption.contains(text, ignoreCase = true)) {
                            // Already has this caption
                            continue
                        }
                    }

                    // Avoid standalone orphan photo credit if no image exists
                    if (isPhotoCredit && (prev == null || prev.type != ParagraphType.IMAGE)) {
                        continue
                    }

                    // Normalize inline bulleted or numbered paragraphs into LIST_ITEM
                    val bulletMatch = Regex("""^[•▪◦\-–]\s+(.+)""").find(text)
                    val numberMatch = Regex("""^(\d{1,2})[.)]\s+(.+)""").find(text)
                    if (p.type == ParagraphType.BODY && bulletMatch != null) {
                        cleaned.add(
                            p.copy(
                                type = ParagraphType.LIST_ITEM,
                                text = bulletMatch.groupValues[1].trim(),
                                caption = "•"
                            )
                        )
                    } else if (p.type == ParagraphType.BODY && numberMatch != null) {
                        cleaned.add(
                            p.copy(
                                type = ParagraphType.LIST_ITEM,
                                text = numberMatch.groupValues[2].trim(),
                                caption = "${numberMatch.groupValues[1]}."
                            )
                        )
                    } else {
                        cleaned.add(p.copy(text = text))
                    }
                }
            }

            return cleaned
        }
    }
}
