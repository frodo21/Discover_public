package com.example.data.repository

import com.example.data.local.ArticleDao
import com.example.data.local.FeedSourceDao
import com.example.data.model.Article
import com.example.data.model.DeletedArticle
import com.example.data.model.FeedSource
import com.example.data.model.ParsedArticle
import com.example.data.remote.ArticleExtractor
import com.example.data.remote.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class DiscoverRepository(
    private val articleDao: ArticleDao,
    private val feedSourceDao: FeedSourceDao,
    private val rssParser: RssParser = RssParser(),
    private val articleExtractor: ArticleExtractor = ArticleExtractor(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()
    val bookmarkedArticles: Flow<List<Article>> = articleDao.getBookmarkedArticles()
    val readingHistory: Flow<List<com.example.data.model.ReadingHistoryEntry>> = articleDao.getAllReadingHistory()
    val allSources: Flow<List<FeedSource>> = feedSourceDao.getAllSources()
    val enabledSources: Flow<List<FeedSource>> = feedSourceDao.getEnabledSources()

    suspend fun refreshFeeds(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val sources = feedSourceDao.getEnabledSources().first()
            if (sources.isEmpty()) {
                return@withContext Result.success(0)
            }

            val deferredResults = sources.map { source ->
                async {
                    try {
                        val request = Request.Builder()
                            .url(source.feedUrl)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 Discover/1.7")
                            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
                            .build()

                        val response = httpClient.newCall(request).execute()
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            val headerCharset = response.body?.contentType()?.charset()?.name()
                            response.close()
                            val xml = if (bytes != null && bytes.isNotEmpty()) {
                                decodeXmlBytes(bytes, headerCharset)
                            } else ""
                            if (xml.isNotBlank()) {
                                rssParser.parse(xml, source)
                            } else emptyList()
                        } else {
                            response.close()
                            emptyList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList<Article>()
                    }
                }
            }

            val fetchedArticles = deferredResults.awaitAll().flatten()
            if (fetchedArticles.isNotEmpty()) {
                // Filter out any articles that the user previously deleted (persisting deletion)
                val deletedIds = articleDao.getAllDeletedArticleIds().toHashSet()
                val articlesToInsert = if (deletedIds.isEmpty()) {
                    fetchedArticles
                } else {
                    fetchedArticles.filter { it.id !in deletedIds }
                }

                if (articlesToInsert.isNotEmpty()) {
                    articleDao.insertArticles(articlesToInsert)
                }

                // Prune articles older than 14 days that aren't bookmarked
                val twoWeeksAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
                articleDao.pruneOldArticles(twoWeeksAgo)

                // Prune deleted article tombstones older than 45 days
                val fortyFiveDaysAgo = System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000)
                articleDao.pruneOldDeletedArticles(fortyFiveDaysAgo)
            }

            Result.success(fetchedArticles.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeXmlBytes(bytes: ByteArray, headerCharset: String?): String {
        if (!headerCharset.isNullOrBlank()) {
            try {
                return String(bytes, java.nio.charset.Charset.forName(headerCharset))
            } catch (_: Exception) {}
        }
        val asciiProbe = String(bytes, 0, minOf(bytes.size, 512), Charsets.ISO_8859_1)
        val xmlEncodingMatch = Regex("""encoding\s*=\s*["']([a-zA-Z0-9_\-]+)["']""", RegexOption.IGNORE_CASE).find(asciiProbe)
        val detectedEncoding = xmlEncodingMatch?.groupValues?.getOrNull(1)?.trim()
        if (!detectedEncoding.isNullOrBlank()) {
            try {
                return String(bytes, java.nio.charset.Charset.forName(detectedEncoding))
            } catch (_: Exception) {}
        }
        return String(bytes, Charsets.UTF_8)
    }

    private val parsedArticleCache = android.util.LruCache<String, ParsedArticle>(15)

    fun getCachedParsedArticle(articleId: String): ParsedArticle? {
        return synchronized(parsedArticleCache) {
            parsedArticleCache.get(articleId)
        }
    }

    suspend fun getParsedArticle(article: Article): ParsedArticle = withContext(Dispatchers.IO) {
        synchronized(parsedArticleCache) {
            parsedArticleCache.get(article.id)
        }?.let { return@withContext it }

        val dbArticle = articleDao.getArticleById(article.id)
        val storedCleanContent = article.cleanContent.ifBlank { dbArticle?.cleanContent ?: "" }
        val rawHtmlContent = article.fullContentRaw.ifBlank { dbArticle?.fullContentRaw ?: "" }
        val bestFallbackContent = when {
            rawHtmlContent.length > storedCleanContent.length -> rawHtmlContent
            storedCleanContent.isNotBlank() -> storedCleanContent
            else -> article.snippet
        }

        val parsed = articleExtractor.extract(
            url = article.link,
            fallbackTitle = article.title,
            fallbackContent = bestFallbackContent,
            sourceName = article.sourceName,
            fallbackImageUrl = article.imageUrl
        )

        if (parsed.paragraphs.isNotEmpty()) {
            synchronized(parsedArticleCache) {
                parsedArticleCache.put(article.id, parsed)
            }
        }

        if (storedCleanContent.isBlank() && parsed.cleanText.isNotBlank()) {
            articleDao.updateCleanContent(article.id, parsed.cleanText)
        }
        parsed
    }

    suspend fun toggleBookmark(articleId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateBookmark(articleId, isBookmarked)
        articleDao.updateReadingHistoryBookmark(articleId, isBookmarked)
    }

    suspend fun markAsRead(articleId: String) = withContext(Dispatchers.IO) {
        articleDao.markAsRead(articleId)
    }

    suspend fun recordArticleRead(article: Article) = withContext(Dispatchers.IO) {
        articleDao.markAsRead(article.id)
        val historyEntry = com.example.data.model.ReadingHistoryEntry(
            articleId = article.id,
            title = article.title,
            link = article.link,
            sourceId = article.sourceId,
            sourceName = article.sourceName,
            category = article.category,
            pubDateMillis = article.pubDateMillis,
            pubDateFormatted = article.pubDateFormatted,
            snippet = article.snippet,
            imageUrl = article.imageUrl,
            author = article.author,
            readTimeMinutes = article.readTimeMinutes,
            isBookmarked = article.isBookmarked,
            readAtMillis = System.currentTimeMillis()
        )
        articleDao.insertReadingHistory(historyEntry)
    }

    suspend fun deleteReadingHistoryEntry(articleId: String) = withContext(Dispatchers.IO) {
        articleDao.deleteReadingHistoryEntry(articleId)
    }

    suspend fun clearReadingHistory() = withContext(Dispatchers.IO) {
        articleDao.clearAllReadingHistory()
    }

    suspend fun updateReadState(articleId: String, isRead: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateReadState(articleId, isRead)
    }

    suspend fun deleteReadArticles(): Int = withContext(Dispatchers.IO) {
        val readIds = articleDao.getReadNonBookmarkedArticleIds()
        if (readIds.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val tombstones = readIds.map { DeletedArticle(id = it, deletedAtMillis = now) }
            articleDao.insertDeletedArticles(tombstones)
            articleDao.deleteReadArticles()
        } else {
            0
        }
    }

    suspend fun markAllArticlesAsRead() = withContext(Dispatchers.IO) {
        articleDao.markAllArticlesAsRead()
    }

    suspend fun toggleSourceEnabled(sourceId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        feedSourceDao.updateSourceEnabled(sourceId, isEnabled)
    }

    suspend fun addCustomSource(name: String, url: String, category: String): Result<FeedSource> = withContext(Dispatchers.IO) {
        try {
            val trimmedUrl = url.trim()
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", "DiscoverNewsReader/1.1 (Android; RSS Reader)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(Exception("Server vrátil HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            response.close()
            if (!body.contains("<rss", ignoreCase = true) && !body.contains("<feed", ignoreCase = true) && !body.contains("<xml", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Zadaná URL adresa neobsahuje platný RSS alebo Atom feed"))
            }

            val source = FeedSource(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                feedUrl = trimmedUrl,
                websiteUrl = trimmedUrl,
                category = category,
                isEnabled = true,
                isCustom = true
            )
            feedSourceDao.insertSource(source)
            Result.success(source)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSource(sourceId: String) = withContext(Dispatchers.IO) {
        feedSourceDao.deleteSourceById(sourceId)
    }

    suspend fun getArticleById(id: String): Article? = withContext(Dispatchers.IO) {
        articleDao.getArticleById(id)
    }

    suspend fun deleteArticle(articleId: String) = withContext(Dispatchers.IO) {
        articleDao.insertDeletedArticle(DeletedArticle(id = articleId))
        articleDao.deleteArticleById(articleId)
    }

    suspend fun insertArticle(article: Article) = withContext(Dispatchers.IO) {
        articleDao.removeDeletedArticle(article.id)
        articleDao.insertArticle(article)
    }

    suspend fun clearDeletedArticlesHistory() = withContext(Dispatchers.IO) {
        articleDao.clearAllDeletedArticles()
    }

    suspend fun syncDefaultSourcesIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existing = feedSourceDao.getSourcesList()
            val existingMap = existing.associateBy { it.id }
            val missingOrUpdated = mutableListOf<FeedSource>()
            for (defaultSource in com.example.data.local.PreloadedData.INITIAL_SOURCES) {
                val current = existingMap[defaultSource.id]
                if (current == null) {
                    missingOrUpdated.add(defaultSource)
                } else if (current.feedUrl != defaultSource.feedUrl) {
                    missingOrUpdated.add(current.copy(feedUrl = defaultSource.feedUrl, websiteUrl = defaultSource.websiteUrl, name = defaultSource.name))
                }
            }
            if (missingOrUpdated.isNotEmpty()) {
                feedSourceDao.insertSources(missingOrUpdated)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreDefaultSources() = withContext(Dispatchers.IO) {
        try {
            feedSourceDao.insertSources(com.example.data.local.PreloadedData.INITIAL_SOURCES)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertRegionalSources(sources: List<FeedSource>) = withContext(Dispatchers.IO) {
        try {
            feedSourceDao.insertSources(sources)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateSourceEnabled(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        feedSourceDao.updateSourceEnabled(id, isEnabled)
    }
}
