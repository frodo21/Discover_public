package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ripple
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.core.text.HtmlCompat
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.ui.components.ArticleGalleryViewer
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.ui.components.NativeReaderRecyclerView
import com.example.ui.components.ReaderColorConfig
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.example.ui.theme.findActivity
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ParagraphType
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderParagraph
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.ui.theme.ReaderMono
import com.example.ui.theme.ReaderSans
import com.example.ui.theme.ReaderSerif
import com.example.ui.theme.ReaderSlab
import com.example.ui.theme.SepiaBg
import com.example.ui.theme.SepiaSurface
import com.example.ui.theme.SepiaText
import com.example.ui.theme.SepiaTextSecondary
import com.example.ui.viewmodel.DiscoverViewModel

private fun normalizeImageUrl(url: String): String {
    if (url.isBlank()) return ""
    return url.substringBefore('?')
        .substringBefore('#')
        .removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')
        .replace(Regex("""[-_]\d{2,4}x\d{2,4}"""), "")
}

private fun isValidArticleImage(url: String): Boolean {
    if (url.isBlank() || !url.startsWith("http")) return false
    val lower = url.lowercase()
    val invalidKeywords = listOf(
        "pixel", "blank", "spacer", "tracking", "transparent", "1x1",
        "avatar", "author", "logo", "icon", "badge", "button", "share",
        "spinner", "loader", "placeholder", "advert", "banner-ad", "doubleclick"
    )
    if (invalidKeywords.any { lower.contains(it) }) return false
    if (lower.endsWith(".svg") || lower.endsWith(".ico") || lower.endsWith(".gif")) return false
    return true
}

data class ThemeHighlightColors(
    val bg: Color,
    val text: Color,
    val blockTint: Color
)

fun getThemeHighlightColors(theme: ReaderTheme, isDark: Boolean): ThemeHighlightColors {
    return when (theme) {
        ReaderTheme.SEPIA -> ThemeHighlightColors(
            bg = Color(0xFFE5C178),
            text = Color(0xFF3E2713),
            blockTint = Color(0xFFE5C178).copy(alpha = 0.18f)
        )
        ReaderTheme.DARK_ZEN -> ThemeHighlightColors(
            bg = Color(0xFF0284C7).copy(alpha = 0.45f),
            text = Color(0xFFE0F2FE),
            blockTint = Color(0xFF0284C7).copy(alpha = 0.16f)
        )
        ReaderTheme.OLED -> ThemeHighlightColors(
            bg = Color(0xFFEAB308).copy(alpha = 0.42f),
            text = Color(0xFFFEF08A),
            blockTint = Color(0xFFEAB308).copy(alpha = 0.15f)
        )
        ReaderTheme.DARK -> ThemeHighlightColors(
            bg = Color(0xFFD97706).copy(alpha = 0.45f),
            text = Color(0xFFFEF3C7),
            blockTint = Color(0xFFD97706).copy(alpha = 0.16f)
        )
        ReaderTheme.LIGHT -> ThemeHighlightColors(
            bg = Color(0xFFFDE68A),
            text = Color(0xFF78350F),
            blockTint = Color(0xFFFDE68A).copy(alpha = 0.20f)
        )
        ReaderTheme.SYSTEM -> if (isDark) {
            ThemeHighlightColors(
                bg = Color(0xFFD97706).copy(alpha = 0.45f),
                text = Color(0xFFFEF3C7),
                blockTint = Color(0xFFD97706).copy(alpha = 0.16f)
            )
        } else {
            ThemeHighlightColors(
                bg = Color(0xFFFDE68A),
                text = Color(0xFF78350F),
                blockTint = Color(0xFFFDE68A).copy(alpha = 0.20f)
            )
        }
    }
}

fun findWordStartOffset(text: String, charIndex: Int): Int {
    if (text.isEmpty()) return 0
    val clamped = charIndex.coerceIn(0, text.length - 1)
    var idx = clamped
    while (idx > 0 && !text[idx - 1].isWhitespace()) {
        idx--
    }
    return idx
}

private fun buildHighlightedText(
    fullText: String,
    isActive: Boolean,
    charRange: Pair<Int, Int>?,
    defaultColor: Color,
    highlightBgColor: Color,
    highlightTextColor: Color
): AnnotatedString {
    if (!isActive) return AnnotatedString(fullText)
    val builder = AnnotatedString.Builder(fullText)
    val (start, end) = charRange ?: (0 to 0)
    if (start in 0 until end && end <= fullText.length) {
        builder.addStyle(
            SpanStyle(
                background = highlightBgColor,
                color = highlightTextColor,
                fontWeight = FontWeight.Bold
            ),
            start,
            end
        )
    }
    return builder.toAnnotatedString()
}

@Composable
private fun ReaderReadingProgressBar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val progress by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 1) return@derivedStateOf 0f

            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf 0f

            val lastVisible = visibleItems.last()
            if (lastVisible.index >= totalItems - 1) {
                return@derivedStateOf 1f
            }

            val firstIndex = state.firstVisibleItemIndex
            val firstOffset = state.firstVisibleItemScrollOffset
            val itemHeight = visibleItems.firstOrNull()?.size?.toFloat() ?: 120f

            val currentPosition = firstIndex.toFloat() + (firstOffset / itemHeight.coerceAtLeast(1f))
            val maxScrollPosition = (totalItems - 1).coerceAtLeast(1).toFloat()

            (currentPosition / maxScrollPosition).coerceIn(0f, 1f)
        }
    }

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeScreen(
    viewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onProgressChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val article by viewModel.selectedArticle.collectAsState()
    val feedArticles by viewModel.feedArticles.collectAsState()
    val parsedArticle by viewModel.parsedArticle.collectAsState()
    val isLoading by viewModel.isLoadingArticle.collectAsState()
    val readerSettings by viewModel.readerSettings.collectAsState()
    val feedSettings by viewModel.feedSettings.collectAsState()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val isReadingSummary by viewModel.isReadingSummary.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    val aiSummaryResult by viewModel.aiSummaryResult.collectAsState()
    val showTtsPlayer by viewModel.showTtsPlayer.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val ttsCurrentParagraphIndex by viewModel.ttsCurrentParagraphIndex.collectAsState()
    val ttsCurrentWordRange by viewModel.ttsCurrentWordRange.collectAsState()
    val articleTransitionDirection by viewModel.articleTransitionDirection.collectAsState()

    val isTtsActive = showTtsPlayer || isTtsSpeaking
    var activeBubbleParagraphIndex by remember { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var isSpeedDialExpanded by remember { mutableStateOf(false) }
    var showAiSummaryOverlay by remember { mutableStateOf(false) }
    var isGalleryOpen by remember { mutableStateOf(false) }
    var galleryInitialIndex by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState()

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeOffsetAnim = remember { Animatable(screenWidthPx) }

    // Smooth fluid entrance animation when opening reader from feed (starts at screenWidthPx on Frame 1 -> 0f)
    var hasInitiallyEntered by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasInitiallyEntered) {
            hasInitiallyEntered = true
            swipeOffsetAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
    }

    // Keep live progress updated so feed underneath tracks in real time
    androidx.compose.runtime.LaunchedEffect(swipeOffsetAnim.value) {
        val prog = (swipeOffsetAnim.value / screenWidthPx).coerceIn(0f, 1f)
        onProgressChanged(prog)
    }

    // Predictive back handler with 1:1 displacement tracking and smooth slide dismiss
    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                val targetOffset = backEvent.progress * screenWidthPx
                swipeOffsetAnim.snapTo(targetOffset)
            }
            swipeOffsetAnim.animateTo(
                targetValue = screenWidthPx,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
            viewModel.stopTts()
            onBack()
        } catch (e: CancellationException) {
            swipeOffsetAnim.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    if (article == null) {
        onBack()
        return
    }

    val currentArticle = article!!
    val currentArticleIndex = feedArticles.indexOfFirst { it.id == currentArticle.id }
    val prevArticle = if (currentArticleIndex > 0) feedArticles[currentArticleIndex - 1] else null
    val nextArticle = if (currentArticleIndex in 0 until feedArticles.size - 1) feedArticles[currentArticleIndex + 1] else null

    val heroImg = parsedArticle?.heroImageUrl ?: currentArticle.imageUrl
    val normHero = remember(heroImg) { heroImg?.let { normalizeImageUrl(it) } ?: "" }
    val bodyImages = remember(parsedArticle, normHero) {
        val seen = mutableSetOf<String>()
        if (normHero.isNotBlank()) seen.add(normHero)
        parsedArticle?.paragraphs
            ?.filter { it.type == ParagraphType.IMAGE && !it.imageUrl.isNullOrBlank() && isValidArticleImage(it.imageUrl) }
            ?.mapNotNull { it.imageUrl }
            ?.filter { url ->
                val norm = normalizeImageUrl(url)
                if (norm.isNotBlank() && !seen.contains(norm)) {
                    seen.add(norm)
                    true
                } else false
            } ?: emptyList()
    }
    val allArticleImages = remember(heroImg, bodyImages) {
        val list = mutableListOf<String>()
        if (!heroImg.isNullOrBlank() && isValidArticleImage(heroImg)) list.add(heroImg)
        bodyImages.forEach { url ->
            if (!list.contains(url)) list.add(url)
        }
        list
    }

    val mediaDomain = remember(currentArticle.link) {
        try {
            val host = Uri.parse(currentArticle.link).host ?: ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }
    val faviconUrl = remember(mediaDomain) {
        if (mediaDomain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$mediaDomain&sz=64" else null
    }

    // USER REQUIREMENT: Article theme uses app/feed theme if "Podľa aplikácie" (SYSTEM), or explicit theme
    val effectiveTheme = if (readerSettings.theme == ReaderTheme.SYSTEM) {
        feedSettings.theme
    } else {
        readerSettings.theme
    }

    val isDarkTheme = when (effectiveTheme) {
        ReaderTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ReaderTheme.LIGHT, ReaderTheme.SEPIA -> false
        ReaderTheme.DARK, ReaderTheme.DARK_ZEN, ReaderTheme.OLED -> true
    }

    val view = LocalView.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    DisposableEffect(isDarkTheme) {
        val activity = view.context.findActivity()
        if (activity != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(activity.window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }
        onDispose {
            val activity = view.context.findActivity()
            if (activity != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(activity.window, view)
                val isFeedDark = when (feedSettings.theme) {
                    ReaderTheme.SYSTEM -> systemDark
                    ReaderTheme.LIGHT, ReaderTheme.SEPIA -> false
                    ReaderTheme.DARK, ReaderTheme.DARK_ZEN, ReaderTheme.OLED -> true
                }
                insetsController.isAppearanceLightStatusBars = !isFeedDark
            }
        }
    }

    val (bgColor, textColor, secondaryTextColor, cardBgColor) = when (effectiveTheme) {
        ReaderTheme.SYSTEM -> if (isDarkTheme) {
            listOf(Color(0xFF0F172A), Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFF1E293B))
        } else {
            listOf(Color(0xFFFCFCFC), Color(0xFF1E293B), Color(0xFF64748B), Color(0xFFF1F5F9))
        }
        ReaderTheme.LIGHT -> listOf(Color(0xFFFCFCFC), Color(0xFF1E293B), Color(0xFF64748B), Color(0xFFF1F5F9))
        ReaderTheme.SEPIA -> listOf(SepiaBg, SepiaText, SepiaTextSecondary, SepiaSurface)
        ReaderTheme.DARK -> listOf(Color(0xFF181E29), Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF242C3D))
        ReaderTheme.DARK_ZEN -> listOf(Color(0xFF141923), Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF1C2430))
        ReaderTheme.OLED -> listOf(Color.Black, Color(0xFFEDEDED), Color(0xFFA0A0A0), Color(0xFF141414))
    }

    val highlightColors = remember(effectiveTheme, isDarkTheme) { getThemeHighlightColors(effectiveTheme, isDarkTheme) }

    val fontFamily = when (readerSettings.font) {
        ReaderFont.SANS -> ReaderSans
        ReaderFont.SERIF -> ReaderSerif
        ReaderFont.SLAB -> ReaderSlab
        ReaderFont.MONO -> ReaderMono
    }

    val currentOffset = swipeOffsetAnim.value
    val progress = (currentOffset / screenWidthPx).coerceIn(0f, 1f)
    val scrimAlpha = (1f - progress) * 0.28f
    val scale = 1f - (progress * 0.04f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (currentOffset > 0) scrimAlpha else 0f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = if (currentOffset > 0) 16.dp.toPx() else 0f
                    shape = RoundedCornerShape(
                        topStart = (progress * 20).dp,
                        bottomStart = (progress * 20).dp
                    )
                    clip = currentOffset > 0
                }
                .pointerInput(currentArticle.id, feedArticles.size) {
                    val touchSlop = viewConfiguration.touchSlop
                    val edgeThreshold = with(density) { 85.dp.toPx() }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isEdge = down.position.x <= edgeThreshold
                        var isDraggingBack = false
                        var isArticleSwipe = false
                        var totalDx = 0f
                        var totalDy = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.isConsumed) break
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalDx += dx
                            totalDy += dy

                            if (!isDraggingBack && !isArticleSwipe) {
                                if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx)) {
                                    break
                                }
                                if (isEdge && totalDx > touchSlop && totalDx > abs(totalDy) * 1.2f) {
                                    isDraggingBack = true
                                } else if (!isEdge && abs(totalDx) > touchSlop * 2f && abs(totalDx) > abs(totalDy) * 1.8f) {
                                    isArticleSwipe = true
                                }
                            }
                            if (isDraggingBack) {
                                change.consume()
                                coroutineScope.launch {
                                    swipeOffsetAnim.snapTo((swipeOffsetAnim.value + dx).coerceAtLeast(0f))
                                }
                            } else if (isArticleSwipe) {
                                change.consume()
                            }
                            if (event.changes.all { !it.pressed }) break
                        }
                        if (isDraggingBack) {
                            coroutineScope.launch {
                                if (swipeOffsetAnim.value > screenWidthPx * 0.28f) {
                                    swipeOffsetAnim.animateTo(
                                        screenWidthPx,
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    )
                                    viewModel.stopTts()
                                    onBack()
                                } else {
                                    swipeOffsetAnim.animateTo(
                                        0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        } else if (isArticleSwipe) {
                            if (totalDx < -screenWidthPx * 0.18f && nextArticle != null) {
                                viewModel.stopTts()
                                viewModel.openArticle(nextArticle)
                            } else if (totalDx > screenWidthPx * 0.18f && prevArticle != null) {
                                viewModel.stopTts()
                                viewModel.openArticle(prevArticle)
                            }
                        }
                    }
                }
        ) {
            Scaffold(
        topBar = {
            TopAppBar(
                // USER REQUIREMENT: In top bar, show ONLY media favicon, no article title and no "Čitateľský režim" text!
                title = {
                    Surface(
                        shape = CircleShape,
                        color = cardBgColor,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reader_media_favicon")
                    ) {
                        if (!faviconUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = faviconUrl,
                                contentDescription = currentArticle.sourceName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(5.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentArticle.sourceName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopTts()
                            coroutineScope.launch {
                                swipeOffsetAnim.animateTo(
                                    targetValue = screenWidthPx,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                                )
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Späť do feedu"
                        )
                    }
                },
                actions = {
                    // USER REQUIREMENT: In top bar, keep ONLY bookmark, share, open in browser, and tT in this exact order!
                    // 1. Bookmark toggle
                    IconButton(
                        onClick = { viewModel.toggleBookmark(currentArticle) },
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (currentArticle.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Záložka",
                            tint = if (currentArticle.isBookmarked) MaterialTheme.colorScheme.primary else textColor
                        )
                    }

                    // 2. Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, currentArticle.title)
                                putExtra(Intent.EXTRA_TEXT, "${currentArticle.title}\n\n${currentArticle.link}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Zdieľať článok"))
                        },
                        modifier = Modifier.testTag("reader_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Zdieľať článok"
                        )
                    }

                    // 3. Open in Browser button
                    IconButton(
                        onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentArticle.link))
                            context.startActivity(browserIntent)
                        },
                        modifier = Modifier.testTag("reader_open_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Otvoriť originál v prehliadači"
                        )
                    }

                    // 4. Appearance settings button (tT)
                    IconButton(
                        onClick = { showAppearanceSheet = true },
                        modifier = Modifier.testTag("reader_appearance_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Nastavenia zobrazenia a písma"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor,
                    actionIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // USER REQUIREMENT: TTS Audio Player with speed options 1.0x, 1.5x, 2.0x, 2.5x
            AnimatedVisibility(
                visible = showTtsPlayer,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_player_bottom_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isTtsSpeaking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = if (isTtsSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (isTtsSpeaking) "Hlasové čítanie článku" else "Prehrávanie pozastavené",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTtsSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentArticle.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Close player button
                            IconButton(
                                onClick = { viewModel.closeTtsPlayer() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Zavrieť prehrávač",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Player controls row: Play/Pause, Stop, Prev/Next paragraph, Replay, and Speed chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Skip Previous Paragraph
                                IconButton(
                                    onClick = { viewModel.skipToPreviousParagraph() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Predchádzajúci odsek",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Play / Pause
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable {
                                            val paras = parsedArticle?.paragraphs
                                                ?.filter { it.type != ParagraphType.IMAGE }
                                                ?.map { it.text } ?: emptyList()
                                            viewModel.toggleTts(parsedArticle?.title ?: currentArticle.title, paras)
                                        }
                                        .testTag("tts_play_pause_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isTtsSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isTtsSpeaking) "Pozastaviť" else "Spustiť",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Skip Next Paragraph
                                IconButton(
                                    onClick = { viewModel.skipToNextParagraph() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Nasledujúci odsek",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Stop
                                IconButton(
                                    onClick = { viewModel.stopTts() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Zastaviť",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // SPEED CHIPS: 1.0x, 1.5x, 2.0x, 2.5x
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val speeds = listOf(1.0f, 1.5f, 2.0f, 2.5f)
                                speeds.forEach { speed ->
                                    val isSelected = (ttsSpeed == speed)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clickable { viewModel.setTtsSpeed(speed) }
                                            .testTag("tts_speed_${speed}x")
                                    ) {
                                        Text(
                                            text = if (speed == 1.0f) "1x" else "${speed}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor),
            contentAlignment = Alignment.TopCenter
        ) {
            val readingProgressState = remember(currentArticle.id) { mutableFloatStateOf(0f) }

            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            val readerColorConfig = remember(
                bgColor, textColor, secondaryTextColor, cardBgColor,
                primaryColor, primaryContainerColor, onPrimaryContainerColor, highlightColors
            ) {
                ReaderColorConfig(
                    bgColor = bgColor.toArgb(),
                    textColor = textColor.toArgb(),
                    secondaryTextColor = secondaryTextColor.toArgb(),
                    cardBgColor = cardBgColor.toArgb(),
                    primaryColor = primaryColor.toArgb(),
                    primaryContainerColor = primaryContainerColor.toArgb(),
                    onPrimaryContainerColor = onPrimaryContainerColor.toArgb(),
                    highlightBgColor = highlightColors.bg.toArgb(),
                    highlightTextColor = highlightColors.text.toArgb(),
                    highlightBlockTintColor = highlightColors.blockTint.toArgb(),
                    dividerColor = secondaryTextColor.copy(alpha = 0.2f).toArgb(),
                    nextCardBgColor = primaryContainerColor.copy(alpha = 0.5f).toArgb(),
                    nextCardStrokeColor = primaryColor.copy(alpha = 0.35f).toArgb(),
                    prevButtonStrokeColor = secondaryTextColor.copy(alpha = 0.35f).toArgb()
                )
            }

            // 1. HORIZONTAL READING PROGRESS BAR (tracks native scroll progress)
            LinearProgressIndicator(
                progress = { readingProgressState.floatValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .testTag("reader_reading_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )

            // 2. SMOOTH HORIZONTAL SLIDE TRANSITION BETWEEN ARTICLES
            AnimatedContent(
                targetState = currentArticle,
                transitionSpec = {
                    val direction = articleTransitionDirection
                    if (direction >= 0) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                            )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 150))
                            )
                    }
                },
                label = "article_slide_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetArticle ->
                var boundParsed by remember(targetArticle.id) { mutableStateOf<com.example.data.model.ParsedArticle?>(null) }
                if (parsedArticle != null && (parsedArticle?.sourceUrl == targetArticle.link || targetArticle.id == currentArticle.id)) {
                    boundParsed = parsedArticle
                }
                val activeParsed = boundParsed

                val targetHeroImg = activeParsed?.heroImageUrl ?: targetArticle.imageUrl
                val targetNormHero = remember(targetHeroImg) { targetHeroImg?.let { normalizeImageUrl(it) } ?: "" }
                val cleanParagraphs = remember(activeParsed?.paragraphs, targetNormHero) {
                        val rawList = activeParsed?.paragraphs ?: emptyList()
                        val seenNorms = mutableSetOf<String>()
                        if (targetNormHero.isNotBlank()) {
                            seenNorms.add(targetNormHero)
                        }

                        rawList.filter { para ->
                            if (para.type == ParagraphType.IMAGE) {
                                val imgUrl = para.imageUrl ?: ""
                                if (!isValidArticleImage(imgUrl)) return@filter false
                                val norm = normalizeImageUrl(imgUrl)
                                if (norm.isBlank() || seenNorms.contains(norm)) return@filter false
                                seenNorms.add(norm)
                                true
                            } else {
                                true
                            }
                        }
                    }

                val parsedClean = activeParsed?.cleanText ?: ""
                val fallbackContent = when {
                    parsedClean.isNotBlank() -> parsedClean
                    targetArticle.cleanContent.isNotBlank() -> targetArticle.cleanContent
                    else -> targetArticle.snippet
                }

                NativeReaderRecyclerView(
                    article = targetArticle,
                    paragraphs = cleanParagraphs,
                    fallbackText = fallbackContent,
                    heroImageUrl = targetHeroImg,
                    allArticleImages = allArticleImages,
                    settings = readerSettings,
                    colors = readerColorConfig,
                    isTtsActive = isTtsActive,
                    isReadingSummary = isReadingSummary,
                    ttsCurrentParagraphIndex = ttsCurrentParagraphIndex,
                    ttsCurrentWordRange = ttsCurrentWordRange,
                    activeBubbleParagraphIndex = activeBubbleParagraphIndex,
                    nextArticle = nextArticle,
                    prevArticle = prevArticle,
                    currentArticleIndex = currentArticleIndex,
                    totalArticlesCount = feedArticles.size,
                    onShowBubble = { index ->
                        activeBubbleParagraphIndex = index
                    },
                    onDismissBubble = {
                        activeBubbleParagraphIndex = null
                    },
                    onReadFromHere = { index ->
                        activeBubbleParagraphIndex = null
                        val articleTitle = parsedArticle?.title ?: targetArticle.title
                        val paras = if (cleanParagraphs.isNotEmpty()) {
                            cleanParagraphs
                        } else {
                            listOf(ReaderParagraph(type = ParagraphType.BODY, text = fallbackContent))
                        }
                        viewModel.speakFromParagraph(index, articleTitle, paras)
                    },
                    onSeekToWord = { index, wordOffset ->
                        val articleTitle = parsedArticle?.title ?: targetArticle.title
                        val paras = if (cleanParagraphs.isNotEmpty()) {
                            cleanParagraphs
                        } else {
                            listOf(ReaderParagraph(type = ParagraphType.BODY, text = fallbackContent))
                        }
                        viewModel.speakFromParagraph(
                            index,
                            articleTitle,
                            paras,
                            startCharOffset = wordOffset
                        )
                    },
                    onImageClick = { url ->
                        val idx = allArticleImages.indexOf(url).coerceAtLeast(0)
                        galleryInitialIndex = idx
                        isGalleryOpen = true
                    },
                    onNextArticle = {
                        if (nextArticle != null) {
                            viewModel.stopTts()
                            viewModel.openArticle(nextArticle)
                        }
                    },
                    onPrevArticle = {
                        if (prevArticle != null) {
                            viewModel.stopTts()
                            viewModel.openArticle(prevArticle)
                        }
                    },
                    onDeleteArticle = {
                        val next = nextArticle
                        val prev = prevArticle
                        viewModel.deleteArticle(targetArticle)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Článok bol vymazaný",
                                actionLabel = "OBNOVIŤ",
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                viewModel.restoreLastDeletedArticle()
                            }
                        }
                        if (next != null) {
                            viewModel.stopTts()
                            viewModel.openArticle(next)
                        } else if (prev != null) {
                            viewModel.stopTts()
                            viewModel.openArticle(prev)
                        } else {
                            viewModel.stopTts()
                            onBack()
                        }
                    },
                    onScrollProgress = { progress ->
                        readingProgressState.floatValue = progress
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim to tap away when speed-dial is expanded
            if (isSpeedDialExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isSpeedDialExpanded = false
                        }
                        .zIndex(15f)
                )
            }

            // USER REQUIREMENT: Floating Speed-Dial Button (stably anchored, zero scroll recomp/animation overhead)
            // "Tlačidlo na zhrnutie článku prehod do plávajúceho tlacidla tak ako je zvuková transkripcia. bude tam spoločne tlačidlo ktoré rozbalk ďalšie 2 guličky - audio a AI zhrnutie."
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
                    .zIndex(20f)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Expanded sub-buttons (guličky)
                    AnimatedVisibility(
                        visible = isSpeedDialExpanded,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sub-button 1: AI Zhrnutie
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = "AI Zhrnutie",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        isSpeedDialExpanded = false
                                        showAiSummaryOverlay = true
                                        if (aiSummaryResult == null && !isSummarizing) {
                                            viewModel.summarizeCurrentArticle()
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .testTag("speed_dial_ai_summary")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Zhrnutie",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Sub-button 2: Audio transkripcia
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = "Audio čítanie",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        isSpeedDialExpanded = false
                                        val articleTitle = parsedArticle?.title ?: currentArticle.title
                                        val rawParas = parsedArticle?.paragraphs
                                            ?.filter { it.type != ParagraphType.IMAGE && it.text.isNotBlank() }
                                        val fallback = parsedArticle?.cleanText?.ifBlank { null }
                                            ?: currentArticle.cleanContent.ifBlank { null }
                                            ?: currentArticle.snippet
                                        val paras = if (!rawParas.isNullOrEmpty()) {
                                            rawParas
                                        } else if (fallback.isNotBlank()) {
                                            listOf(ReaderParagraph(type = ParagraphType.BODY, text = fallback))
                                        } else {
                                            emptyList()
                                        }
                                        viewModel.showTtsPlayer.value = true
                                        if (!isTtsSpeaking) {
                                            viewModel.speakCurrentArticle(articleTitle, paras)
                                        } else {
                                            viewModel.toggleTts(articleTitle, paras)
                                        }
                                    },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .testTag("speed_dial_audio")
                                ) {
                                    Icon(
                                        imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                        contentDescription = "Audio čítanie",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Common Floating Action Button (rotates smoothly into an 'x' when opened)
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isSpeedDialExpanded) 45f else 0f,
                        label = "fab_rotation"
                    )
                    FloatingActionButton(
                        onClick = { isSpeedDialExpanded = !isSpeedDialExpanded },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("reader_speed_dial_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Rýchle akcie článku",
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer { rotationZ = rotationAngle }
                        )
                    }
                }
            }

            // USER REQUIREMENT: AI summary displayed in an overlay bubble
            // "AI zhrnutie urob čo najkratšie ale aby zastihlo podstatu a nafotmatuj ho. a zobrazuj ho v overlay bubline."
            if (showAiSummaryOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showAiSummaryOverlay = false }
                        .zIndex(30f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 520.dp)
                            .wrapContentHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* prevent click-through */ }
                            .testTag("ai_summary_overlay_bubble")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "AI Zhrnutie",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Podstata článku",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showAiSummaryOverlay = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Zavrieť",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Body
                            if (isSummarizing) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Generujem stručnú podstatu článku...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (aiSummaryResult != null) {
                                val summaryBlocks = remember(aiSummaryResult) { splitSummaryIntoBlocks(aiSummaryResult ?: "") }
                                val cleanSummaryList = remember(summaryBlocks) {
                                    summaryBlocks.map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim() }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    summaryBlocks.forEachIndexed { bIdx, blockHtml ->
                                        FormattedSummaryBlock(
                                            blockHtml = blockHtml,
                                            blockIndex = bIdx,
                                            fontFamily = fontFamily,
                                            isReadingSummary = isReadingSummary,
                                            isTtsActive = isTtsActive,
                                            activeBlockIndex = ttsCurrentParagraphIndex,
                                            wordRange = ttsCurrentWordRange,
                                            highlightColors = highlightColors,
                                            onSeekBlock = { blockIndex, wordOffset ->
                                                viewModel.speakFromSummary(blockIndex, cleanSummaryList, startCharOffset = wordOffset)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Read summary aloud
                                    Button(
                                        onClick = {
                                            viewModel.speakFromSummary(0, cleanSummaryList, 0)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Prečítať", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Copy summary
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val cleanSummary = HtmlCompat.fromHtml(
                                                aiSummaryResult!!,
                                                HtmlCompat.FROM_HTML_MODE_COMPACT
                                            ).toString()
                                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Summary", cleanSummary))
                                            Toast.makeText(context, "Zhrnutie skopírované do schránky", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kopírovať", fontSize = 12.sp)
                                    }

                                    // Regenerate
                                    IconButton(
                                        onClick = { viewModel.summarizeCurrentArticle() },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Prepočítať",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Zatiaľ nebolo vytvorené zhrnutie pre tento článok.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { viewModel.summarizeCurrentArticle() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Vytvoriť zhrnutie")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Appearance Modal Bottom Sheet
    if (showAppearanceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppearanceSheet = false },
            sheetState = sheetState
        ) {
            AppearanceSettingsContent(
                settings = readerSettings,
                onUpdateFontSize = { viewModel.updateFontSize(it) },
                onSetTheme = { viewModel.setReaderTheme(it) },
                onSetFont = { viewModel.setReaderFont(it) },
                onToggleImages = { viewModel.toggleReaderImages(it) }
            )
        }
    }

    if (isGalleryOpen && allArticleImages.isNotEmpty()) {
        ArticleGalleryViewer(
            images = allArticleImages,
            initialIndex = galleryInitialIndex,
            onDismiss = { isGalleryOpen = false }
        )
    }
        }
    }
}

@Composable
fun ReaderParagraphItem(
    paragraph: ReaderParagraph,
    paragraphIndex: Int,
    isActiveSpeaking: Boolean,
    isTtsActive: Boolean,
    isBubbleVisible: Boolean,
    wordRange: Pair<Int, Int>?,
    highlightColors: ThemeHighlightColors,
    settings: ReaderSettings,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    fontFamily: FontFamily,
    onImageClick: (String) -> Unit = {},
    onReadFromHere: () -> Unit = {},
    onSeekToWord: (startCharOffset: Int) -> Unit = {},
    onShowBubble: () -> Unit = {}
) {
    if (paragraph.type == ParagraphType.IMAGE) {
        if (settings.showImages && !paragraph.imageUrl.isNullOrBlank() && isValidArticleImage(paragraph.imageUrl)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = paragraph.imageUrl,
                    contentDescription = paragraph.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 340.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(paragraph.imageUrl) },
                    contentScale = ContentScale.Crop
                )
                if (!paragraph.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = paragraph.caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

    val isHighlighted = isActiveSpeaking && isTtsActive
    val styledText = remember(paragraph.text, isHighlighted, wordRange, textColor, highlightColors) {
        buildHighlightedText(
            fullText = paragraph.text,
            isActive = isHighlighted,
            charRange = wordRange,
            defaultColor = textColor,
            highlightBgColor = highlightColors.bg,
            highlightTextColor = highlightColors.text
        )
    }

    val textLayoutRef = remember { arrayOfNulls<TextLayoutResult>(1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isBubbleVisible && !isTtsActive) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onReadFromHere() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Prečítať odtiaľto",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        val tapModifier = Modifier.pointerInput(isTtsActive) {
            detectTapGestures(
                onLongPress = {
                    if (!isTtsActive) onShowBubble()
                },
                onTap = { tapOffset ->
                    if (isTtsActive) {
                        val charIdx = textLayoutRef[0]?.getOffsetForPosition(tapOffset) ?: 0
                        val wordStart = findWordStartOffset(paragraph.text, charIdx)
                        onSeekToWord(wordStart)
                    }
                }
            )
        }

        when (paragraph.type) {
            ParagraphType.HEADING_1 -> {
                Text(
                    text = styledText,
                    onTextLayout = { textLayoutRef[0] = it },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = (settings.fontSizeSp + 6.5f).sp,
                        lineHeight = ((settings.fontSizeSp + 6.5f) * 1.24f).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (isHighlighted) Modifier.background(highlightColors.blockTint) else Modifier)
                        .then(tapModifier)
                        .padding(top = 18.dp, bottom = 6.dp)
                )
            }
            ParagraphType.HEADING_2 -> {
                Text(
                    text = styledText,
                    onTextLayout = { textLayoutRef[0] = it },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (settings.fontSizeSp + 4.5f).sp,
                        lineHeight = ((settings.fontSizeSp + 4.5f) * 1.26f).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (isHighlighted) Modifier.background(highlightColors.blockTint) else Modifier)
                        .then(tapModifier)
                        .padding(top = 14.dp, bottom = 4.dp)
                )
            }
            ParagraphType.BODY -> {
                Text(
                    text = styledText,
                    onTextLayout = { textLayoutRef[0] = it },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * 1.5f).sp,
                        fontFamily = fontFamily
                    ),
                    textAlign = TextAlign.Start,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (isHighlighted) Modifier.background(highlightColors.blockTint) else Modifier)
                        .then(tapModifier)
                )
            }
            ParagraphType.BLOCKQUOTE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBgColor)
                        .then(if (isHighlighted) Modifier.border(1.dp, highlightColors.bg, RoundedCornerShape(8.dp)) else Modifier)
                        .then(tapModifier)
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = styledText,
                        onTextLayout = { textLayoutRef[0] = it },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (settings.fontSizeSp - 0.5f).sp,
                            fontStyle = FontStyle.Italic,
                            fontFamily = fontFamily
                        ),
                        textAlign = TextAlign.Start,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ParagraphType.LIST_ITEM -> {
                val listMarker = paragraph.caption?.takeIf { it.isNotBlank() } ?: "•"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .then(if (isHighlighted) Modifier.background(highlightColors.blockTint) else Modifier)
                        .then(tapModifier)
                        .padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = listMarker,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = settings.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.widthIn(min = 24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = styledText,
                        onTextLayout = { textLayoutRef[0] = it },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * 1.4f).sp,
                            fontFamily = fontFamily
                        ),
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AppearanceSettingsContent(
    settings: ReaderSettings,
    onUpdateFontSize: (Float) -> Unit,
    onSetTheme: (ReaderTheme) -> Unit,
    onSetFont: (ReaderFont) -> Unit,
    onToggleImages: (Boolean) -> Unit
) {
    val systemBg = MaterialTheme.colorScheme.background
    val systemBorder = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Prispôsobenie čítania",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Font size slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Veľkosť písma", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${settings.fontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "A", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = onUpdateFontSize,
                    valueRange = 13f..26f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "A", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Theme selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Téma pozadia", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderTheme.entries.forEach { theme ->
                    val isSelected = settings.theme == theme
                    val (previewBg, previewBorder) = when (theme) {
                        ReaderTheme.SYSTEM -> systemBg to systemBorder
                        ReaderTheme.LIGHT -> Color(0xFFFCFCFC) to Color.LightGray
                        ReaderTheme.SEPIA -> SepiaBg to Color(0xFFD4B895)
                        ReaderTheme.DARK -> Color(0xFF1E293B) to Color(0xFF475569)
                        ReaderTheme.DARK_ZEN -> Color(0xFF141923) to Color(0xFF334155)
                        ReaderTheme.OLED -> Color.Black to Color(0xFF333333)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetTheme(theme) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(previewBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = previewBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else previewBorder
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {}
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = theme.getArticleLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // USER REQUIREMENT: Working font selector with distinct fonts (Sans, Serif, Slab, Mono)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Typ písma", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderFont.entries.forEach { font ->
                    val isSelected = settings.font == font
                    val itemFontFamily = when (font) {
                        ReaderFont.SANS -> ReaderSans
                        ReaderFont.SERIF -> ReaderSerif
                        ReaderFont.SLAB -> ReaderSlab
                        ReaderFont.MONO -> ReaderMono
                    }

                    Surface(
                        onClick = { onSetFont(font) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("font_option_${font.name}")
                    ) {
                        Text(
                            text = font.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = itemFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Zobrazovať obrázky v článku", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Vypnutie šetrí mobilné dáta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.showImages,
                onCheckedChange = onToggleImages
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun splitSummaryIntoBlocks(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val normalized = raw
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)<p>"), "")

    val lines = normalized.split("\n")
    val rawBlocks = mutableListOf<String>()
    val current = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            if (current.isNotEmpty()) {
                rawBlocks.add(current.toString().trim())
                current.clear()
            }
        } else if (
            trimmed.startsWith("•") ||
            trimmed.startsWith("-") ||
            trimmed.startsWith("📌") ||
            trimmed.startsWith("🎯") ||
            trimmed.startsWith("⚡") ||
            trimmed.startsWith("💡") ||
            trimmed.startsWith("<b>")
        ) {
            if (current.isNotEmpty()) {
                rawBlocks.add(current.toString().trim())
                current.clear()
            }
            rawBlocks.add(trimmed)
        } else {
            if (current.isNotEmpty()) {
                current.append(" ")
            }
            current.append(trimmed)
        }
    }
    if (current.isNotEmpty()) {
        rawBlocks.add(current.toString().trim())
    }

    // Merge "Podstata" heading line with the immediately following essence paragraph so Part 1 is a single cohesive card
    val mergedBlocks = mutableListOf<String>()
    var i = 0
    while (i < rawBlocks.size) {
        val block = rawBlocks[i]
        val plain = block.replace(Regex("<[^>]*>"), "").trim()
        val isEssenceHeadingOnly = (plain.contains("Podstata", ignoreCase = true) || plain.contains("Hlavná myšlienka", ignoreCase = true)) &&
            plain.length <= 32 &&
            i + 1 < rawBlocks.size &&
            !rawBlocks[i + 1].trim().startsWith("•")

        if (isEssenceHeadingOnly) {
            mergedBlocks.add("$block<br>${rawBlocks[i + 1]}")
            i += 2
        } else {
            mergedBlocks.add(block)
            i++
        }
    }

    return mergedBlocks.ifEmpty { listOf(raw) }
}

@Composable
fun FormattedSummaryBlock(
    blockHtml: String,
    blockIndex: Int,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    isReadingSummary: Boolean,
    isTtsActive: Boolean,
    activeBlockIndex: Int,
    wordRange: Pair<Int, Int>?,
    highlightColors: ThemeHighlightColors,
    onSeekBlock: (blockIndex: Int, charOffset: Int) -> Unit
) {
    val spanned = remember(blockHtml) {
        androidx.core.text.HtmlCompat.fromHtml(
            blockHtml,
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }
    val plainText = spanned.toString()
    val isActiveBlock = isReadingSummary && isTtsActive && (activeBlockIndex == blockIndex)
    val isCoreEssenceBlock = blockIndex == 0 && !plainText.trimStart().startsWith("•")

    val annotatedString = remember(spanned, isActiveBlock, wordRange, highlightColors) {
        val builder = androidx.compose.ui.text.AnnotatedString.Builder(plainText)

        val styleSpans = spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java)
        for (span in styleSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span.style) {
                android.graphics.Typeface.BOLD -> builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end
                )
                android.graphics.Typeface.ITALIC -> builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    start,
                    end
                )
                android.graphics.Typeface.BOLD_ITALIC -> builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    start,
                    end
                )
            }
        }

        if (isActiveBlock && wordRange != null) {
            val (wStart, wEnd) = wordRange
            if (wStart in 0 until wEnd && wEnd <= plainText.length) {
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        background = highlightColors.bg,
                        color = highlightColors.text,
                        fontWeight = FontWeight.Bold
                    ),
                    wStart,
                    wEnd
                )
            }
        }
        builder.toAnnotatedString()
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isActiveBlock -> highlightColors.blockTint
            isCoreEssenceBlock -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = when {
            isActiveBlock -> BorderStroke(1.5.dp, highlightColors.bg)
            isCoreEssenceBlock -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            else -> null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(blockIndex, plainText) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val charIdx = textLayoutResult?.getOffsetForPosition(tapOffset) ?: 0
                        val wordStart = findWordStartOffset(plainText, charIdx)
                        onSeekBlock(blockIndex, wordStart)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = if (isCoreEssenceBlock) 12.dp else 9.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = annotatedString,
                onTextLayout = { textLayoutResult = it },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isCoreEssenceBlock) 15.sp else 14.sp,
                    lineHeight = 22.sp,
                    fontFamily = fontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

