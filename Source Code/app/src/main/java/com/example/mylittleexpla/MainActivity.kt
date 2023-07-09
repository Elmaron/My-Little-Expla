package com.example.mylittleexpla

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.RecyclerListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private data class Article(val title: String, val shortDescription: String, val tags: List<String>, val articleID: Int)

    private val myArticles: List<Article> = listOf(
        Article("Dateien freigeben (Android)", "M\u00F6glichkeiten der Dateifreigabe in Android", listOf("Nearby Share", "Dateifreigabe", "Android"), R.layout.article_android_nearby_share)
    )

    private lateinit var articleHolder: LinearLayout

    private lateinit var searchField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadMain()
    }

    override fun onBackPressed() {
        if(android.R.id.content != R.layout.activity_main)
            loadMain()
        else
            super.onBackPressed()
    }

    private fun loadMain()
    {
        setContentView(R.layout.activity_main)

        articleHolder = findViewById(R.id.LinearLayoutArticleHolder)

        searchField = findViewById(R.id.editTextSearchField)

        for(article in myArticles)
        {
            val layout = LinearLayout(ContextThemeWrapper(this, R.style.Article))
            val title = TextView(ContextThemeWrapper(this, R.style.Article_Title))
            val description = TextView(ContextThemeWrapper(this, R.style.Article_Description))

            title.text = article.title
            description.text = article.shortDescription

            layout.orientation = LinearLayout.VERTICAL

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layout.layoutParams = layoutParams
            title.layoutParams = layoutParams
            description.layoutParams = layoutParams

            layout.addView(title)
            layout.addView(description)
            layout.setOnClickListener {
                setContentView(article.articleID)
            }

            articleHolder.addView(layout)
        }

        searchField.doOnTextChanged { text, start, before, count ->
            run {
                if (text != null) {
                    if (text.equals(""))
                        for (i in 0 until articleHolder.childCount) {
                            val layout = articleHolder.getChildAt(i)
                            layout.visibility = LinearLayout.VISIBLE
                        }
                    else {
                        for (i in 0 until articleHolder.childCount) {
                            val layout = articleHolder.getChildAt(i)
                            var isVisible = false
                            for (tag in myArticles[i].tags) {
                                if(tag.lowercase().contains(text.toString().lowercase()))
                                {
                                    isVisible = true
                                    break
                                }
                            }
                            layout.visibility = if (isVisible)  LinearLayout.VISIBLE else LinearLayout.GONE
                        }
                    }


                }
            }
        }
    }
}