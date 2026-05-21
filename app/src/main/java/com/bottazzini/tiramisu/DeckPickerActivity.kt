package com.bottazzini.tiramisu

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.tiramisu.settings.Configuration
import com.bottazzini.tiramisu.settings.SettingsHandler
import com.bottazzini.tiramisu.utils.CardDeck
import com.bottazzini.tiramisu.utils.CardDeckRegistry
import com.bottazzini.tiramisu.utils.DeckGridAdapter
import com.bottazzini.tiramisu.utils.DeckRegion
import com.bottazzini.tiramisu.utils.WindowInsetsUtils
import com.google.android.material.tabs.TabLayout

class DeckPickerActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedNameLabel: TextView
    private lateinit var confirmButton: Button

    private var selectedDeck: CardDeck? = null
    private val adapterByRegion = mutableMapOf<DeckRegion, DeckGridAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_deck_picker)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.deckPickerScrollView))

        settingsHandler = SettingsHandler(applicationContext)

        tabLayout = findViewById(R.id.deckRegionTabs)
        recyclerView = findViewById(R.id.deckGrid)
        selectedNameLabel = findViewById(R.id.deckSelectedName)
        confirmButton = findViewById(R.id.buttonDeckConfirm)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        setupAdapters()
        setupTabs()

        val defaultDeck = CardDeckRegistry.byId("piacentine")
        selectDeck(defaultDeck)
        tabLayout.getTabAt(0)?.select()
        showRegion(DeckRegion.NORD)

        confirmButton.setOnClickListener { confirmSelection() }
    }

    private fun setupAdapters() {
        DeckRegion.values().forEach { region ->
            adapterByRegion[region] = DeckGridAdapter(CardDeckRegistry.byRegion(region)) { deck ->
                selectDeck(deck)
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_nord))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_sud_isole))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_internazionali))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showRegion(DeckRegion.values()[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showRegion(region: DeckRegion) {
        val adapter = adapterByRegion[region] ?: return
        selectedDeck?.let { adapter.setSelectedId(it.id) }
        recyclerView.adapter = adapter
    }

    private fun selectDeck(deck: CardDeck) {
        selectedDeck = deck
        selectedNameLabel.text = getString(deck.labelRes)
        confirmButton.text = getString(R.string.deck_picker_play_with, getString(deck.labelRes))
        confirmButton.isEnabled = true
        confirmButton.alpha = 1f
        adapterByRegion.values.forEach { it.setSelectedId(deck.id) }
    }

    private fun confirmSelection() {
        val deck = selectedDeck ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, deck.id)
        getSharedPreferences("tiramisu_prefs", MODE_PRIVATE)
            .edit().putBoolean("deck_chosen", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
