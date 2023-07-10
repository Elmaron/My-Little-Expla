package com.elmaronstanford.mylittleexpla

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged

class MainActivity : AppCompatActivity() {

    private data class Article(val title: String, val shortDescription: String, val tags: List<String>, val articleID: Int)

    private val myArticles: List<Article> = listOf(
        Article("Dateien freigeben", "M\u00F6glichkeiten der Dateifreigabe auf allen möglichen Geräten", listOf("Nearby Share", "Dateifreigabe", "Android", "Dateien", "Direktverbindungen", "App", "Programm", "Cloud", "Bluetooth", "Quick Share", "Samsung", "Bilder", "versenden", "E-Mail", "Wolken", "Himmel", "Sender", "Empf\u00E4", "Windows"), R.layout.article_android_dateifreigabe),
        Article("Tastenkombinationen", "Verschiedene Tastenkombinationen für alle möglichen Plattformaen und Programme", listOf("Shortcuts", "Tastenkombinationen", "Word", "Grundlagen", "Grundfunktionen"), R.layout.article_general_shortcuts)
    )

    private lateinit var articleHolder: LinearLayout

    private lateinit var searchField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadMain()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(!checkDisplaySize()) {
            if (android.R.id.content != R.layout.activity_main)
                loadMain()
        }
        else
            super.onBackPressed()
    }

    private fun checkDisplaySize(): Boolean
    {
        return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE) ||
                (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE)
    }

    private fun loadMain()
    {
        setContentView(R.layout.activity_main)

        articleHolder = findViewById(R.id.LinearLayoutArticleHolder)

        searchField = findViewById(R.id.editTextSearchField)

        for(article in myArticles)
        {
            val layout = LinearLayout(ContextThemeWrapper(this, R.style.Article))
            val title = TextView(ContextThemeWrapper(this, R.style.Article_Text_Title))
            val description = TextView(ContextThemeWrapper(this, R.style.Article_Text_Description))

            title.text = article.title
            description.text = article.shortDescription

            layout.orientation = LinearLayout.VERTICAL

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            title.layoutParams = layoutParams
            description.layoutParams = layoutParams

            layoutParams.setMargins(0,0,0,32)
            layout.layoutParams = layoutParams

            layout.addView(title)
            layout.addView(description)
            if(checkDisplaySize()) layout.setOnClickListener {
                findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
                findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                    LayoutInflater.from(this).inflate(article.articleID, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
            }
            else layout.setOnClickListener {
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