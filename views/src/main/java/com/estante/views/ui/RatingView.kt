package com.estante.views.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import com.estante.views.R
import com.estante.views.databinding.ViewRatingBinding

/**
 * Componente XML reutilizável: 5 estrelas de avaliação.
 * Infla `view_rating.xml` (com <merge>) e avisa por [onRatingChanged] quando o usuário toca.
 */
class RatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val binding = ViewRatingBinding.inflate(LayoutInflater.from(context), this)
    private val stars: List<ImageButton> = listOf(
        binding.star1, binding.star2, binding.star3, binding.star4, binding.star5,
    )

    /** Nota atual (1..5) ou `null` se ainda não avaliado. */
    var rating: Int? = null
        set(value) {
            field = value
            render()
        }

    var onRatingChanged: ((Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                val newRating = index + 1
                rating = newRating
                onRatingChanged?.invoke(newRating)
            }
        }
        render()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        stars.forEach { it.isEnabled = enabled }
        alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun render() {
        val current = rating ?: 0
        stars.forEachIndexed { index, star ->
            val position = index + 1
            star.setImageResource(
                if (position <= current) R.drawable.ic_star else R.drawable.ic_star_outline,
            )
            star.contentDescription = context.getString(R.string.cd_rate_stars, position)
        }
        contentDescription = rating?.let { context.getString(R.string.cd_rating_value, it) }
            ?: context.getString(R.string.cd_rating_empty)
    }

    private companion object {
        const val DISABLED_ALPHA = 0.38f
    }
}
