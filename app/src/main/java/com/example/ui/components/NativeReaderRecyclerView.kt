package com.example.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.util.CoilUtils
import com.example.R
import com.example.data.model.Article
import com.example.data.model.ParagraphType
import com.example.data.model.ReaderFont
import com.example.data.model.ReaderParagraph
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.ui.screens.ThemeHighlightColors
import com.example.ui.screens.findWordStartOffset
import kotlin.math.roundToInt

data class ReaderColorConfig(
    val bgColor: Int,
    val textColor: Int,
    val secondaryTextColor: Int,
    val cardBgColor: Int,
    val primaryColor: Int,
    val primaryContainerColor: Int,
    val onPrimaryContainerColor: Int,
    val highlightBgColor: Int,
    val highlightTextColor: Int,
    val highlightBlockTintColor: Int,
    val dividerColor: Int,
    val nextCardBgColor: Int,
    val nextCardStrokeColor: Int,
    val prevButtonStrokeColor: Int
)

sealed class ReaderItem {
    data class Header(
        val sourceName: String,
        val pubDate: String,
        val readTimeMinutes: Int,
        val articleIndex: Int,
        val totalArticles: Int,
        val title: String
    ) : ReaderItem()

    data class HeroImage(val url: String) : ReaderItem()

    data class Paragraph(
        val index: Int,
        val paragraph: ReaderParagraph
    ) : ReaderItem()

    data class FallbackText(val text: String) : ReaderItem()

    data class Footer(
        val nextArticle: Article?,
        val prevArticle: Article?,
        val articleIndex: Int,
        val totalArticles: Int
    ) : ReaderItem()
}

/**
 * High-performance native Android RecyclerView reader engine.
 * Delivers 120 FPS hardware-accelerated text and image scrolling,
 * instant native fling physics, and zero Compose layout recompositions.
 */
@Composable
fun NativeReaderRecyclerView(
    article: Article,
    paragraphs: List<ReaderParagraph>,
    fallbackText: String,
    heroImageUrl: String?,
    allArticleImages: List<String>,
    settings: ReaderSettings,
    colors: ReaderColorConfig,
    isTtsActive: Boolean,
    isReadingSummary: Boolean,
    ttsCurrentParagraphIndex: Int,
    ttsCurrentWordRange: Pair<Int, Int>?,
    activeBubbleParagraphIndex: Int?,
    nextArticle: Article?,
    prevArticle: Article?,
    currentArticleIndex: Int,
    totalArticlesCount: Int,
    onShowBubble: (Int) -> Unit,
    onDismissBubble: () -> Unit,
    onReadFromHere: (Int) -> Unit,
    onSeekToWord: (paragraphIndex: Int, charOffset: Int) -> Unit,
    onImageClick: (String) -> Unit,
    onNextArticle: () -> Unit,
    onPrevArticle: () -> Unit,
    onDeleteArticle: () -> Unit,
    onScrollProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentOnShowBubble = rememberUpdatedState(onShowBubble)
    val currentOnDismissBubble = rememberUpdatedState(onDismissBubble)
    val currentOnReadFromHere = rememberUpdatedState(onReadFromHere)
    val currentOnSeekToWord = rememberUpdatedState(onSeekToWord)
    val currentOnImageClick = rememberUpdatedState(onImageClick)
    val currentOnNextArticle = rememberUpdatedState(onNextArticle)
    val currentOnPrevArticle = rememberUpdatedState(onPrevArticle)
    val currentOnDeleteArticle = rememberUpdatedState(onDeleteArticle)
    val currentOnScrollProgress = rememberUpdatedState(onScrollProgress)

    val rootLayoutRef = remember { arrayOfNulls<FrameLayout>(1) }
    val floatingBubbleRef = remember { arrayOfNulls<LinearLayout>(1) }
    val recyclerViewRef = remember { arrayOfNulls<RecyclerView>(1) }
    val adapterRef = remember { arrayOfNulls<NativeReaderAdapter>(1) }

    // Scroll to beginning when opening new article
    LaunchedEffect(article.id) {
        recyclerViewRef[0]?.scrollToPosition(0)
    }

    // Prepare list of items
    val items = remember(article.id, paragraphs, fallbackText, heroImageUrl, settings.showImages, nextArticle?.id, prevArticle?.id) {
        val list = mutableListOf<ReaderItem>()
        // 1. Header
        list.add(
            ReaderItem.Header(
                sourceName = article.sourceName,
                pubDate = article.pubDate,
                readTimeMinutes = article.readTimeMinutes,
                articleIndex = currentArticleIndex,
                totalArticles = totalArticlesCount,
                title = article.title
            )
        )
        // 2. Hero Image
        if (settings.showImages && !heroImageUrl.isNullOrBlank()) {
            list.add(ReaderItem.HeroImage(heroImageUrl))
        }
        // 3. Paragraphs or Fallback
        if (paragraphs.isNotEmpty()) {
            paragraphs.forEachIndexed { idx, p ->
                list.add(ReaderItem.Paragraph(idx, p))
            }
        } else if (fallbackText.isNotBlank()) {
            val splitFallback = fallbackText
                .split(Regex("\\n\\s*\\n|\\n"))
                .map { com.example.data.remote.ArticleExtractor.cleanArticleText(it) }
                .filter { it.isNotBlank() }
            if (splitFallback.isNotEmpty()) {
                splitFallback.forEachIndexed { idx, block ->
                    list.add(ReaderItem.Paragraph(idx, ReaderParagraph(ParagraphType.BODY, block)))
                }
            } else {
                list.add(ReaderItem.FallbackText(com.example.data.remote.ArticleExtractor.cleanArticleText(fallbackText)))
            }
        }
        // 4. Footer
        if (nextArticle != null || prevArticle != null) {
            list.add(
                ReaderItem.Footer(
                    nextArticle = nextArticle,
                    prevArticle = prevArticle,
                    articleIndex = currentArticleIndex,
                    totalArticles = totalArticlesCount
                )
            )
        }
        list
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val density = ctx.resources.displayMetrics.density

            val rootLayout = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipChildren = false
                clipToPadding = false
            }
            rootLayoutRef[0] = rootLayout

            val layoutManager = LinearLayoutManager(ctx, RecyclerView.VERTICAL, false).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 8
            }

            var showFloatingBubbleAt: ((Int, Float, Float) -> Unit)? = null

            val adapter = NativeReaderAdapter(
                context = ctx,
                initialSettings = settings,
                initialColors = colors,
                onShowBubbleAt = { paraIndex, rawX, rawY ->
                    showFloatingBubbleAt?.invoke(paraIndex, rawX, rawY)
                },
                onReadFromHere = { currentOnReadFromHere.value(it) },
                onSeekToWord = { idx, off -> currentOnSeekToWord.value(idx, off) },
                onImageClick = { currentOnImageClick.value(it) },
                onNextArticle = { currentOnNextArticle.value() },
                onPrevArticle = { currentOnPrevArticle.value() },
                onDeleteArticle = { currentOnDeleteArticle.value() }
            )
            adapterRef[0] = adapter

            var lastReportedProgress = -1f

            val rv = RecyclerView(ctx).apply {
                this.layoutManager = layoutManager
                this.adapter = adapter
                this.itemAnimator = null // No jank from layout change animations
                this.setHasFixedSize(false)
                this.setItemViewCacheSize(15)
                this.clipToPadding = false
                this.clipChildren = false
                this.overScrollMode = View.OVER_SCROLL_ALWAYS
                this.setBackgroundColor(AndroidColor.TRANSPARENT)
                val padH = (16f * density).roundToInt()
                val padBottom = (32f * density).roundToInt()
                this.setPadding(padH, 0, padH, padBottom)

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            floatingBubbleRef[0]?.visibility = View.GONE
                            currentOnDismissBubble.value()
                        }
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val total = layoutManager.itemCount
                        if (total <= 1) {
                            if (lastReportedProgress != 0f) {
                                lastReportedProgress = 0f
                                currentOnScrollProgress.value(0f)
                            }
                            return
                        }
                        val firstPos = layoutManager.findFirstVisibleItemPosition()
                        val firstView = layoutManager.findViewByPosition(firstPos)
                        val firstTop = firstView?.top ?: 0
                        val itemHeight = firstView?.height?.coerceAtLeast(1) ?: 100
                        val currentPos = firstPos.toFloat() + (-firstTop.toFloat() / itemHeight)
                        val progress = (currentPos / (total - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                        if (kotlin.math.abs(progress - lastReportedProgress) >= 0.012f || progress == 0f || progress >= 0.99f) {
                            lastReportedProgress = progress
                            currentOnScrollProgress.value(progress)
                        }
                    }
                })
            }
            recyclerViewRef[0] = rv
            rootLayout.addView(rv, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            // Floating Bubble Overlay in Z-axis
            val floatingBubble = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = (16f * density).roundToInt()
                val padV = (9f * density).roundToInt()
                setPadding(padH, padV, padH, padV)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                elevation = 20f * density
                translationZ = 20f * density
                isClickable = true
                isFocusable = true
                visibility = View.GONE

                val icon = TextView(ctx).apply {
                    text = "▶  "
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
                addView(icon)

                val label = TextView(ctx).apply {
                    text = "Prečítať odtiaľto"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                addView(label)
            }
            floatingBubbleRef[0] = floatingBubble
            rootLayout.addView(floatingBubble)

            showFloatingBubbleAt = { paraIndex, rawX, rawY ->
                val bubble = floatingBubbleRef[0]
                val root = rootLayoutRef[0]
                if (bubble != null && root != null) {
                    val rootLoc = IntArray(2)
                    root.getLocationOnScreen(rootLoc)
                    val localX = rawX - rootLoc[0]
                    val localY = rawY - rootLoc[1]

                    bubble.background = GradientDrawable().apply {
                        setColor(colors.primaryContainerColor)
                        setStroke((1.5f * density).roundToInt(), colors.primaryColor)
                        cornerRadius = 24f * density
                    }
                    val icon = bubble.getChildAt(0) as TextView
                    val label = bubble.getChildAt(1) as TextView
                    icon.setTextColor(colors.onPrimaryContainerColor)
                    label.setTextColor(colors.onPrimaryContainerColor)

                    bubble.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    val bWidth = bubble.measuredWidth
                    val bHeight = bubble.measuredHeight

                    val margin = 16f * density
                    val clampedX = (localX - bWidth / 2f).coerceIn(margin, (root.width - bWidth - margin).coerceAtLeast(margin))
                    val targetY = if (localY - bHeight - 16f * density < margin) {
                        localY + 20f * density
                    } else {
                        localY - bHeight - 16f * density
                    }
                    val clampedY = targetY.coerceIn(margin, (root.height - bHeight - margin).coerceAtLeast(margin))

                    bubble.x = clampedX
                    bubble.y = clampedY
                    bubble.alpha = 0f
                    bubble.scaleX = 0.85f
                    bubble.scaleY = 0.85f
                    bubble.visibility = View.VISIBLE
                    bubble.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .start()

                    bubble.setOnClickListener {
                        bubble.visibility = View.GONE
                        currentOnReadFromHere.value(paraIndex)
                    }
                    currentOnShowBubble.value(paraIndex)
                }
            }

            rootLayout
        },
        update = { _ ->
            val adapter = adapterRef[0] ?: return@AndroidView
            if (activeBubbleParagraphIndex == null) {
                floatingBubbleRef[0]?.visibility = View.GONE
            }
            adapter.updateConfig(
                newSettings = settings,
                newColors = colors,
                isTtsActive = isTtsActive,
                isReadingSummary = isReadingSummary,
                activeParaIndex = ttsCurrentParagraphIndex,
                activeWordRange = ttsCurrentWordRange
            )
            adapter.submitList(items)
        },
        onRelease = { _ ->
            recyclerViewRef[0]?.adapter = null
            recyclerViewRef[0]?.clearOnScrollListeners()
            rootLayoutRef[0] = null
            floatingBubbleRef[0] = null
            recyclerViewRef[0] = null
            adapterRef[0] = null
        }
    )
}

private class NativeReaderAdapter(
    private val context: Context,
    private var initialSettings: ReaderSettings,
    private var initialColors: ReaderColorConfig,
    private val onShowBubbleAt: (paragraphIndex: Int, rawX: Float, rawY: Float) -> Unit,
    private val onReadFromHere: (Int) -> Unit,
    private val onSeekToWord: (Int, Int) -> Unit,
    private val onImageClick: (String) -> Unit,
    private val onNextArticle: () -> Unit,
    private val onPrevArticle: () -> Unit,
    private val onDeleteArticle: () -> Unit
) : ListAdapter<ReaderItem, RecyclerView.ViewHolder>(READER_DIFF_CALLBACK) {

    private val density = context.resources.displayMetrics.density
    private var settings = initialSettings
    private var readerColors = initialColors
    private var isTtsActive = false
    private var isReadingSummary = false
    private var activeParaIndex = -1
    private var activeWordRange: Pair<Int, Int>? = null

    private var activeTypeface: Typeface = resolveTypeface(context, settings.font)

    private fun findItemPositionForParaIndex(paraIndex: Int?): Int? {
        if (paraIndex == null) return null
        if (paraIndex == -1) return 0
        for (i in 0 until itemCount) {
            when (val item = getItem(i)) {
                is ReaderItem.Paragraph -> if (item.index == paraIndex) return i
                is ReaderItem.FallbackText -> if (paraIndex == 0) return i
                else -> {}
            }
        }
        return null
    }

    fun updateConfig(
        newSettings: ReaderSettings,
        newColors: ReaderColorConfig,
        isTtsActive: Boolean,
        isReadingSummary: Boolean,
        activeParaIndex: Int,
        activeWordRange: Pair<Int, Int>?
    ) {
        val fontChanged = settings.font != newSettings.font || settings.fontSizeSp != newSettings.fontSizeSp
        val colorsChanged = readerColors != newColors
        val oldActiveParaIndex = this.activeParaIndex
        val oldTtsActive = this.isTtsActive

        this.settings = newSettings
        this.readerColors = newColors
        this.isTtsActive = isTtsActive
        this.isReadingSummary = isReadingSummary
        this.activeParaIndex = activeParaIndex
        this.activeWordRange = activeWordRange

        if (fontChanged) {
            activeTypeface = resolveTypeface(context, newSettings.font)
        }

        if (fontChanged || colorsChanged) {
            // Full rebind only when styling, fonts or theme actually change
            notifyDataSetChanged()
        } else {
            // High efficiency targeted updates: zero image rebinds during TTS!
            if (oldActiveParaIndex != activeParaIndex) {
                findItemPositionForParaIndex(oldActiveParaIndex)?.let {
                    notifyItemChanged(it, PAYLOAD_TTS_WORD)
                }
                findItemPositionForParaIndex(activeParaIndex)?.let {
                    notifyItemChanged(it, PAYLOAD_TTS_WORD)
                }
            } else if (isTtsActive) {
                findItemPositionForParaIndex(activeParaIndex)?.let {
                    notifyItemChanged(it, PAYLOAD_TTS_WORD)
                }
            } else if (oldTtsActive && !isTtsActive) {
                findItemPositionForParaIndex(oldActiveParaIndex)?.let {
                    notifyItemChanged(it, PAYLOAD_TTS_WORD)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReaderItem.Header -> 0
            is ReaderItem.HeroImage -> 1
            is ReaderItem.Paragraph -> {
                val p = (getItem(position) as ReaderItem.Paragraph).paragraph
                if (p.type == ParagraphType.IMAGE) 3 else 2
            }
            is ReaderItem.FallbackText -> 2
            is ReaderItem.Footer -> 4
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> HeaderViewHolder(createHeaderView(parent.context))
            1 -> HeroImageViewHolder(createHeroImageView(parent.context))
            2 -> ParagraphViewHolder(createParagraphView(parent.context))
            3 -> BodyImageViewHolder(createBodyImageView(parent.context))
            4 -> FooterViewHolder(createFooterView(parent.context))
            else -> ParagraphViewHolder(createParagraphView(parent.context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ReaderItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ReaderItem.HeroImage -> (holder as HeroImageViewHolder).bind(item)
            is ReaderItem.Paragraph -> {
                if (item.paragraph.type == ParagraphType.IMAGE) {
                    (holder as BodyImageViewHolder).bind(item.paragraph)
                } else {
                    (holder as ParagraphViewHolder).bindParagraph(position, item.index, item.paragraph)
                }
            }
            is ReaderItem.FallbackText -> (holder as ParagraphViewHolder).bindFallback(item.text)
            is ReaderItem.Footer -> (holder as FooterViewHolder).bind(item)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            if (payloads.contains(PAYLOAD_TTS_WORD)) {
                when (holder) {
                    is HeaderViewHolder -> holder.updateTtsHighlight()
                    is ParagraphViewHolder -> holder.updateTtsHighlight()
                }
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }


    // --- View Creators ---

    private fun createHeaderView(ctx: Context): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, (8f * density).roundToInt(), 0, (8f * density).roundToInt())
        }

        // 1. Meta info row
        val metaRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Source badge pill
        val sourceBadge = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val padH = (10f * density).roundToInt()
            val padV = (4f * density).roundToInt()
            setPadding(padH, padV, padH, padV)
        }
        metaRow.addView(sourceBadge)

        // Metadata details (date, read time)
        val metaDetails = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            val marginL = (8f * density).roundToInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(marginL, 0, 0, 0) }
        }
        metaRow.addView(metaDetails)
        root.addView(metaRow)

        // 2. Article Title
        val titleText = TextView(ctx).apply {
            id = View.generateViewId()
            val marginT = (12f * density).roundToInt()
            val marginB = (6f * density).roundToInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, marginT, 0, marginB) }
        }
        root.addView(titleText)

        // 3. Divider
        val divider = View(ctx).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (1f * density).roundToInt()
            ).apply { setMargins(0, 0, 0, (8f * density).roundToInt()) }
        }
        root.addView(divider)

        return root
    }

    private fun createHeroImageView(ctx: Context): View {
        val root = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (14f * density).roundToInt()) }
        }

        val imageView = ImageView(ctx).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (230f * density).roundToInt()
            )
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 12f * density)
                }
            }
            clipToOutline = true
        }
        root.addView(imageView)
        return root
    }

    private fun createParagraphView(ctx: Context): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, (4f * density).roundToInt(), 0, (8f * density).roundToInt())
        }

        // 1. Content Frame (Paragraph text) placed first
        val contentFrame = LinearLayout(ctx).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Blockquote accent line
        val quoteBar = View(ctx).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                (4f * density).roundToInt(),
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, 0, (12f * density).roundToInt(), 0) }
            visibility = View.GONE
        }
        contentFrame.addView(quoteBar)

        // Bullet text for list item
        val bulletText = TextView(ctx).apply {
            id = View.generateViewId()
            text = "• "
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        contentFrame.addView(bulletText)

        // Main Paragraph TextView
        val paraTextView = TextView(ctx).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setLineSpacing(0f, 1.45f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                justificationMode = Layout.JUSTIFICATION_MODE_NONE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
        }
        contentFrame.addView(paraTextView)
        root.addView(contentFrame)

        return root
    }

    private fun createBodyImageView(ctx: Context): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, (6f * density).roundToInt(), 0, (12f * density).roundToInt()) }
        }

        val imageView = ImageView(ctx).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (220f * density).roundToInt()
            )
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                }
            }
            clipToOutline = true
        }
        root.addView(imageView)

        val captionText = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, (4f * density).roundToInt(), 0, 0) }
        }
        root.addView(captionText)

        return root
    }

    private fun createFooterView(ctx: Context): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, (20f * density).roundToInt(), 0, (40f * density).roundToInt()) }
        }

        // 1. Next Article Card
        val nextCard = LinearLayout(ctx).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            val pad = (16f * density).roundToInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (14f * density).roundToInt()) }
        }

        val nextLabel = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        nextCard.addView(nextLabel)

        val nextTitle = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            val marginV = (4f * density).roundToInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, marginV, 0, marginV) }
        }
        nextCard.addView(nextTitle)

        val nextSource = TextView(ctx).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        nextCard.addView(nextSource)

        // Centered advance button with prominent arrow
        val nextArrowBtn = LinearLayout(ctx).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val padV = (10f * density).roundToInt()
            val padH = (22f * density).roundToInt()
            setPadding(padH, padV, padH, padV)
            val marginT = (12f * density).roundToInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, marginT, 0, (2f * density).roundToInt())
            }
        }
        val nextArrowText = TextView(ctx).apply {
            id = View.generateViewId()
            text = "Čítať ďalej  ➔"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        nextArrowBtn.addView(nextArrowText)
        nextCard.addView(nextArrowBtn)
        root.addView(nextCard)

        // 2. Action Toolbar: [ ← Predchádzajúci ] [ 🗑 Vymazať článok ] [ Nasledujúci → ]
        val actionsRow = LinearLayout(ctx).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Prev Article Button
        val prevButton = TextView(ctx).apply {
            id = View.generateViewId()
            text = "← Predošlý"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            val padH = (12f * density).roundToInt()
            val padV = (11f * density).roundToInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, (6f * density).roundToInt(), 0)
            }
        }
        actionsRow.addView(prevButton)

        // Delete Article Button
        val deleteButton = TextView(ctx).apply {
            id = View.generateViewId()
            text = "🗑 Vymazať"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            val padH = (12f * density).roundToInt()
            val padV = (11f * density).roundToInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins((3f * density).roundToInt(), 0, (3f * density).roundToInt(), 0)
            }
        }
        actionsRow.addView(deleteButton)

        // Quick Next Button in row
        val quickNextButton = TextView(ctx).apply {
            id = View.generateViewId()
            text = "Ďalší →"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            val padH = (12f * density).roundToInt()
            val padV = (11f * density).roundToInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins((6f * density).roundToInt(), 0, 0, 0)
            }
        }
        actionsRow.addView(quickNextButton)

        root.addView(actionsRow)
        return root
    }

    // --- ViewHolders ---

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout = itemView as LinearLayout
        private val metaRow = rootLayout.getChildAt(0) as LinearLayout
        private val badgeView = metaRow.getChildAt(0) as TextView
        private val detailsView = metaRow.getChildAt(1) as TextView
        private val titleView = rootLayout.getChildAt(1) as TextView
        private val dividerView = rootLayout.getChildAt(2)

        private var currentTitle: String = ""
        private var lastTouchRawX = 0f
        private var lastTouchRawY = 0f

        fun bind(item: ReaderItem.Header) {
            currentTitle = item.title

            // Badge
            badgeView.text = item.sourceName
            badgeView.setTextColor(readerColors.primaryColor)
            badgeView.background = GradientDrawable().apply {
                setColor(this@NativeReaderAdapter.readerColors.cardBgColor)
                cornerRadius = 10f * density
            }

            // Details
            val indexStr = if (item.totalArticles > 1) " • ${item.articleIndex + 1} z ${item.totalArticles}" else ""
            detailsView.text = "• ${item.pubDate} • ${item.readTimeMinutes} min čítania$indexStr"
            detailsView.setTextColor(readerColors.secondaryTextColor)

            // Title
            currentTitle = com.example.data.remote.ArticleExtractor.cleanArticleText(item.title)
            titleView.typeface = Typeface.create(activeTypeface, Typeface.BOLD)
            titleView.paintFlags = titleView.paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp + 7.5f)
            updateTtsHighlight()

            // Touch listener to track raw coordinates for precise floating bubble and word seek
            titleView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    lastTouchRawX = event.rawX
                    lastTouchRawY = event.rawY
                } else if (event.action == MotionEvent.ACTION_UP && isTtsActive) {
                    val tv = v as TextView
                    val x = event.x.toInt() - tv.totalPaddingLeft + tv.scrollX
                    val y = event.y.toInt() - tv.totalPaddingTop + tv.scrollY
                    val layout = tv.layout
                    if (layout != null) {
                        val line = layout.getLineForVertical(y)
                        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                        val wordStart = findWordStartOffset(item.title, offset)
                        onSeekToWord(-1, wordStart)
                    }
                }
                false
            }

            // Long click to show floating bubble directly at touch location in Z-axis
            titleView.setOnLongClickListener {
                onShowBubbleAt(-1, lastTouchRawX, lastTouchRawY)
                true
            }

            // Divider
            dividerView.setBackgroundColor(this@NativeReaderAdapter.readerColors.dividerColor)
        }

        fun updateTtsHighlight() {
            val isTitleActive = isTtsActive && !isReadingSummary && (activeParaIndex == -1)
            if (isTitleActive && activeWordRange != null) {
                val span = SpannableStringBuilder(currentTitle)
                val (st, en) = activeWordRange!!
                if (st in 0 until en && en <= currentTitle.length) {
                    span.setSpan(BackgroundColorSpan(readerColors.highlightBgColor), st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    span.setSpan(ForegroundColorSpan(readerColors.highlightTextColor), st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                titleView.setText(span, TextView.BufferType.SPANNABLE)
            } else {
                titleView.text = currentTitle
                titleView.setTextColor(readerColors.textColor)
            }
        }
    }

    inner class HeroImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = (itemView as FrameLayout).getChildAt(0) as ImageView
        private var boundUrl: String? = null

        fun bind(item: ReaderItem.HeroImage) {
            if (boundUrl != item.url) {
                boundUrl = item.url
                imageView.load(item.url) {
                    size((360 * density).roundToInt(), (230 * density).roundToInt())
                    precision(Precision.INEXACT)
                    allowHardware(true)
                    crossfade(false)
                }
            }
            imageView.setOnClickListener {
                onImageClick(item.url)
            }
        }

        fun clear() {
            CoilUtils.dispose(imageView)
            imageView.setImageDrawable(null)
            boundUrl = null
        }
    }

    inner class ParagraphViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout = itemView as LinearLayout
        private val contentFrame = rootLayout.getChildAt(0) as LinearLayout
        private val quoteBar = contentFrame.getChildAt(0)
        private val bulletText = contentFrame.getChildAt(1) as TextView
        private val paraTextView = contentFrame.getChildAt(2) as TextView

        private var currentIndex: Int = -1
        private var currentText: String = ""
        private var currentType: ParagraphType = ParagraphType.BODY
        private var lastTouchRawX = 0f
        private var lastTouchRawY = 0f

        fun bindParagraph(adapterPos: Int, index: Int, para: ReaderParagraph) {
            val prevIsList = (currentList.getOrNull(adapterPos - 1) as? ReaderItem.Paragraph)?.paragraph?.type == ParagraphType.LIST_ITEM
            val nextIsList = (currentList.getOrNull(adapterPos + 1) as? ReaderItem.Paragraph)?.paragraph?.type == ParagraphType.LIST_ITEM
            val listMarker = para.caption?.takeIf { it.isNotBlank() } ?: "•"
            bindInternal(index, para.text, para.type, prevIsList, nextIsList, listMarker)
        }

        fun bindFallback(text: String) {
            bindInternal(0, text, ParagraphType.BODY, prevIsList = false, nextIsList = false, listMarker = "•")
        }

        private fun bindInternal(
            index: Int,
            text: String,
            type: ParagraphType,
            prevIsList: Boolean,
            nextIsList: Boolean,
            listMarker: String
        ) {
            val cleanedText = com.example.data.remote.ArticleExtractor.cleanArticleText(text)
            currentIndex = index
            currentText = cleanedText
            currentType = type

            // Configure based on type
            quoteBar.visibility = if (type == ParagraphType.BLOCKQUOTE) View.VISIBLE else View.GONE
            bulletText.visibility = if (type == ParagraphType.LIST_ITEM) View.VISIBLE else View.GONE

            when (type) {
                ParagraphType.HEADING_1 -> {
                    paraTextView.typeface = Typeface.create(activeTypeface, Typeface.BOLD)
                    paraTextView.paintFlags = paraTextView.paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                    paraTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp + 6.5f)
                    paraTextView.setLineSpacing(0f, 1.22f)
                    contentFrame.background = null
                    contentFrame.setPadding(0, 0, 0, 0)
                    rootLayout.setPadding(0, (22f * density).roundToInt(), 0, (8f * density).roundToInt())
                }
                ParagraphType.HEADING_2 -> {
                    paraTextView.typeface = Typeface.create(activeTypeface, Typeface.BOLD)
                    paraTextView.paintFlags = paraTextView.paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                    paraTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp + 4.5f)
                    paraTextView.setLineSpacing(0f, 1.25f)
                    contentFrame.background = null
                    contentFrame.setPadding(0, 0, 0, 0)
                    rootLayout.setPadding(0, (18f * density).roundToInt(), 0, (6f * density).roundToInt())
                }
                ParagraphType.BLOCKQUOTE -> {
                    paraTextView.typeface = Typeface.create(activeTypeface, Typeface.ITALIC)
                    paraTextView.paintFlags = paraTextView.paintFlags and android.graphics.Paint.FAKE_BOLD_TEXT_FLAG.inv()
                    paraTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp - 0.5f)
                    paraTextView.setLineSpacing(0f, 1.4f)
                    quoteBar.setBackgroundColor(readerColors.primaryColor)
                    contentFrame.background = GradientDrawable().apply {
                        setColor(this@NativeReaderAdapter.readerColors.cardBgColor)
                        cornerRadius = 8f * density
                    }
                    val pad = (14f * density).roundToInt()
                    contentFrame.setPadding(pad, pad, pad, pad)
                    rootLayout.setPadding(0, (8f * density).roundToInt(), 0, (10f * density).roundToInt())
                }
                ParagraphType.LIST_ITEM -> {
                    paraTextView.typeface = activeTypeface
                    paraTextView.paintFlags = paraTextView.paintFlags and android.graphics.Paint.FAKE_BOLD_TEXT_FLAG.inv()
                    paraTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
                    paraTextView.setLineSpacing(0f, 1.35f)
                    bulletText.text = "$listMarker "
                    bulletText.typeface = Typeface.create(activeTypeface, Typeface.BOLD)
                    bulletText.paintFlags = bulletText.paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
                    bulletText.minWidth = (24f * density).roundToInt()
                    bulletText.setTextColor(readerColors.primaryColor)
                    bulletText.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
                    contentFrame.background = null
                    contentFrame.setPadding((10f * density).roundToInt(), (1f * density).roundToInt(), 0, (1f * density).roundToInt())

                    // Tight spacing between consecutive list items, generous spacing above first and below last item
                    val topSpaceDp = if (!prevIsList) 16f else 2f
                    val bottomSpaceDp = if (!nextIsList) 18f else 2f
                    rootLayout.setPadding(
                        0,
                        (topSpaceDp * density).roundToInt(),
                        0,
                        (bottomSpaceDp * density).roundToInt()
                    )
                }
                else -> {
                    // BODY
                    paraTextView.typeface = activeTypeface
                    paraTextView.paintFlags = paraTextView.paintFlags and android.graphics.Paint.FAKE_BOLD_TEXT_FLAG.inv()
                    paraTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
                    paraTextView.setLineSpacing(0f, 1.45f)
                    contentFrame.background = null
                    contentFrame.setPadding(0, 0, 0, 0)
                    rootLayout.setPadding(0, (5f * density).roundToInt(), 0, (9f * density).roundToInt())
                }
            }

            // Highlighting
            updateTtsHighlight()

            // Touch listener to track raw coordinates for precise floating bubble and word seek
            paraTextView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    lastTouchRawX = event.rawX
                    lastTouchRawY = event.rawY
                } else if (event.action == MotionEvent.ACTION_UP && isTtsActive) {
                    val tv = v as TextView
                    val x = event.x.toInt() - tv.totalPaddingLeft + tv.scrollX
                    val y = event.y.toInt() - tv.totalPaddingTop + tv.scrollY
                    val layout = tv.layout
                    if (layout != null) {
                        val line = layout.getLineForVertical(y)
                        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                        val wordStart = findWordStartOffset(text, offset)
                        onSeekToWord(index, wordStart)
                    }
                }
                false
            }

            // Long click to show floating bubble directly at touch point without displacing text
            paraTextView.setOnLongClickListener {
                onShowBubbleAt(index, lastTouchRawX, lastTouchRawY)
                true
            }
        }

        fun updateTtsHighlight() {
            val isSpeaking = isTtsActive && !isReadingSummary && (activeParaIndex == currentIndex)
            if (isSpeaking && activeWordRange != null) {
                val span = SpannableStringBuilder(currentText)
                val (st, en) = activeWordRange!!
                if (st in 0 until en && en <= currentText.length) {
                    span.setSpan(BackgroundColorSpan(readerColors.highlightBgColor), st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    span.setSpan(ForegroundColorSpan(readerColors.highlightTextColor), st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                paraTextView.setText(span, TextView.BufferType.SPANNABLE)
            } else {
                paraTextView.text = currentText
                paraTextView.setTextColor(readerColors.textColor)
            }
        }
    }

    inner class BodyImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root = itemView as LinearLayout
        private val imageView = root.getChildAt(0) as ImageView
        private val captionView = root.getChildAt(1) as TextView
        private var boundUrl: String? = null

        fun bind(para: ReaderParagraph) {
            val url = para.imageUrl ?: ""
            if (boundUrl != url) {
                boundUrl = url
                imageView.load(url) {
                    size((360 * density).roundToInt(), (220 * density).roundToInt())
                    precision(Precision.INEXACT)
                    allowHardware(true)
                    crossfade(false)
                }
            }
            imageView.setOnClickListener {
                onImageClick(url)
            }

            if (!para.caption.isNullOrBlank()) {
                captionView.visibility = View.VISIBLE
                captionView.text = para.caption
                captionView.setTextColor(readerColors.secondaryTextColor)
            } else {
                captionView.visibility = View.GONE
            }
        }

        fun clear() {
            CoilUtils.dispose(imageView)
            imageView.setImageDrawable(null)
            boundUrl = null
        }
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root = itemView as LinearLayout
        private val nextCard = root.getChildAt(0) as LinearLayout
        private val actionsRow = root.getChildAt(1) as LinearLayout
        private val prevButton = actionsRow.getChildAt(0) as TextView
        private val deleteButton = actionsRow.getChildAt(1) as TextView
        private val quickNextButton = actionsRow.getChildAt(2) as TextView

        fun bind(item: ReaderItem.Footer) {
            val hasNext = item.nextArticle != null
            val hasPrev = item.prevArticle != null

            if (hasNext) {
                nextCard.visibility = View.VISIBLE
                quickNextButton.visibility = View.VISIBLE

                val label = nextCard.getChildAt(0) as TextView
                val title = nextCard.getChildAt(1) as TextView
                val source = nextCard.getChildAt(2) as TextView
                val arrowBtn = nextCard.getChildAt(3) as LinearLayout
                val arrowText = arrowBtn.getChildAt(0) as TextView

                val countStr = if (item.totalArticles > 1) " (${item.articleIndex + 2} z ${item.totalArticles})" else ""
                label.text = "NASLEDUJÚCI ČLÁNOK$countStr"
                label.setTextColor(readerColors.primaryColor)

                title.text = item.nextArticle!!.title
                title.setTextColor(readerColors.textColor)

                source.text = item.nextArticle.sourceName
                source.setTextColor(readerColors.secondaryTextColor)

                nextCard.background = GradientDrawable().apply {
                    setColor(this@NativeReaderAdapter.readerColors.nextCardBgColor)
                    setStroke((1f * density).roundToInt(), this@NativeReaderAdapter.readerColors.nextCardStrokeColor)
                    cornerRadius = 16f * density
                }

                arrowBtn.background = GradientDrawable().apply {
                    setColor(this@NativeReaderAdapter.readerColors.primaryColor)
                    cornerRadius = 24f * density
                }
                arrowText.setTextColor(readerColors.bgColor)

                nextCard.setOnClickListener { onNextArticle() }
                arrowBtn.setOnClickListener { onNextArticle() }
                quickNextButton.setOnClickListener { onNextArticle() }

                quickNextButton.setTextColor(readerColors.primaryColor)
                quickNextButton.background = GradientDrawable().apply {
                    setColor(this@NativeReaderAdapter.readerColors.cardBgColor)
                    setStroke((1f * density).roundToInt(), this@NativeReaderAdapter.readerColors.nextCardStrokeColor)
                    cornerRadius = 12f * density
                }
            } else {
                nextCard.visibility = View.GONE
                quickNextButton.visibility = View.GONE
            }

            if (hasPrev) {
                prevButton.visibility = View.VISIBLE
                prevButton.setTextColor(readerColors.primaryColor)
                prevButton.background = GradientDrawable().apply {
                    setColor(this@NativeReaderAdapter.readerColors.cardBgColor)
                    setStroke((1f * density).roundToInt(), this@NativeReaderAdapter.readerColors.prevButtonStrokeColor)
                    cornerRadius = 12f * density
                }
                prevButton.setOnClickListener { onPrevArticle() }
            } else {
                prevButton.visibility = View.GONE
            }

            // Always show Delete Article button
            deleteButton.visibility = View.VISIBLE
            deleteButton.setTextColor(readerColors.primaryColor)
            deleteButton.background = GradientDrawable().apply {
                setColor(this@NativeReaderAdapter.readerColors.cardBgColor)
                setStroke((1f * density).roundToInt(), this@NativeReaderAdapter.readerColors.prevButtonStrokeColor)
                cornerRadius = 12f * density
            }
            deleteButton.setOnClickListener { onDeleteArticle() }
        }
    }

}

private const val PAYLOAD_TTS_WORD = "payload_tts_word"

private fun resolveTypeface(context: Context, font: ReaderFont): Typeface {
    return when (font) {
        ReaderFont.SERIF -> Typeface.SERIF
        ReaderFont.SANS -> Typeface.SANS_SERIF
        ReaderFont.SLAB -> {
            try {
                ResourcesCompat.getFont(context, R.font.roboto_slab) ?: Typeface.SERIF
            } catch (e: Exception) {
                Typeface.SERIF
            }
        }
        ReaderFont.MONO -> Typeface.MONOSPACE
    }
}

private val READER_DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReaderItem>() {
    override fun areItemsTheSame(oldItem: ReaderItem, newItem: ReaderItem): Boolean {
        return when {
            oldItem is ReaderItem.Header && newItem is ReaderItem.Header -> oldItem.title == newItem.title
            oldItem is ReaderItem.HeroImage && newItem is ReaderItem.HeroImage -> oldItem.url == newItem.url
            oldItem is ReaderItem.Paragraph && newItem is ReaderItem.Paragraph ->
                oldItem.index == newItem.index && oldItem.paragraph.type == newItem.paragraph.type
            oldItem is ReaderItem.FallbackText && newItem is ReaderItem.FallbackText -> true
            oldItem is ReaderItem.Footer && newItem is ReaderItem.Footer -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ReaderItem, newItem: ReaderItem): Boolean {
        return oldItem == newItem
    }
}
