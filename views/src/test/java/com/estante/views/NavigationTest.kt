package com.estante.views

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estante.views.model.ReadingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Testa a navegação por Intent explícita e a interação na tela de detalhes. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @Test
    fun tocarNoLivroAbreDetalhesComIntentExplicita() {
        ActivityScenario.launch(BookListActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.recycler_books)
                list.measure(0, 0)
                list.layout(0, 0, 1080, 1920)
                list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()

                val intent = shadowOf(activity).nextStartedActivity
                assertEquals(BookDetailActivity::class.java.name, intent.component?.className)
                assertEquals(1, intent.getIntExtra(BookDetailActivity.EXTRA_BOOK_ID, -1))
            }
        }
    }

    @Test
    fun marcarComoLidoAtualizaATelaEDevolveResultado() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = BookDetailActivity.newIntent(context, bookId = 4, state = ReadingState())

        ActivityScenario.launchActivityForResult<BookDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val status = activity.findViewById<TextView>(R.id.text_status)
                val synopsis = activity.findViewById<TextView>(R.id.text_synopsis)
                // Livro 4 não tem sinopse: o valor opcional mostra o texto alternativo.
                assertEquals(activity.getString(R.string.synopsis_unavailable), synopsis.text.toString())
                assertEquals(activity.getString(R.string.status_not_read), status.text.toString())
                assertFalse(activity.findViewById<View>(R.id.rating_view).isEnabled)

                activity.findViewById<View>(R.id.button_toggle_read).performClick()

                assertEquals(activity.getString(R.string.status_read), status.text.toString())
                assertTrue(activity.findViewById<View>(R.id.rating_view).isEnabled)
                activity.findViewById<View>(R.id.star4).performClick()
                activity.finish()
            }
            val result = scenario.result.resultData
            assertTrue(BookDetailActivity.readingStateFrom(result).isRead)
            assertEquals(4, BookDetailActivity.readingStateFrom(result).rating)
        }
    }
}
