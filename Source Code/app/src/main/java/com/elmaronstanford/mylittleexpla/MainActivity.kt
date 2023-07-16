package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
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

    //private lateinit var localDatabase: LocalDatabase

    private lateinit var availableContentTypes: List<ContentType>

    private val articleLengthShort: Short = 1
    private val articleLengthMiddle: Short = 2
    private val articleLengthLong: Short = 3

    private val initializeDatabaseScope = MainScope()

    //private fun databaseInitializer(): Deferred<Unit> = initializeDatabaseScope.async {
    //    availableContentTypes = localDatabase.contentTypeDao().getContentTypes().first()
    //}

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //LocalDatabase.createDatabase(this)
        setContentView(R.layout.activity_initialize_database)
        findViewById<ProgressBar>(R.id.progressBarInitializeDatabase).setProgress(0)
        //localDatabase = LocalDatabase.getInstance(this)
        //initializeDatabaseScope.launch {
        //    databaseInitializer().await()
        //}
        findViewById<ProgressBar>(R.id.progressBarInitializeDatabase).setProgress(100)

        if(checkDisplaySize())
        {
            loadTablet()
        } else
        {
            loadPhone()
        }

        val myFiles = FileLoader(this, "articles", "aifa")

        /*
        for (contents in myFiles.getFileContentsFromSubfolder())
        {
            for (test in ArticleInterpreter(this, contents).getTestFile())
            Log.d("ArticleInterpreter", test)
        }*/

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
        findViewById<BottomNavigationView>(R.id.bottom_menu).selectedItemId = R.id.menu_recommended
    }

    private fun loadTablet()
    {
        setContentView(R.layout.activity_main)

        if(findViewById<BottomNavigationView>(R.id.bottom_menu) != null)
        {
            findViewById<BottomNavigationView>(R.id.bottom_menu).setOnNavigationItemSelectedListener(this)
            findViewById<BottomNavigationView>(R.id.bottom_menu).selectedItemId = R.id.menu_recommended
        } else if (findViewById<NavigationView>(R.id.side_menu) != null)
        {
            findViewById<NavigationView>(R.id.side_menu).setNavigationItemSelectedListener(this)
            findViewById<NavigationView>(R.id.side_menu).setCheckedItem(R.id.menu_recommended)
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
                loadArticles("favourite")
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                        loadArticles(text.toString(), "favourite")
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
                loadArticles("recommended")//loadArticles(localDatabase.articleDao().getRecommendedID(), this)
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                        loadArticles(text.toString(), "recommended")
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
                loadArticles("")
                findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                    run {
                       loadArticles(text.toString(), "")
                    }
                }
                return true
            }
            else -> return false
        }
    }

    private fun loadArticles(searchContent: String, page: String)
    {
        findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).removeAllViews()
        val myFiles = FileLoader(this, "articles", "aifa")
        val myArticles = mutableListOf<ArticleInterpreter>()
        for (file in myFiles.getFileContentsFromSubfolder())
        {
            myArticles += ArticleInterpreter(this, file)
        }

        for (myArticle in myArticles)
        {
            if (myArticle.getFileContent().contains(searchContent)) //(myArticle.getArticleInfo()[0].contains(searchContent))
            {
                if(page.equals("favourite") && myArticle.isFavourite()) {
                    val card = LinearLayout(this)
                    val title = TextView(this)
                    val description = TextView(this)

                    title.setText(myArticle.getArticleInfo()[0])
                    description.setText(myArticle.getArticleInfo()[2])

                    card.orientation = LinearLayout.VERTICAL
                    card.setOnClickListener { openArticle(myArticle) }

                    card.addView(title)
                    card.addView(description)

                    findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).addView(card)
                } else if (page.equals("recommended") && myArticle.isRecommended())
                {
                    val card = LinearLayout(this)
                    val title = TextView(this)
                    val description = TextView(this)

                    title.setText(myArticle.getArticleInfo()[0])
                    description.setText(myArticle.getArticleInfo()[2])

                    card.orientation = LinearLayout.VERTICAL
                    card.setOnClickListener { openArticle(myArticle) }

                    card.addView(title)
                    card.addView(description)

                    findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).addView(card)
                } else if (page.equals(""))
                {
                    val card = LinearLayout(this)
                    val title = TextView(this)
                    val description = TextView(this)

                    title.setText(myArticle.getArticleInfo()[0])
                    description.setText(myArticle.getArticleInfo()[2])

                    card.orientation = LinearLayout.VERTICAL
                    card.setOnClickListener { openArticle(myArticle) }

                    card.addView(title)
                    card.addView(description)

                    findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).addView(card)
                }
            }
        }
    }

    private fun loadArticles(page: String) {
        loadArticles("", page)
    }

    private fun openArticle(article: ArticleInterpreter)
    {
        Log.d("MainActivity", "Opening Article")
        if(article.getArticleInfo()[1] == "short")
        {
            if(checkDisplaySize())
            {
                findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
                findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_article_short, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                )
            } else
            {
                setContentView(R.layout.activity_article_short)
            }
        } else if (article.getArticleInfo()[1] == "middle")
        {
            if(checkDisplaySize())
            {
                findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
                findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_article_middle, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                )
            } else
            {
                setContentView(R.layout.activity_article_middle)
            }

        } else if (article.getArticleInfo()[1] == "long")
        {
            if(checkDisplaySize())
            {
                findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
                findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                    LayoutInflater.from(this).inflate(R.layout.activity_article_long, null),
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                )
            } else
            {
                setContentView(R.layout.activity_article_long)
            }
        } else return

        findViewById<TextView>(R.id.textViewArticleTitle).text = article.getArticleInfo()[0]
        findViewById<TextView>(R.id.textViewArticleHint).text = article.getArticleInfo()[article.getArticleInfo().size-1]
        findViewById<Button>(R.id.buttonBack).setOnClickListener { this.onBackPressed() }

        if(article.getArticleInfo()[1] == "short")
        {

        } else if (article.getArticleInfo()[1] == "middle")
        {
            for (x in article.getContent())
            {
                findViewById<LinearLayout>(R.id.LinearLayoutArticle).addView(x)
            }
        } else if (article.getArticleInfo()[1] == "long")
        {

        }
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