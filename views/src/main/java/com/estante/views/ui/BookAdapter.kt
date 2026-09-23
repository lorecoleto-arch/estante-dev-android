package com.estante.views.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.estante.views.R
import com.estante.views.databinding.ItemBookBinding
import com.estante.views.model.BookItem
import com.estante.views.model.initials

/** Adapter da lista de livros. Usa DiffUtil para atualizar só os itens que mudaram. */
class BookAdapter(
    private val onBookClick: (BookItem) -> Unit,
) : ListAdapter<BookItem, BookAdapter.BookViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(
        private val binding: ItemBookBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookItem) {
            val book = item.book
            val context = binding.root.context

            binding.textTitle.text = book.title
            binding.textAuthor.text = book.author
            // Valor opcional: se não houver ano, mostramos um texto padrão.
            binding.textYear.text = book.year?.toString()
                ?: context.getString(R.string.year_unknown)

            binding.textInitials.text = book.initials
            binding.cover.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, book.coverColor))

            binding.iconRead.isVisible = item.reading.isRead
            binding.root.contentDescription = context.getString(
                if (item.reading.isRead) R.string.cd_book_item_read else R.string.cd_book_item,
                book.title,
                book.author,
            )
            binding.root.setOnClickListener { onBookClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<BookItem>() {
            override fun areItemsTheSame(old: BookItem, new: BookItem) = old.book.id == new.book.id
            override fun areContentsTheSame(old: BookItem, new: BookItem) = old == new
        }
    }
}
