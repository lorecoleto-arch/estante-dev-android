package com.estante.views.model

import androidx.annotation.ColorRes

/**
 * Livro do catálogo simulado. Imutável: qualquer "mudança" gera uma cópia com [copy].
 * Campos que nem todo livro possui são anuláveis e tratados na tela.
 */
data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val year: Int?,
    val pageCount: Int?,
    val synopsis: String?,
    @ColorRes val coverColor: Int,
)

/** Situação de leitura que o usuário registra para um livro. */
data class ReadingState(
    val isRead: Boolean = false,
    /** Nota de 1 a 5; `null` enquanto o livro não foi avaliado. */
    val rating: Int? = null,
)

/** O que a lista exibe: o livro + o estado de leitura dele. */
data class BookItem(
    val book: Book,
    val reading: ReadingState = ReadingState(),
)

/** Iniciais usadas como "capa" (ex.: "Código Limpo" -> "CL"; ignora palavras curtas como "O", "de"). */
val Book.initials: String
    get() = title.split(" ")
        .filter { it.length > 2 && it.first().isUpperCase() }
        .take(2)
        .joinToString("") { it.first().toString() }
        .ifEmpty { title.take(1) }
