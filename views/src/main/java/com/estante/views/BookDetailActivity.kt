package com.estante.views

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.estante.views.data.MockBooks
import com.estante.views.databinding.ActivityBookDetailBinding
import com.estante.views.model.ReadingState
import com.estante.views.ui.BookDetailFragment

/**
 * Tela 2: detalhes do livro. Recebe o id e o estado de leitura pela Intent
 * e hospeda o [BookDetailFragment], que desenha a interface.
 */
class BookDetailActivity : AppCompatActivity(), BookDetailFragment.Listener {

    private lateinit var binding: ActivityBookDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        val bookId = intent.getIntExtra(EXTRA_BOOK_ID, -1)
        val book = MockBooks.findById(bookId)
        if (book == null) {
            // Id inválido: não há o que mostrar, então voltamos.
            finish()
            return
        }

        binding.toolbar.title = book.title
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Só cria o fragment na primeira vez; após rotação o FragmentManager o recria sozinho.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    R.id.fragment_container,
                    BookDetailFragment.newInstance(book.id, readingStateFrom(intent)),
                )
            }
        }
    }

    /** Chamado pelo fragment sempre que o usuário muda o estado de leitura. */
    override fun onReadingChanged(bookId: Int, state: ReadingState) {
        setResult(RESULT_OK, Intent().putReadingExtras(bookId, state))
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        private const val EXTRA_IS_READ = "extra_is_read"
        private const val EXTRA_RATING = "extra_rating"

        /** Monta a Intent explícita para abrir esta tela. */
        fun newIntent(context: Context, bookId: Int, state: ReadingState): Intent =
            Intent(context, BookDetailActivity::class.java).putReadingExtras(bookId, state)

        fun readingStateFrom(intent: Intent): ReadingState = ReadingState(
            isRead = intent.getBooleanExtra(EXTRA_IS_READ, false),
            // Extra opcional: só existe quando o livro já foi avaliado.
            rating = if (intent.hasExtra(EXTRA_RATING)) intent.getIntExtra(EXTRA_RATING, 0) else null,
        )

        private fun Intent.putReadingExtras(bookId: Int, state: ReadingState): Intent = apply {
            putExtra(EXTRA_BOOK_ID, bookId)
            putExtra(EXTRA_IS_READ, state.isRead)
            state.rating?.let { putExtra(EXTRA_RATING, it) }
        }
    }
}
