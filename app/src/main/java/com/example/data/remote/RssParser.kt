package com.example.data.remote

import com.example.data.model.Article
import com.example.data.model.FeedSource
import org.jsoup.Jsoup
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

class RssParser {

    fun parse(xmlContent: String, source: FeedSource): List<Article> = parseRss(xmlContent, source)

    fun parseRss(xmlContent: String, source: FeedSource): List<Article> {
        val articles = mutableListOf<Article>()
        try {
            val doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser())
            val items = doc.select("item, entry")

            for (item in items) {
                val title = item.select("title").text().trim()
                var link = item.select("link").text().trim()
                if (link.isEmpty()) {
                    link = item.select("link").attr("href").trim()
                }

                if (link.startsWith("/")) {
                    link = source.websiteUrl.trimEnd('/') + link
                }

                if (title.isBlank() || link.isBlank()) continue

                val description = item.select("description, summary").text().trim()
                val contentEncoded = item.select("content\\:encoded, content").text().trim()
                val author = item.select("author, dc\\:creator, creator").text().trim()
                val pubDateStr = item.select("pubDate, published, updated").text().trim()

                // Extract highest quality image
                var rawImageUrl: String? = null

                // 1. Check media:content (often has HD images)
                val mediaContents = item.select("media\\:content")
                for (mc in mediaContents) {
                    val mUrl = mc.attr("url").trim()
                    val medium = mc.attr("medium").lowercase()
                    val type = mc.attr("type").lowercase()
                    if (mUrl.startsWith("http") && (medium == "image" || type.startsWith("image") || isImageExtension(mUrl))) {
                        rawImageUrl = mUrl
                        break
                    }
                }

                // 2. Check enclosure tag
                if (rawImageUrl == null) {
                    val enclosure = item.select("enclosure")
                    val encUrl = enclosure.attr("url").trim()
                    val encType = enclosure.attr("type").lowercase()
                    if (encUrl.startsWith("http") && (encType.startsWith("image") || isImageExtension(encUrl))) {
                        rawImageUrl = encUrl
                    }
                }

                // 3. Check media:thumbnail
                if (rawImageUrl == null) {
                    val mediaThumb = item.select("media\\:thumbnail").attr("url").trim()
                    if (mediaThumb.startsWith("http")) {
                        rawImageUrl = mediaThumb
                    }
                }

                // 4. Check direct figure / img tags inside item (e.g. Nextech)
                if (rawImageUrl == null) {
                    val directImg = item.select("figure img, img").first()?.attr("src")?.trim()
                    if (!directImg.isNullOrBlank()) {
                        rawImageUrl = directImg
                    }
                }

                // 5. Extract from HTML content
                if (rawImageUrl == null) {
                    rawImageUrl = extractImageFromHtml(contentEncoded.ifEmpty { description })
                }

                // If image URL is relative (e.g. /files/photo/...), resolve against websiteUrl
                if (rawImageUrl != null && rawImageUrl.startsWith("/")) {
                    rawImageUrl = source.websiteUrl.trimEnd('/') + "/" + rawImageUrl.trimStart('/')
                }

                // Upgrade image URL to maximum quality (remove downscaling params)
                val finalImageUrl = rawImageUrl?.let { upgradeImageToHighRes(it) }

                val cleanSnippet = cleanHtmlSnippet(description)
                val pubMillis = parseDateToMillis(pubDateStr)
                val formattedDate = formatRelativeTime(pubMillis)
                val id = generateArticleId(link, title)

                // Calculate realistic reading time
                val rawText = (contentEncoded.ifEmpty { description }).replace(Regex("<[^>]*>"), " ").trim()
                val wordCount = rawText.split(Regex("\\s+")).count { it.isNotBlank() }
                val estimatedMinutes = if (wordCount > 50) maxOf(2, minOf(12, (wordCount / 130) + 1)) else 3

                articles.add(
                    Article(
                        id = id,
                        title = cleanHtmlEntities(title),
                        link = link,
                        sourceId = source.id,
                        sourceName = source.name,
                        category = source.category,
                        pubDateMillis = pubMillis,
                        pubDateFormatted = formattedDate,
                        snippet = cleanSnippet,
                        fullContentRaw = contentEncoded.ifEmpty { description },
                        cleanContent = if (contentEncoded.isNotBlank() && contentEncoded.length > 150) cleanHtmlFullText(contentEncoded) else "",
                        imageUrl = finalImageUrl,
                        author = author,
                        readTimeMinutes = estimatedMinutes
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articles
    }

    private fun isImageExtension(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".jpg") || lower.contains(".jpeg") ||
                lower.contains(".png") || lower.contains(".webp") ||
                lower.contains(".avif")
    }

    private fun upgradeImageToHighRes(url: String): String {
        var u = url.trim()

        // 1. Google News / Googleusercontent thumbnails (e.g. =s100, =w150-h150, =w200, =s0-w100) -> upgrade to high-res =w1200
        if (u.contains("googleusercontent.com") || u.contains("gstatic.com")) {
            u = u.replace(Regex("""=(?:s|w\d+-h|w)\d+[^&?#]*"""), "=w1200")
        }

        // 2. WordPress / news blog thumbnail suffixes: e.g. -150x150.jpg, -300x200.png, -600x400.webp
        // Strip the resolution suffix so it requests the original crystal-clear high-res asset
        u = u.replace(Regex("""-\d{2,4}x\d{2,4}(?=\.(jpg|jpeg|png|webp|avif))""", RegexOption.IGNORE_CASE), "")

        // 3. Remove tiny resize query parameters (e.g., ?w=150&h=150 or ?resize=200%2C150 or ?quality=50)
        u = u.replace(Regex("""[?&](?:w|width|h|height|resize|size)=\d+[^&#]*"""), "")

        // 4. Slovak/Czech news server thumb downscalers: /thumb/ or /w150/ or /w300/ -> /w1200/
        u = u.replace(Regex("""/w\d{2,3}/"""), "/w1200/")

        return u
    }

    private fun extractImageFromHtml(html: String): String? {
        if (html.isBlank()) return null
        return try {
            val doc = Jsoup.parseBodyFragment(html)
            val imgs = doc.select("img")
            for (img in imgs) {
                val src = (img.attr("src").ifEmpty { img.attr("data-src") }).trim()
                if (src.startsWith("http") && isImageExtension(src)) {
                    // Ignore tiny tracking pixels, emojis, or badges
                    val width = img.attr("width").toIntOrNull() ?: 100
                    val height = img.attr("height").toIntOrNull() ?: 100
                    if (width > 20 && height > 20 && !src.contains("pixel") && !src.contains("tracking") && !src.contains("icon")) {
                        return src
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanHtmlSnippet(html: String): String {
        if (html.isBlank()) return ""
        val withSpaces = html
            .replace(Regex("(?i)<br\\s*/?>"), " ")
            .replace(Regex("(?i)</(p|div|h1|h2|h3|h4|li)>"), " ")
        val text = ArticleExtractor.cleanArticleText(Jsoup.parse(withSpaces).text())
        return if (text.length > 220) text.take(217).trimEnd() + "..." else text
    }

    private fun cleanHtmlFullText(html: String): String {
        if (html.isBlank()) return ""
        val withBreaks = html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|h1|h2|h3|h4|h5|li|blockquote)>"), "\n\n")
        val doc = Jsoup.parse(withBreaks)
        doc.outputSettings().prettyPrint(false)
        return doc.wholeText()
            .split(Regex("\\n\\s*\\n|\\n"))
            .map { ArticleExtractor.cleanArticleText(it) }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun cleanHtmlEntities(text: String): String {
        return ArticleExtractor.cleanArticleText(Jsoup.parse(text).text())
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun formatRelativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 2 -> "Práve teraz"
            minutes < 60 -> "Pred ${minutes} min"
            hours < 24 -> "Pred ${hours} h"
            days == 1L -> "Včera"
            days < 7 -> "Pred ${days} dňami"
            else -> {
                val sdf = SimpleDateFormat("d. MMMM", Locale.forLanguageTag("sk"))
                sdf.format(java.util.Date(millis))
            }
        }
    }

    private fun generateArticleId(link: String, title: String): String {
        val input = "$link|$title"
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
