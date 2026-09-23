package com.estante.views.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.estante.views.R
import com.estante.views.data.MockBooks
import com.estante.views.databinding.FragmentBookDetailBinding
import com.estante.views.model.Book
import com.estante.views.model.ReadingState
import com.estante.views.model.initials

/**
 * Interface de detalhes em um Fragment, usando ViewBinding e respeitando o ciclo de vida:
 * o binding é criado em onCreateView e liberado em onDestroyView (a View morre antes do Fragment).
 */
class BookDetailFragment : Fragment() {

    /** Quem hospeda o fragment é avisado quando o estado de leitura muda. */
    interface Listener {
        fun onReadingChanged(bookId: Int, state: ReadingState)
    }

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = checkNotNull(_binding)

    private lateinit var book: Book
    private var reading = ReadingState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        book = checkNotNull(MockBooks.findById(args.getInt(ARG_BOOK_ID)))
        // Após rotação, o estado salvo tem prioridade sobre o que veio nos argumentos.
        val source = savedInstanceState ?: args
        reading = ReadingState(
            isRead = source.getBoolean(ARG_IS_READ),
            rating = source.getInt(ARG_RATING).takeIf { it > 0 },
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindBook()

        binding.buttonToggleRead.setOnClickListener {
            // Desmarcar como lido também apaga a nota: só se avalia o que foi lido.
            val newState = if (reading.isRead) ReadingState() else reading.copy(isRead = true)
            updateReading(newState)
        }
        binding.ratingView.onRatingChanged = { stars ->
            updateReading(reading.copy(rating = stars))
        }
        // Também avisa já na criação: após rotação a Activity é nova e precisa do resultado de novo.
        updateReading(reading)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_IS_READ, reading.isRead)
        outState.putInt(ARG_RATING, reading.rating ?: 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // evita vazamento de memória da View antiga
    }

    private fun bindBook() {
        val context = requireContext()
        binding.textInitials.text = book.initials
        binding.cover.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, book.coverColor))
        binding.textTitle.text = book.title
        binding.textAuthor.text = book.author

        // Valores opcionais: cada um tem um texto alternativo quando está ausente.
        val year = book.year?.toString() ?: getString(R.string.year_unknown)
        val pages = book.pageCount?.let {
            resources.getQuantityString(R.plurals.pages, it, it)
        } ?: getString(R.string.pages_unknown)
        binding.textMeta.text = getString(R.string.meta_format, year, pages)
        binding.textSynopsis.text = book.synopsis ?: getString(R.string.synopsis_unavailable)
    }

    private fun updateReading(newState: ReadingState) {
        reading = newState
        renderReading()
        (activity as? Listener)?.onReadingChanged(book.id, reading)
    }

    private fun renderReading() {
        binding.textStatus.setText(
            if (reading.isRead) R.string.status_read else R.string.status_not_read,
        )
        binding.buttonToggleRead.setText(
            if (reading.isRead) R.string.action_mark_unread else R.string.action_mark_read,
        )
        binding.ratingView.rating = reading.rating
        binding.ratingView.isEnabled = reading.isRead
        binding.textRatingHint.setText(
            if (reading.isRead) R.string.rating_hint else R.string.rating_locked_hint,
        )
    }

    companion object {
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_IS_READ = "is_read"
        private const val ARG_RATING = "rating"

        fun newInstance(bookId: Int, state: ReadingState) = BookDetailFragment().apply {
            arguments = bundleOf(
                ARG_BOOK_ID to bookId,
                ARG_IS_READ to state.isRead,
                ARG_RATING to (state.rating ?: 0),
            )
        }
    }
}
