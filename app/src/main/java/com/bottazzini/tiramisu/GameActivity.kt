package com.bottazzini.tiramisu

import android.annotation.SuppressLint
import android.content.ClipData
import androidx.constraintlayout.widget.ConstraintLayout
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bottazzini.tiramisu.settings.*
import com.bottazzini.tiramisu.utils.*

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
        /** Visible height of each non-top card in a pile (dp). Top card shows fully. */
        private const val CARD_PEEK_DP = 24
        /** Card image native dimensions (e.g. piacentine_b1.png is 200×364). */
        private const val CARD_ASPECT_W = 200f
        private const val CARD_ASPECT_H = 364f
        /** ClipData label used to identify our drag events. */
        private const val DRAG_LABEL = "tiramisu_pile_drag"
        // Animation timing (ms)
        private const val DEAL_CARD_DURATION_MS   = 220L
        private const val DEAL_CARD_STAGGER_MS    = 80L
        private const val REDEAL_CARD_DURATION_MS = 200L
        private const val REDEAL_CARD_STAGGER_MS  = 60L
        private const val REDEAL_PILE_GAP_MS      = 150L
        private const val ACE_DURATION_MS         = 400L
        private const val ACE_STAGGER_MS          = 200L
    }

    // ---- ViewModel & Repos ----
    private val vm: TiramisuViewModel by lazy { ViewModelProvider(this)[TiramisuViewModel::class.java] }
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var gameStateRepo: GameStateRepository
    private lateinit var gameLogRepo: GameLogRepository
    private lateinit var recordsHandler: com.bottazzini.tiramisu.settings.RecordsHandler

    // ---- UI refs ----
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvStockCount: TextView
    private lateinit var tvRedealsLeft: TextView
    private lateinit var stockImage: ImageView
    private lateinit var stockArea: FrameLayout
    private lateinit var btnRedeal: Button
    private lateinit var btnHint: Button
    private lateinit var btnUndo: Button
    private lateinit var btnMenu: ImageButton
    private lateinit var gameRoot: View
    private val foundationViews = arrayOfNulls<ImageView>(4)
    private val pileContainers  = arrayOfNulls<LinearLayout>(4)
    private val pileScrollViews = arrayOfNulls<ScrollView>(4)
    private var firstLayoutDone  = false
    private var dragSourcePile: Int? = null
    private lateinit var tutorialOverlay: LinearLayout
    private lateinit var tvTutorialInstruction: TextView
    private lateinit var btnTutorialNext: Button

    // ---- State ----
    private var isTutorialMode = false
    private var tutorialEngine: TiramisuTutorialEngine? = null
    private var hintsUsedThisGame = 0
    private var cardType = "piacentine"
    private var cardBackKey = "bg2"
    private var hintedPileIdx: Int? = null
    private var mediaPlayer: MediaPlayer? = null
    private var soundsEnabled: Boolean = true
    /** True while a ghost animation (redeal or auto-ace) is in flight. Blocks all interactions except the pause/menu button. */
    private var isAnimating = false

    // ---- Timer ----
    private val timerHandler  = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.game)
        supportActionBar?.hide()

        bindViews()
        WindowInsetsUtils.applySystemBarInsets(window, gameRoot)
        setupListeners()

        settingsHandler = SettingsHandler(applicationContext)
        gameStateRepo   = GameStateRepository(applicationContext)
        gameLogRepo     = GameLogRepository(applicationContext)
        recordsHandler  = com.bottazzini.tiramisu.settings.RecordsHandler(applicationContext)

        isTutorialMode = intent.getBooleanExtra(EXTRA_TUTORIAL_MODE, false)
        val resume     = intent.getBooleanExtra("resume", false)

        cardType    = settingsHandler.readValue(Configuration.CARD_TYPE.value)  ?: "piacentine"
        cardBackKey = settingsHandler.readValue(Configuration.CARD_BACK.value)  ?: "bg2"
        val bg      = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        val bgResId = resources.getIdentifier(bg, "drawable", packageName)
        if (bgResId != 0) gameRoot.background = ContextCompat.getDrawable(this, bgResId)

        when {
            isTutorialMode -> startTutorial()
            resume         -> resumeGame()
            else           -> startNewGame()
        }
    }

    // ---- Initialisation ----

    private fun bindViews() {
        gameRoot       = findViewById(R.id.gameRoot)
        tvTimer        = findViewById(R.id.tvTimer)
        tvDifficulty   = findViewById(R.id.tvDifficulty)
        tvStockCount   = findViewById(R.id.tvStockCount)
        tvRedealsLeft  = findViewById(R.id.tvRedealsLeft)
        stockImage     = findViewById(R.id.stockImage)
        stockArea      = findViewById(R.id.stockArea)
        btnRedeal      = findViewById(R.id.btnRedeal)
        btnHint        = findViewById(R.id.btnHint)
        btnUndo        = findViewById(R.id.btnUndo)
        btnMenu        = findViewById(R.id.btnMenu)
        tutorialOverlay       = findViewById(R.id.tutorialOverlay)
        tvTutorialInstruction = findViewById(R.id.tvTutorialInstruction)
        btnTutorialNext       = findViewById(R.id.btnTutorialNext)

        for (i in 0..3) {
            foundationViews[i] = findViewById(resources.getIdentifier("foundation$i", "id", packageName))
            pileContainers[i]  = findViewById(resources.getIdentifier("pileContainer$i", "id", packageName))
            pileScrollViews[i] = findViewById(resources.getIdentifier("pileScroll$i", "id", packageName))
        }
    }

    private fun setupListeners() {
        stockArea.setOnClickListener { onStockTapped() }
        btnRedeal.setOnClickListener { onRedealTapped() }
        btnHint.setOnClickListener   { onHintTapped() }
        btnUndo.setOnClickListener   { onUndoTapped() }
        btnMenu.setOnClickListener   { showMenuDialog() }
        btnTutorialNext.setOnClickListener { advanceTutorial() }

        for (i in 0..3) {
            foundationViews[i]?.setOnClickListener { onFoundationViewTapped(i) }
            foundationViews[i]?.setOnDragListener(makeFoundationDropListener(i))
            pileScrollViews[i]?.setOnDragListener(makePileDropListener(i))
        }

        // Re-render once the layout has been measured so the pile column
        // width is known and we can size cards at their native aspect ratio.
        gameRoot.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (firstLayoutDone) return
                    firstLayoutDone = true
                    gameRoot.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (vm.state != null) renderAll()
                }
            }
        )
    }

    private fun startNewGame() {
        val diffKey    = settingsHandler.readValue(Configuration.DIFFICULTY.value) ?: Difficulty.NORMALE.key
        val difficulty = Difficulty.fromKey(diffKey)
        vm.newGame(difficulty)
        gameStateRepo.clear()
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
    }

    private fun resumeGame() {
        val saved = gameStateRepo.load()
        if (saved != null) {
            vm.restoreState(saved)
            restoreTimer(saved)
        } else {
            startNewGame()
        }
        renderAll()
    }

    private fun startTutorial() {
        vm.newTutorialGame()
        // Do NOT pre-deal here: step 1 asks the user to tap the tallone so they see
        // the ace (b1) animate to the foundation. Step 3 covers the second deal.
        val steps = TiramisuTutorialSteps.steps(resources)
        tutorialEngine = TiramisuTutorialEngine(steps)
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
        showTutorialStep()
    }

    // ---- Game interactions ----

    private fun onStockTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isStockDealStep()) return
        }
        val sizeBefore = vm.state?.piles?.map { it.size } ?: List(4) { 0 }
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            animateDeal(sizeBefore) {
                maybeAnimateAutoFoundation {
                    checkWin()
                    checkLost()
                    if (isTutorialMode) advanceTutorial()
                }
            }
        }
    }

    private fun onRedealTapped() {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isRedealStep()) return
        }
        if (!vm.canRedeal()) return
        animateRedeal()
    }

    /** Convenience: 2-element [x, y] screen coords. */
    private fun locationOnScreen(view: View): IntArray =
        IntArray(2).also { view.getLocationOnScreen(it) }

    /**
     * Animates each pile's cards sliding to the stock area, pile 3→0 sequentially.
     * Mutates state via [vm.redeal] up-front; the visual catch-up happens after
     * the ghost animations finish.
     */
    private fun animateRedeal() {
        val gameRootContainer = gameRoot as ConstraintLayout
        val gameRootPos = locationOnScreen(gameRootContainer)
        val stockPos = locationOnScreen(stockArea)

        data class GhostTask(val pileIdx: Int, val ghost: ImageView)
        val tasks = mutableListOf<GhostTask>()

        for (pileIdx in 3 downTo 0) {
            val container = pileContainers[pileIdx] ?: continue
            for (childIdx in (container.childCount - 1) downTo 0) {
                val original = container.getChildAt(childIdx) as? ImageView ?: continue
                val origPos = locationOnScreen(original)
                val ghost = ImageView(this).apply {
                    setImageDrawable(original.drawable)
                    scaleType = original.scaleType
                    layoutParams = ConstraintLayout.LayoutParams(original.width, original.height)
                    translationX = (origPos[0] - gameRootPos[0]).toFloat()
                    translationY = (origPos[1] - gameRootPos[1]).toFloat()
                }
                gameRootContainer.addView(ghost)
                original.alpha = 0f
                tasks.add(GhostTask(pileIdx, ghost))
            }
        }

        if (tasks.isEmpty()) {
            vm.redeal()
            renderAll()
            checkLost()
            return
        }

        isAnimating = true
        playSound(R.raw.flipcard)
        vm.redeal()

        val targetX = (stockPos[0] - gameRootPos[0]).toFloat()
        val targetY = (stockPos[1] - gameRootPos[1]).toFloat()

        var delay = 0L
        var prevPile = -1
        var lastStartDelay = 0L
        for (task in tasks) {
            if (prevPile != -1 && task.pileIdx != prevPile) {
                delay += REDEAL_PILE_GAP_MS
            }
            task.ghost.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(REDEAL_CARD_DURATION_MS)
                .setStartDelay(delay)
                .start()
            lastStartDelay = delay
            prevPile = task.pileIdx
            delay += REDEAL_CARD_STAGGER_MS
        }

        val totalDuration = lastStartDelay + REDEAL_CARD_DURATION_MS
        gameRoot.postDelayed({
            for (task in tasks) gameRootContainer.removeView(task.ghost)
            renderAll()
            isAnimating = false
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }, totalDuration)
    }

    /**
     * Animates each newly-dealt card flying from the stock area to its target pile,
     * one card at a time with a stagger. Must be called AFTER [vm.dealFromStock] has
     * updated game state but BEFORE [renderAll]. [sizeBefore] is the per-pile card count
     * captured before the deal so we can diff which piles received new cards.
     *
     * Cards that were dealt and immediately auto-moved (aces) won't appear in the diff;
     * their animation is handled separately by [maybeAnimateAutoFoundation] called from [onComplete].
     */
    private fun animateDeal(sizeBefore: List<Int>, onComplete: () -> Unit) {
        val s = vm.state ?: run { renderAll(); onComplete(); return }
        val gameRootContainer = gameRoot as ConstraintLayout
        val gameRootPos = locationOnScreen(gameRootContainer)
        val stockPos    = locationOnScreen(stockArea)
        val density     = resources.displayMetrics.density
        val peekPx      = (CARD_PEEK_DP * density).toInt()

        data class DealTask(val ghost: ImageView)
        val tasks = mutableListOf<DealTask>()
        var delay         = 0L
        var lastStartDelay = 0L

        for (pileIdx in 0..3) {
            val container = pileContainers[pileIdx] ?: continue
            val oldSize   = sizeBefore.getOrElse(pileIdx) { 0 }
            val pile      = s.piles[pileIdx]
            // Skip piles that didn't gain a card (or whose card was an ace and auto-removed)
            if (pile.size <= oldSize) continue

            val card      = pile.last()
            val cardWidth = container.width - container.paddingLeft - container.paddingRight
            if (cardWidth <= 0) continue
            val cardHeightPx = (cardWidth * (CARD_ASPECT_H / CARD_ASPECT_W)).toInt()

            // Target: the position the new top card will occupy in the rendered pile.
            // Card at position N in a pile starts at containerTop + N * peekPx.
            val contPos = locationOnScreen(container)
            val targetX = (contPos[0] - gameRootPos[0]).toFloat()
            val targetY = (contPos[1] + oldSize * peekPx - gameRootPos[1]).toFloat()

            val resId = resources.getIdentifier("${cardType}_$card", "drawable", packageName)
            val ghost = ImageView(this).apply {
                if (resId != 0) setImageResource(resId)
                scaleType    = ImageView.ScaleType.FIT_CENTER
                layoutParams = ConstraintLayout.LayoutParams(cardWidth, cardHeightPx)
                // Start at stock position
                translationX = (stockPos[0] - gameRootPos[0]).toFloat()
                translationY = (stockPos[1] - gameRootPos[1]).toFloat()
            }
            gameRootContainer.addView(ghost)
            tasks.add(DealTask(ghost))

            ghost.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(DEAL_CARD_DURATION_MS)
                .setStartDelay(delay)
                .start()
            lastStartDelay = delay
            delay += DEAL_CARD_STAGGER_MS
        }

        if (tasks.isEmpty()) {
            renderAll()
            onComplete()
            return
        }

        isAnimating = true
        val totalMs = lastStartDelay + DEAL_CARD_DURATION_MS
        gameRoot.postDelayed({
            for (task in tasks) gameRootContainer.removeView(task.ghost)
            isAnimating = false
            renderAll()
            onComplete()
        }, totalMs)
    }

    /**
     * If the ViewModel's last action queued any auto-ace moves, animate them.
     * Idempotent: consuming the list clears it so the next render won't replay.
     * Invokes [onComplete] once any animation has finished — or immediately if
     * there is nothing to animate — so that callers can sequence end-of-turn
     * checks (checkWin, checkLost, advanceTutorial) after the cards have
     * visibly landed instead of cutting them off mid-flight.
     */
    private fun maybeAnimateAutoFoundation(onComplete: () -> Unit = {}) {
        val moves = vm.consumeAutoFoundationMoves()
        if (moves.isEmpty()) { onComplete(); return }
        animateAutoFoundation(moves, onComplete)
    }

    private fun animateAutoFoundation(
        moves: List<AutoFoundationMove>,
        onComplete: () -> Unit = {}
    ) {
        val gameRootContainer = gameRoot as ConstraintLayout
        val gameRootPos = locationOnScreen(gameRootContainer)
        val density = resources.displayMetrics.density
        val peekPx = (CARD_PEEK_DP * density).toInt()
        val ghosts = mutableListOf<ImageView>()
        val hiddenFoundations = mutableListOf<ImageView>()

        // renderAll has already run; pile containers reflect the POST-chain state.
        // For PILE_TOP-source moves we reconstruct each card's original row:
        // preSize[P] = current childCount + (moves from P), and the k-th move from
        // P sat at row (preSize[P] - 1 - k) in pile-removal order.
        val perPileTotal = IntArray(4)
        val perPileProcessed = IntArray(4)
        for (m in moves) perPileTotal[m.fromPile]++

        for ((idx, move) in moves.withIndex()) {
            val destView = foundationViews[move.toFoundation] ?: continue
            val resId = resources.getIdentifier("${cardType}_${move.card}", "drawable", packageName)
            if (resId == 0) continue

            val sourceLoc = when (move.source) {
                AutoFoundationSource.STOCK -> locationOnScreen(stockArea)
                AutoFoundationSource.PILE_TOP -> {
                    val container = pileContainers[move.fromPile]
                    if (container != null) {
                        val k = perPileProcessed[move.fromPile]
                        val preSize = container.childCount + perPileTotal[move.fromPile]
                        val rowIdx = (preSize - 1 - k).coerceAtLeast(0)
                        val contPos = locationOnScreen(container)
                        intArrayOf(contPos[0], contPos[1] + rowIdx * peekPx)
                    } else locationOnScreen(stockArea)
                }
            }
            perPileProcessed[move.fromPile]++

            val destLoc = locationOnScreen(destView)

            destView.alpha = 0f
            hiddenFoundations.add(destView)

            val ghost = ImageView(this).apply {
                setImageResource(resId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ConstraintLayout.LayoutParams(destView.width, destView.height)
                translationX = (sourceLoc[0] - gameRootPos[0]).toFloat()
                translationY = (sourceLoc[1] - gameRootPos[1]).toFloat()
            }
            gameRootContainer.addView(ghost)
            ghosts.add(ghost)

            ghost.animate()
                .translationX((destLoc[0] - gameRootPos[0]).toFloat())
                .translationY((destLoc[1] - gameRootPos[1]).toFloat())
                .setDuration(ACE_DURATION_MS)
                .setStartDelay(idx * ACE_STAGGER_MS)
                .start()
        }

        if (ghosts.isEmpty()) { onComplete(); return }

        isAnimating = true
        val totalDuration = (moves.size - 1) * ACE_STAGGER_MS + ACE_DURATION_MS
        gameRoot.postDelayed({
            for (ghost in ghosts) gameRootContainer.removeView(ghost)
            for (view in hiddenFoundations) view.alpha = 1f
            isAnimating = false
            onComplete()
        }, totalDuration)
    }

    private fun onPileCardTapped(pileIdx: Int) {
        if (isAnimating) return
        if (isTutorialMode) {
            val eng      = tutorialEngine ?: return
            val card     = vm.state?.topOfPile(pileIdx) ?: return
            val selected = vm.selectedPileIndex
            if (selected == null) {
                // First tap: selecting the source pile — must match requiredMove.sourcePile
                if (!eng.isPileTapAllowed(pileIdx, card)) return
            } else if (pileIdx != selected) {
                // Second tap on a different pile: completing a pile→pile move
                // Must be the correct destination for the current tutorial step
                if (!eng.isCorrectPileMove(selected, pileIdx)) {
                    showInvalidMoveToast()
                    return
                }
            }
            // pileIdx == selected → deselecting, always allowed
        }

        val result = vm.onPileTapped(pileIdx)
        when (result) {
            TapResult.MOVED   -> {
                playSound(R.raw.flipcard)
                renderAll()
                maybeAnimateAutoFoundation {
                    checkWin()
                    checkLost()
                    if (isTutorialMode) advanceTutorial()
                }
            }
            TapResult.INVALID -> showInvalidMoveToast()
            else               -> renderAll()
        }
    }

    private fun onFoundationViewTapped(foundationIdx: Int) {
        if (isAnimating) return
        val sel = vm.selectedPileIndex ?: return
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isCorrectFoundationMove(sel)) {
                showInvalidMoveToast()
                return
            }
        }
        if (vm.onFoundationTapped(sel)) {
            playSound(R.raw.flipcard)
            renderAll()
            maybeAnimateAutoFoundation {
                checkWin()
                checkLost()
                if (isTutorialMode) advanceTutorial()
            }
        }
    }

    private fun onHintTapped() {
        if (isAnimating) return
        val s    = vm.state ?: return
        val hint = TiramisuSolver.findHint(s)
        hintsUsedThisGame++
        if (hint == null) {
            Toast.makeText(this, "Nessuna mossa disponibile", Toast.LENGTH_SHORT).show()
            return
        }
        hintedPileIdx = hint.fromPile
        renderAll()
        timerHandler.postDelayed({ hintedPileIdx = null; renderAll() }, 1500)
    }

    private fun onUndoTapped() {
        if (isAnimating) return
        if (isTutorialMode) return
        if (vm.undo()) {
            playSound(R.raw.flipcard)
            renderAll()
        }
    }

    // ---- Rendering ----

    private fun renderAll() {
        val s = vm.state ?: return
        renderFoundations(s)
        for (i in 0..3) renderPile(i, s)
        renderBottomBar(s)
        tvDifficulty.text = s.difficulty.displayName
    }

    private fun renderFoundations(s: TiramisuGameState) {
        // Tutorial: if the current step requires a foundation move, find which
        // foundation index will receive the card from the source pile and paint
        // that one green ("drop here"). The mapping is dynamic because foundations
        // are assigned by suit on first ace.
        val tutFoundationIdx: Int? = run {
            if (!isTutorialMode) return@run null
            val step = tutorialEngine?.currentStep() ?: return@run null
            val req  = step.requiredMove ?: return@run null
            if (req.sourcePile < 0 || req.targetPile != -1) return@run null
            val srcCard = s.topOfPile(req.sourcePile)
            if (srcCard == "zero") return@run null
            s.foundations.indexOfFirst { f ->
                TiramisuMoveValidator.canMoveToFoundation(srcCard, f)
            }.takeIf { it >= 0 }
        }

        for (i in 0..3) {
            val view = foundationViews[i] ?: continue
            val top  = s.foundations[i]
            if (top == "zero") {
                view.setImageResource(R.drawable.zero)
                view.contentDescription = getString(R.string.foundation_empty_desc, i + 1)
            } else {
                val resId = resources.getIdentifier("${cardType}_$top", "drawable", packageName)
                if (resId != 0) view.setImageResource(resId)
                view.contentDescription = cardDescription(top)
            }
            if (i == tutFoundationIdx) {
                view.setColorFilter(0xCC00AA50.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
            } else {
                view.clearColorFilter()
            }
        }
    }

    private fun renderPile(pileIdx: Int, s: TiramisuGameState) {
        val container = pileContainers[pileIdx] ?: return
        container.removeAllViews()
        val pile      = s.piles[pileIdx]
        val density   = resources.displayMetrics.density
        val peekPx    = (CARD_PEEK_DP * density).toInt()

        // The pile column width is only known after the first layout pass.
        // Skip rendering until then — the OnGlobalLayoutListener will retry.
        val cardWidth = container.width - container.paddingLeft - container.paddingRight
        if (cardWidth <= 0) return

        // Each card keeps its native aspect ratio (e.g. 200×364 → height ≈ width × 1.82).
        val cardHeightPx = (cardWidth * (CARD_ASPECT_H / CARD_ASPECT_W)).toInt()

        val tutStep          = if (isTutorialMode) tutorialEngine?.currentStep() else null
        val isTutorialSource = tutStep?.highlightSource == pileIdx
        val isTutorialTarget = tutStep?.highlightTarget == pileIdx

        // Tutorial column glow — applied once for both empty and non-empty piles so the
        // highlight is always visible regardless of card colour (e.g. dark bastoni images).
        container.setBackgroundColor(when {
            isTutorialSource -> 0x66FF8C00.toInt()               // orange: "drag from here"
            isTutorialTarget -> 0x5500AA50.toInt()               // green:  "drop here"
            else             -> android.graphics.Color.TRANSPARENT
        })

        if (pile.isEmpty()) {
            val placeholder = ImageView(this)
            placeholder.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, cardHeightPx)
            placeholder.setImageResource(R.drawable.zero)
            placeholder.scaleType = ImageView.ScaleType.FIT_CENTER
            placeholder.contentDescription = getString(R.string.pile_empty_desc, pileIdx + 1)
            placeholder.alpha = 0.4f
            container.addView(placeholder)
            return
        }

        val isSelected  = vm.selectedPileIndex == pileIdx
        val isObbligato = vm.obbligatoTargets().contains(pileIdx)
        val isHinted    = hintedPileIdx == pileIdx

        pile.forEachIndexed { cardIdx, card ->
            val imageView = ImageView(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, cardHeightPx)
            // Overlap each non-first card with the previous so only `peekPx` of the
            // top strip remains visible — full card image, no horizontal stretch.
            if (cardIdx > 0) params.topMargin = peekPx - cardHeightPx
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.adjustViewBounds = false

            val resId = resources.getIdentifier("${cardType}_$card", "drawable", packageName)
            if (resId != 0) imageView.setImageResource(resId)
            imageView.contentDescription = cardDescription(card)

            if (cardIdx == pile.lastIndex) {
                imageView.isFocusable = true
                imageView.isClickable = true
                imageView.setOnClickListener { onPileCardTapped(pileIdx) }
                attachInstantDragListener(imageView, pileIdx)
                when {
                    isSelected         -> imageView.alpha = 0.7f
                    // Tutorial: orange = "drag this card", green = "drop here"
                    isTutorialSource   -> imageView.setColorFilter(
                        0xCCFF8C00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isTutorialTarget   -> imageView.setColorFilter(
                        0xCC00AA50.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isObbligato        -> imageView.setColorFilter(
                        0x88FF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isHinted           -> imageView.setColorFilter(
                        0x8800FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    else               -> { imageView.alpha = 1f; imageView.clearColorFilter() }
                }
            } else {
                imageView.isFocusable = false
                imageView.isClickable = false
            }

            container.addView(imageView)
        }

        pileScrollViews[pileIdx]?.post {
            pileScrollViews[pileIdx]?.fullScroll(View.FOCUS_DOWN)
        }
    }

    // ---- Drag & drop ----

    /**
     * Instant drag (no long-press): once the finger has moved past the system
     * touchSlop on the top card, kick off [startCardDrag]. We also tell the
     * parent ScrollView not to intercept the gesture so a short vertical drag
     * doesn't get stolen as a scroll. If the gesture never crosses the slop,
     * the touch falls through to the click listener and behaves as a tap.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachInstantDragListener(view: View, pileIdx: Int) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var dragStarted = false
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    dragStarted = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragStarted) {
                        true
                    } else {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (dx * dx + dy * dy > slop * slop) {
                            if (startCardDrag(v, pileIdx)) {
                                dragStarted = true
                                true
                            } else {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                false
                            }
                        } else false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
    }

    private fun startCardDrag(v: View, pileIdx: Int): Boolean {
        if (isAnimating) return false
        if (isTutorialMode) {
            val eng  = tutorialEngine ?: return false
            val card = vm.state?.topOfPile(pileIdx) ?: return false
            if (!eng.isPileTapAllowed(pileIdx, card)) return false
        }
        if (vm.state?.topOfPile(pileIdx) == "zero") return false

        dragSourcePile = pileIdx
        val clip   = ClipData.newPlainText(DRAG_LABEL, pileIdx.toString())
        val shadow = View.DragShadowBuilder(v)
        return v.startDragAndDrop(clip, shadow, pileIdx, 0)
    }

    private fun makePileDropListener(targetPileIdx: Int) = View.OnDragListener { view, event ->
        if (event.clipDescription?.label != DRAG_LABEL) return@OnDragListener false
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                val src = dragSourcePile
                if (src != null && vm.canMoveBetweenPiles(src, targetPileIdx)) {
                    view.foreground = ContextCompat.getDrawable(
                        this, R.drawable.casino_drop_zone_valid)
                }
                true
            }
            DragEvent.ACTION_DRAG_EXITED  -> { view.foreground = null; true }
            DragEvent.ACTION_DROP         -> {
                view.foreground = null
                val src = dragSourcePile ?: return@OnDragListener false
                handlePileDrop(src, targetPileIdx)
            }
            DragEvent.ACTION_DRAG_ENDED   -> { view.foreground = null; true }
            else -> true
        }
    }

    private fun makeFoundationDropListener(foundationIdx: Int) = View.OnDragListener { view, event ->
        if (event.clipDescription?.label != DRAG_LABEL) return@OnDragListener false
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                val src = dragSourcePile
                if (src != null && vm.canMoveTopToAnyFoundation(src)) {
                    view.foreground = ContextCompat.getDrawable(
                        this, R.drawable.casino_drop_zone_valid)
                }
                true
            }
            DragEvent.ACTION_DRAG_EXITED  -> { view.foreground = null; true }
            DragEvent.ACTION_DROP         -> {
                view.foreground = null
                val src = dragSourcePile ?: return@OnDragListener false
                handleFoundationDrop(src)
            }
            DragEvent.ACTION_DRAG_ENDED   -> {
                view.foreground = null
                if (event.action == DragEvent.ACTION_DRAG_ENDED) dragSourcePile = null
                true
            }
            else -> true
        }
    }

    private fun handlePileDrop(srcPile: Int, dstPile: Int): Boolean {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return false
            if (!eng.isCorrectPileMove(srcPile, dstPile)) {
                showInvalidMoveToast()
                return false
            }
        }
        if (!vm.tryMoveBetweenPiles(srcPile, dstPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoFoundation {
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
        return true
    }

    private fun handleFoundationDrop(srcPile: Int): Boolean {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return false
            if (!eng.isCorrectFoundationMove(srcPile)) {
                showInvalidMoveToast()
                return false
            }
        }
        if (!vm.onFoundationTapped(srcPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        maybeAnimateAutoFoundation {
            checkWin()
            checkLost()
            if (isTutorialMode) advanceTutorial()
        }
        return true
    }

    private fun renderBottomBar(s: TiramisuGameState) {
        tvRedealsLeft.text = getString(R.string.redeals_left, s.redealsLeft)

        val canRedeal = vm.canRedeal()
        btnRedeal.isVisible = canRedeal

        btnUndo.isVisible = !isTutorialMode
        btnUndo.isEnabled = vm.canUndo()

        if (s.stock.isEmpty()) {
            // Hide card image and count badge — the slot frame background remains visible
            // so the user clearly sees an empty slot rather than a misleading full card.
            stockImage.visibility = View.INVISIBLE
            tvStockCount.visibility = View.INVISIBLE
        } else {
            stockImage.visibility = View.VISIBLE
            tvStockCount.visibility = View.VISIBLE
            tvStockCount.text = "${s.stock.size}"
            val backResId = resources.getIdentifier(cardBackKey, "drawable", packageName)
            if (backResId != 0) stockImage.setImageResource(backResId)
            stockImage.alpha = 1f
        }
    }

    private fun cardDescription(card: String): String {
        val suitName = when (TiramisuMoveValidator.suit(card)) {
            "b" -> "Bastoni"
            "c" -> "Coppe"
            "d" -> "Denari"
            "s" -> "Spade"
            else -> card.substring(0, 1)
        }
        val rankName = when (TiramisuMoveValidator.rank(card)) {
            1    -> "Asso"
            else -> TiramisuMoveValidator.rank(card).toString()
        }
        return getString(R.string.card_desc, rankName, suitName)
    }

    // ---- Win / End ----

    private fun checkWin() {
        if (!vm.isWon()) return
        stopTimer()
        val s = vm.state!!
        s.hasActiveGame = false  // prevent onPause() from re-saving after clear()
        val durationMs = System.currentTimeMillis() - s.gameStartTimeMillis
        gameLogRepo.insert(GameLog(
            timestamp   = System.currentTimeMillis(),
            durationMs  = durationMs,
            won         = true,
            hintsUsed   = hintsUsedThisGame,
            difficulty  = s.difficulty.key,
            redealsUsed = s.difficulty.redeals - s.redealsLeft
        ))
        gameStateRepo.clear()
        val intent = Intent(this, YouWonActivity::class.java).apply {
            putExtra("duration_ms", durationMs)
            putExtra("difficulty",  s.difficulty.key)
        }
        startActivity(intent)
        finish()
    }

    private fun checkLost() {
        if (isTutorialMode) return
        if (!vm.isLost()) return
        stopTimer()
        val s = vm.state ?: return
        s.hasActiveGame = false  // prevent onPause() from re-saving after clear()
        val durationMs = System.currentTimeMillis() - s.gameStartTimeMillis - s.timerPausedMs
        gameLogRepo.insert(GameLog(
            timestamp   = System.currentTimeMillis(),
            durationMs  = durationMs,
            won         = false,
            hintsUsed   = hintsUsedThisGame,
            difficulty  = s.difficulty.key,
            redealsUsed = s.difficulty.redeals - s.redealsLeft
        ))
        gameStateRepo.clear()
        recordsHandler.resetStreak()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.lost_title))
            .setMessage(getString(R.string.lost_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.lost_retry)) { _, _ -> retrySameGame() }
            .setNegativeButton(getString(R.string.lost_new))   { _, _ -> startNewGame() }
            .show()
    }

    private fun retrySameGame() {
        vm.retrySameGame()
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
    }

    // ---- Tutorial ----

    private fun showTutorialStep() {
        val eng = tutorialEngine ?: return
        if (eng.isComplete()) { endTutorial(); return }
        val step = eng.currentStep()
        tutorialOverlay.visibility = View.VISIBLE
        tvTutorialInstruction.text = getString(step.instructionResId)
        btnTutorialNext.isVisible  = step.requiredMove == null
    }

    private fun advanceTutorial() {
        val eng = tutorialEngine ?: return
        eng.advanceToNext()
        if (eng.isComplete()) { endTutorial(); return }
        showTutorialStep()
        renderAll()
    }

    private fun endTutorial() {
        tutorialOverlay.visibility = View.GONE
        tutorialEngine             = null
        isTutorialMode             = false
        vm.state?.hasActiveGame    = false  // isTutorialMode is now false, prevent onPause() save
        stopTimer()

        // Unlock "tutorial_done" achievement
        AchievementEngine.create(applicationContext)
            .evaluate(AchievementTrigger.TUTORIAL_COMPLETED)

        // Show completion dialog then navigate back to main menu
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tutorial_complete_title))
            .setMessage(getString(R.string.tutorial_complete_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.tutorial_complete_ok)) { _, _ ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                finish()
            }
            .show()
    }

    private fun showInvalidMoveToast() {
        val msg = if (vm.obbligatoTargets().isNotEmpty())
            getString(R.string.obbligato_hint)
        else
            getString(R.string.invaild_move)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ---- Menu ----

    private fun showMenuDialog() {
        stopTimer()
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(arrayOf("↩ Riprendi", "🏠 Abbandona")) { _, which ->
                when (which) {
                    0 -> startTimer()
                    1 -> abandonGame()
                }
            }
            .setOnCancelListener { startTimer() }
            .show()
    }

    private fun abandonGame() {
        val s = vm.state
        if (s != null) {
            s.hasActiveGame = false  // prevent onPause() from re-saving after clear()
            val durationMs = System.currentTimeMillis() - s.gameStartTimeMillis
            gameLogRepo.insert(GameLog(
                timestamp   = System.currentTimeMillis(),
                durationMs  = durationMs,
                won         = false,
                hintsUsed   = hintsUsedThisGame,
                difficulty  = s.difficulty.key,
                redealsUsed = s.difficulty.redeals - s.redealsLeft
            ))
        }
        gameStateRepo.clear()
        recordsHandler.resetStreak()
        finish()
    }

    // ---- Timer ----

    private fun startTimer() {
        val s = vm.state ?: return
        if (s.gameStartTimeMillis == 0L) s.gameStartTimeMillis = System.currentTimeMillis()
        s.isTimerPaused = false
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - s.gameStartTimeMillis - s.timerPausedMs
                tvTimer.text = TimeUtils.formatTime(elapsed)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        val s = vm.state ?: return
        if (::timerRunnable.isInitialized) timerHandler.removeCallbacks(timerRunnable)
        s.isTimerPaused = true
    }

    private fun restoreTimer(s: TiramisuGameState) {
        if (s.isTimerPaused) {
            s.timerPausedMs += System.currentTimeMillis() - (s.gameStartTimeMillis + s.timerPausedMs)
        }
        startTimer()
    }

    // ---- Lifecycle ----

    override fun onPause() {
        super.onPause()
        stopTimer()
        val s = vm.state
        if (s != null && s.hasActiveGame && !isTutorialMode) {
            gameStateRepo.save(s)
        }
    }

    override fun onResume() {
        super.onResume()
        soundsEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
        // Tutorial: autoComplete is always off — otherwise b2 would self-promote on
        // the second deal and skip the foundation step the user is being taught.
        vm.autoCompleteEnabled = !isTutorialMode &&
            settingsHandler.readValue(Configuration.AUTO_MOVE.value) == "enabled"
        if (vm.state?.hasActiveGame == true) startTimer()
    }

    override fun onDestroy() {
        gameStateRepo.close()
        gameLogRepo.close()
        settingsHandler.close()
        mediaPlayer?.release()
        super.onDestroy()
    }

    // ---- Helpers ----

    private fun playSound(resId: Int) {
        if (!soundsEnabled) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)?.also {
                it.setOnCompletionListener { mp -> mp.release() }
                it.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
