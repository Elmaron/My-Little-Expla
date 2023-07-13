package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var localDatabase: LocalDatabase

    private lateinit var availableContentTypes: List<ContentType>

    private val articleLengthShort: Short = 1
    private val articleLengthMiddle: Short = 2
    private val articleLengthLong: Short = 3

    private val initializeDatabaseScope = MainScope()

    private fun databaseInitializer(): Deferred<Unit> = initializeDatabaseScope.async {
        availableContentTypes = localDatabase.contentTypeDao().getContentTypes().first()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //LocalDatabase.createDatabase(this)
        setContentView(R.layout.activity_initialize_database)
        findViewById<ProgressBar>(R.id.progressBarInitializeDatabase).setProgress(0)
        localDatabase = LocalDatabase.getInstance(this)
        initializeDatabaseScope.launch {
            databaseInitializer().await()
        }
        findViewById<ProgressBar>(R.id.progressBarInitializeDatabase).setProgress(100)

        if(checkDisplaySize())
        {
            loadTablet()
        } else
        {
            loadPhone()
        }

        val myFiles = FileLoader(this, "articles", "aifa")

        for (contents in myFiles.getFileContentsFromSubfolder())
        {
            for (test in ArticleInterpreter("MyTitle", contents).getTestFile())
            Log.d("ArticleInterpreter", test)
        }

        // Log the inserted data from the database
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(checkDisplaySize())
        {
            super.onBackPressed()
        } else
        {
            loadPhone()
        }
    }

    private fun checkDisplaySize(): Boolean
    {
        return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE) ||
                (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE)
    }

    private fun loadPhone()
    {
        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.bottom_menu).setOnNavigationItemSelectedListener(this)

    }

    private fun loadTablet()
    {
        setContentView(R.layout.activity_main)

        if(findViewById<BottomNavigationView>(R.id.bottom_menu) != null)
        {
            findViewById<BottomNavigationView>(R.id.bottom_menu).setOnNavigationItemSelectedListener(this)
        } else if (findViewById<NavigationView>(R.id.side_menu) != null)
        {
            findViewById<NavigationView>(R.id.side_menu).setNavigationItemSelectedListener(this)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNavigationItemSelected(item: MenuItem): Boolean
    {
        findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).removeAllViews()
        if(findViewById<EditText>(R.id.editTextSearchField) != null)
        {
            val searchField = findViewById<EditText>(R.id.editTextSearchField)
            searchField.setText("")
        }
        when (item.itemId) {
            R.id.menu_favourites -> {
                findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_favourites, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
                loadArticles(localDatabase.articleDao().getFavouritesID(), this)
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                        if (text != null) {
                            GlobalScope.launch(Dispatchers.Main) {
                                if (text.equals(""))
                                    loadArticles(localDatabase.articleDao().getFavouritesID(), this@run)
                                else
                                    loadArticles(localDatabase.articleDao().getFavouritesID(), text.toString(), this@run)
                            }
                        }
                    }
                }
                return true
            }
            R.id.menu_recommended -> {
                findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_recommended, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
                loadArticles(localDatabase.articleDao().getRecommendedID(), this)
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                        if (text != null) {
                            GlobalScope.launch(Dispatchers.Main) {
                                if (text.equals(""))
                                    loadArticles(localDatabase.articleDao().getRecommendedID(), this@run)
                                else
                                    loadArticles(
                                        localDatabase.articleDao().getRecommendedID(), text.toString(), this@run)
                            }
                        }
                    }
                }
                return true
            }
            R.id.menu_add_articles -> {
                /*findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_add_article, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )) // */
                return true
            }
            R.id.menu_categorys -> {
                findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_categories, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
                return true
            }
            R.id.menu_search -> {
                findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_search, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
                loadArticles(localDatabase.articleDao().getArticles(), this)
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                        if (text != null) {
                            GlobalScope.launch(Dispatchers.Main) {
                                if (text.equals(""))
                                    loadArticles(localDatabase.articleDao().getArticles(), this@run)
                                else
                                    loadArticles(
                                        localDatabase.articleDao().getArticles(), text.toString(), this@run)
                            }
                        }
                    }
                }
                return true
            }
            else -> return false
        }
    }

    private fun loadArticles(articles: Flow<List<Article>>?, context: Context) {
        MainScope().launch {

            if (articles != null) {
                if (articles.first().isNotEmpty()) {
                    val articleHolder = findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder)
                    articleHolder.removeAllViews()

                    Log.d("loadArticles", "Articles successfully found")
                    for (article in articles.first()) {
                        Log.d(
                            "loadArticles",
                            "Loading Article: ${article.title}\nContent:\n${article}"
                        )

                        val layout = LinearLayout(ContextThemeWrapper(context, R.style.Article))
                        val title =
                            TextView(ContextThemeWrapper(context, R.style.Article_Text_Title))
                        val description =
                            TextView(ContextThemeWrapper(context, R.style.Article_Text_Description))

                        layout.orientation = LinearLayout.VERTICAL

                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        title.layoutParams = layoutParams
                        description.layoutParams = layoutParams

                        layoutParams.setMargins(0, 0, 0, 32)
                        layout.layoutParams = layoutParams

                        layout.addView(title)
                        layout.addView(description)

                        title.text = article.title
                        description.text = article.description

                        layout.setOnClickListener {
                            if (checkDisplaySize()) {
                                findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
                                findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                                    LayoutInflater.from(context).inflate(
                                        when (article.length) {
                                            articleLengthShort -> R.layout.activity_article_short
                                            articleLengthMiddle -> R.layout.activity_article_middle
                                            articleLengthLong -> R.layout.activity_article_long
                                            else -> {
                                                return@setOnClickListener
                                            }
                                        }, null
                                    ),
                                    ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                                        ConstraintLayout.LayoutParams.MATCH_PARENT
                                    )
                                )
                            } else {
                                when (article.length) {
                                    articleLengthShort -> {
                                        setContentView(R.layout.activity_article_short)
                                    }

                                    articleLengthMiddle -> {
                                        setContentView(R.layout.activity_article_middle)
                                    }

                                    articleLengthLong -> {
                                        setContentView(R.layout.activity_article_long)
                                    }

                                    else -> {
                                        return@setOnClickListener
                                    }
                                }
                            }

                            findViewById<TextView>(R.id.textViewArticleTitle).text =
                                article.title
                            findViewById<TextView>(R.id.textViewArticleHint).text =
                                article.hint ?: ""
                            findViewById<Button>(R.id.buttonBack).setOnClickListener { this@MainActivity.onBackPressed() }

                            when (article.length) {
                                articleLengthShort -> {
                                    findViewById<TextView>(R.id.textViewArticleDescription).text =
                                        article.description
                                    return@setOnClickListener
                                }

                                articleLengthMiddle -> {
                                    findViewById<LinearLayout>(R.id.LinearLayoutArticle).removeAllViews()
                                    MainScope().launch {
                                        run {
                                            val chapters = localDatabase.chapterDao()
                                                .getChaptersFromArticle(article.articleID)
                                                .first()
                                            for (chapter in chapters) {
                                                findViewById<LinearLayout>(R.id.LinearLayoutArticle).addView(
                                                    getChapterViews(chapter, context)
                                                )
                                            }
                                        }
                                    }
                                    return@setOnClickListener
                                }

                                articleLengthLong -> {
                                    return@setOnClickListener
                                }
                            }
                        }

                        articleHolder.addView(layout)
                    }
                } else Log.e("loadArticles", "Articles can't be found found")
            } else Log.e("loadArticles", "Articles can't be found")
        }

    }

    private fun loadArticles(articles: Flow<List<Article>>?, searchText: String, context: Context) {
        loadArticles(articles, context)
    }

    private fun getChapterViews(chapter: Chapter, context: Context): LinearLayout {
        var textView: TextView
        val finalViews = LinearLayout(context)
        val card = LinearLayout(context)
        val table = TableLayout(context)
        val tableRow = TableRow(context)
        var cardActive = false
        finalViews.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        MainScope().launch {
            run {
                val chapterContents = localDatabase.chapterContentDao().getChapterContent(chapter.chapterID).first()
                for (chapterContent in chapterContents)
                {
                    Log.d("Function getChapterView", "ChapterContent: $chapterContent")
                    for (contentType in availableContentTypes)
                    if(contentType.contentTypeID == chapterContent.idContentType)
                    {
                        when(contentType.type) {
                            "text" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                                Log.d("Function getChapterView", "CALLED 1")
                            }

                            "header1" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                                Log.d("Function getChapterView", "CALLED 2")
                            }

                            "header2" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                                Log.d("Function getChapterView", "CALLED 3")
                            }

                            "header3" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                                Log.d("Function getChapterView", "CALLED 4")
                            }

                            "description" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                            }

                            "one-line-info" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                tableRow.addView(textView)
                            }

                            "one-line-info-element" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                tableRow.addView(textView)
                                table.addView(tableRow)
                                tableRow.removeAllViews()
                            }

                            "one-line-info-end" -> {
                                if (cardActive) card.addView(table)
                                else finalViews.addView(table)
                                table.removeAllViews()
                            }

                            "unsorted-list-element" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                                Log.d("Function getChapterView", "CALLED 5")
                            }

                            "sorted-list-element" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                            }

                            "image" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                            }

                            "hint" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                            }

                            "chapter-hint" -> {
                                textView = TextView(context)
                                textView.text = chapterContent.content
                                if (cardActive) card.addView(textView)
                                else finalViews.addView(textView)
                            }

                            "card-start" -> {
                                cardActive = true
                            }

                            "card-end" -> {
                                if (table.childCount != 0) {
                                    card.addView(table)
                                    table.removeAllViews()
                                }
                                finalViews.addView(card)
                                card.removeAllViews()
                                cardActive = false
                            }
                        }

                    }
                }
                if(table.childCount != 0)
                {
                    finalViews.addView(table)
                }
            }
        }
        return finalViews
    }


}




//OLD CODE//
/*

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

 */