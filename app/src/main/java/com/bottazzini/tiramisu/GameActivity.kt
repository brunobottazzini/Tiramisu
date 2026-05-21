package com.bottazzini.tiramisu

import android.content.ClipData
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.View
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
    }

    // ---- ViewModel & Repos ----
    private val vm: TiramisuViewModel by lazy { ViewModelProvider(this)[TiramisuViewModel::class.java] }
    private lateinit var settingsHandler: SettingsHandler
    private lateinit var gameStateRepo: GameStateRepository
    private lateinit var gameLogRepo: GameLogRepository

    // ---- UI refs ----
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvStockCount: TextView
    private lateinit var tvRedealsLeft: TextView
    private lateinit var stockImage: ImageView
    private lateinit var stockArea: FrameLayout
    private lateinit var btnRedeal: Button
    private lateinit var btnHint: Button
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
        val steps = TiramisuTutorialSteps.steps(resources)
        tutorialEngine = TiramisuTutorialEngine(steps)
        hintsUsedThisGame = 0
        startTimer()
        renderAll()
        showTutorialStep()
    }

    // ---- Game interactions ----

    private fun onStockTapped() {
        if (isTutorialMode) {
            val eng = tutorialEngine ?: return
            if (!eng.isStockDealStep()) return
        }
        if (vm.dealFromStock()) {
            playSound(R.raw.flipcard)
            renderAll()
            checkWin()
        }
    }

    private fun onRedealTapped() {
        if (vm.redeal()) {
            playSound(R.raw.flipcard)
            renderAll()
        }
    }

    private fun onPileCardTapped(pileIdx: Int) {
        if (isTutorialMode) {
            val eng  = tutorialEngine ?: return
            val card = vm.state?.topOfPile(pileIdx) ?: return
            if (!eng.isPileTapAllowed(pileIdx, card)) return
        }

        val result = vm.onPileTapped(pileIdx)
        when (result) {
            TapResult.MOVED   -> {
                playSound(R.raw.flipcard)
                renderAll()
                checkWin()
                if (isTutorialMode) advanceTutorial()
            }
            TapResult.INVALID -> showInvalidMoveToast()
            else               -> renderAll()
        }
    }

    private fun onFoundationViewTapped(foundationIdx: Int) {
        val sel = vm.selectedPileIndex ?: return
        if (vm.onFoundationTapped(sel)) {
            playSound(R.raw.flipcard)
            renderAll()
            checkWin()
            if (isTutorialMode) advanceTutorial()
        }
    }

    private fun onHintTapped() {
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

    // ---- Rendering ----

    private fun renderAll() {
        val s = vm.state ?: return
        renderFoundations(s)
        for (i in 0..3) renderPile(i, s)
        renderBottomBar(s)
        tvDifficulty.text = s.difficulty.displayName
    }

    private fun renderFoundations(s: TiramisuGameState) {
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
                imageView.setOnLongClickListener { v -> startCardDrag(v, pileIdx) }
                when {
                    isSelected  -> imageView.alpha = 0.7f
                    isObbligato -> imageView.setColorFilter(
                        0x88FF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    isHinted    -> imageView.setColorFilter(
                        0x8800FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
                    else        -> { imageView.alpha = 1f; imageView.clearColorFilter() }
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

    private fun startCardDrag(v: View, pileIdx: Int): Boolean {
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
        if (!vm.tryMoveBetweenPiles(srcPile, dstPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        checkWin()
        if (isTutorialMode) advanceTutorial()
        return true
    }

    private fun handleFoundationDrop(srcPile: Int): Boolean {
        if (!vm.onFoundationTapped(srcPile)) {
            showInvalidMoveToast()
            return false
        }
        playSound(R.raw.flipcard)
        renderAll()
        checkWin()
        if (isTutorialMode) advanceTutorial()
        return true
    }

    private fun renderBottomBar(s: TiramisuGameState) {
        tvStockCount.text  = "${s.stock.size}"
        tvRedealsLeft.text = getString(R.string.redeals_left, s.redealsLeft)

        val canRedeal = vm.canRedeal()
        btnRedeal.isVisible = canRedeal

        if (s.stock.isEmpty() && !canRedeal) {
            stockImage.setImageResource(R.drawable.zero)
            stockImage.alpha = 0.3f
        } else {
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
        Toast.makeText(this, "Tutorial completato! Buon gioco!", Toast.LENGTH_LONG).show()
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
