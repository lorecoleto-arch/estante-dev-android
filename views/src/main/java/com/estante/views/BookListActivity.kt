package com.estante.views

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.estante.views.data.MockBooks
import com.estante.views.databinding.ActivityBookListBinding
import com.estante.views.model.BookItem
import com.estante.views.model.ReadingState
import com.estante.views.ui.BookAdapter

/**
 * Tela 1: lista de livros (RecyclerView).
 * Ao tocar em um livro, abre a Tela 2 com uma Intent explícita e recebe
 * de volta o estado de leitura atualizado (Activity Result API).
 */
class BookListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookListBinding
    private val adapter = BookAdapter(onBookClick = ::openDetail)

    /** Estado da tela: lista imutável; cada alteração gera uma nova lista. */
    private var items: List<BookItem> = MockBooks.all.map { BookItem(it) }

    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val bookId = data.getIntExtra(BookDetailActivity.EXTRA_BOOK_ID, NO_ID)
            val newState = BookDetailActivity.readingStateFrom(data)
            updateReading(bookId, newState)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityBookListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        savedInstanceState?.let { restoreState(it) }

        binding.recyclerBooks.adapter = adapter
        render()
    }

    private fun openDetail(item: BookItem) {
        // Intent explícita: dizemos exatamente qual Activity abrir e passamos os dados como extras.
        val intent = BookDetailActivity.newIntent(this, item.book.id, item.reading)
        detailLauncher.launch(intent)
    }

    private fun updateReading(bookId: Int, newState: ReadingState) {
        items = items.map { item ->
            if (item.book.id == bookId) item.copy(reading = newState) else item
        }
        render()
    }

    private fun render() {
        adapter.submitList(items)
        val readCount = items.count { it.reading.isRead }
        binding.textSummary.text = resources.getQuantityString(
            R.plurals.summary_read, readCount, readCount, items.size,
        )
    }

    // --- Preserva as marcações após rotação da tela ---

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val read = items.filter { it.reading.isRead }
        outState.putIntArray(KEY_READ_IDS, read.map { it.book.id }.toIntArray())
        outState.putIntArray(KEY_RATINGS, read.map { it.reading.rating ?: 0 }.toIntArray())
    }

    private fun restoreState(state: Bundle) {
        val ids = state.getIntArray(KEY_READ_IDS) ?: return
        val ratings = state.getIntArray(KEY_RATINGS) ?: IntArray(ids.size)
        val byId = ids.indices.associate { i -> ids[i] to ratings.getOrElse(i) { 0 } }
        items = items.map { item ->
            val rating = byId[item.book.id] ?: return@map item
            item.copy(reading = ReadingState(isRead = true, rating = rating.takeIf { it > 0 }))
        }
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            binding.recyclerBooks.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private companion object {
        const val NO_ID = -1
        const val KEY_READ_IDS = "read_ids"
        const val KEY_RATINGS = "ratings"
    }
}
