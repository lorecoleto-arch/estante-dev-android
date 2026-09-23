package com.estante.app.domain

/** Erros possíveis no formulário de leitura. As mensagens ficam na UI (strings.xml). */
enum class FieldError {
    PAGES_REQUIRED,
    PAGES_NOT_A_NUMBER,
    PAGES_OUT_OF_RANGE,
    RATING_REQUIRED,
    NOTE_TOO_LONG,
}

data class ValidationResult(
    val pagesError: FieldError? = null,
    val ratingError: FieldError? = null,
    val noteError: FieldError? = null,
) {
    val isValid: Boolean
        get() = pagesError == null && ratingError == null && noteError == null
}

/**
 * Regras de negócio do formulário "Registrar leitura" — sem nenhuma dependência de Android,
 * por isso é fácil de testar com JUnit puro.
 */
object ShelfEntryValidator {

    const val NOTE_MAX_LENGTH = 500

    fun validate(
        status: ReadingStatus,
        pagesRead: String,
        rating: Int?,
        note: String,
        totalPages: Int?,
    ): ValidationResult = ValidationResult(
        pagesError = if (status == ReadingStatus.READING) validatePages(pagesRead, totalPages) else null,
        ratingError = if (status == ReadingStatus.FINISHED && rating !in 1..5) FieldError.RATING_REQUIRED else null,
        noteError = if (note.trim().length > NOTE_MAX_LENGTH) FieldError.NOTE_TOO_LONG else null,
    )

    private fun validatePages(input: String, totalPages: Int?): FieldError? {
        val text = input.trim()
        if (text.isEmpty()) return FieldError.PAGES_REQUIRED
        val pages = text.toIntOrNull() ?: return FieldError.PAGES_NOT_A_NUMBER
        val max = totalPages ?: Int.MAX_VALUE
        return if (pages < 0 || pages > max) FieldError.PAGES_OUT_OF_RANGE else null
    }
}
