package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
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

    private lateinit var myFiles: FileLoader
    private lateinit var myArticles: List<ArticleInterpreter>


    //LOADS THE FILES INTO MEMORY AND CHANGES ALL SETTINGS ACCORDING TO DEVICE, AFTER OPENING THE APP//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadDatabase()
        if(checkDisplaySize()) loadTablet()
        else loadPhone()
    }

    //THIS FUNCTION OVERRIDES, WHAT THE BACK-BUTTON FROM THE SYSTEM DOES WITHIN THE APP//
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(checkDisplaySize()) super.onBackPressed()
        else loadPhone()
    }

    //THIS FUNCTION RETURNS TRUE, IF THE SCREEN SIZE IS LARGE OR XLARGE, WHICH IS TRUE FOR DEVICES LIKE TABLETS OR TVS//
    private fun checkDisplaySize(): Boolean
    {
        return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE) ||
                (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE)
    }

    //THIS FUNCTION ADDS FUNCTIONS ACCORDING TO THE LAYOUT FOR PHONES//
    private fun loadPhone()
    {
        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.bottom_menu).setOnNavigationItemSelectedListener(this)
        findViewById<BottomNavigationView>(R.id.bottom_menu).selectedItemId = R.id.menu_recommended
    }

    //THIS FUNCTION ADDS FUNCTIONS ACCORDING TO THE LAYOUT FOR TABLETS OR DEVICES WITH LARGER SCREENS//
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

    //THIS FUNCTION DECIDES, WHAT HAPPENS, IF YOU PRESS ONE OF THE MENÜ ITEMS-BUTTONS//
    override fun onNavigationItemSelected(item: MenuItem): Boolean
    {
        findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).removeAllViews()

        var layoutID = when (item.itemId) {
            R.id.menu_favourites    -> R.layout.activity_favourites
            R.id.menu_recommended   -> R.layout.activity_recommended
            R.id.menu_add_articles  -> return true
            R.id.menu_categorys     -> R.layout.activity_categories
            R.id.menu_search        -> R.layout.activity_search
            else                    -> return false
        }

        findViewById<ConstraintLayout>(R.id.constraintLayoutMenuHolder).addView(
            LayoutInflater.from(this).inflate(layoutID, null),
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ))

        loadArticles(layoutID)

        if(findViewById<EditText>(R.id.editTextSearchField) != null)
        {
            findViewById<EditText>(R.id.editTextSearchField).setText("")
            findViewById<EditText>(R.id.editTextSearchField).doOnTextChanged{ text, start, before, count ->
                run {
                    loadArticles(text.toString(), layoutID)
                }
            }
        }

        return true
    }

    //THIS FUNCTION LOADS THE ARTICLES AS CARDS AND SHOWS THEM ON THE SPECIFIC SIDES, IF THEY ARE PART OF THAT SPECIFIC CONTENT TYPE//
    private fun loadArticles(searchContent: String, page: Int)
    {
        findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).removeAllViews()
        for (myArticle in myArticles)
        {
            if (myArticle.getFileContent().contains(searchContent))
            {
                if((page == R.layout.activity_favourites && myArticle.isFavourite())
                    || (page == R.layout.activity_recommended && myArticle.isRecommended())
                    || page != R.layout.activity_favourites && page != R.layout.activity_recommended) {
                    val card = LinearLayout(ContextThemeWrapper(this, R.style.ArticleCard))
                    val title = TextView(ContextThemeWrapper(this, R.style.ArticleCard_Title))
                    val description = TextView(ContextThemeWrapper(this, R.style.ArticleCard_Description))

                    title.setText(myArticle.getArticleInfo()[0])
                    if(myArticle.getArticleInfo()[1] == "short")
                    {
                        description.text = myArticle.getArticleInfo()[2].substring(0, 20)
                    }

                    else
                        description.text = myArticle.getArticleInfo()[2]

                    title.setTextColor(Color.BLACK)
                    description.setTextColor(Color.BLACK)

                    card.orientation = LinearLayout.VERTICAL
                    card.setOnClickListener { openArticle(myArticle) }

                    if(myArticle.getArticleInfo()[1] == "short")
                        card.setBackgroundResource(R.drawable.article_card_ahort_background)
                    else if(myArticle.getArticleInfo()[1] == "middle")
                        card.setBackgroundResource(R.drawable.article_card_middle_background)
                    else if (myArticle.getArticleInfo()[1] == "long")
                        card.setBackgroundResource(R.drawable.article_card_long_background)
                    else
                        card.setBackgroundResource(R.drawable.article_background)

                    card.addView(title)
                    card.addView(description)

                    findViewById<LinearLayout>(R.id.LinearLayoutArticleHolder).addView(card)
                }
            }
        }
    } //HINT: This function should be replaced be a function which loads another layout and changes settings according to the article type etc.

    //THIS FUNCTION DOES RUN THE SAME FUNCTION AS ABOVE, BUT WITHOUT ANY SEARCH INPUT//
    private fun loadArticles(page: Int) {
        loadArticles("", page)
    }

    //THIS FUNCTION LOADS ALL CONTENT OF AN ARTICLE PREDEFINED IN THE ARTICLEINTERPRETER CLASS//
    private fun openArticle(article: ArticleInterpreter)
    {
        Log.d("MainActivity", "Opening Article")
        // -- Determines how to load the article -- //
        var layoutID = when(article.getArticleInfo()[1])
        {
            "short"  -> R.layout.activity_article_short
            "middle" -> R.layout.activity_article_middle
            "long"   -> R.layout.activity_article_long
            else     -> return
        }

        // -- Determines how to load for which devices -- //
        if(checkDisplaySize())
        {
            findViewById<ConstraintLayout>(R.id.OpenedArticles).removeAllViews()
            findViewById<ConstraintLayout>(R.id.OpenedArticles).addView(
                LayoutInflater.from(this).inflate(layoutID, null),
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            )

        } else setContentView(layoutID)


        findViewById<TextView>(R.id.textViewArticleTitle).text = article.getArticleInfo()[0]

        val articleHint = findViewById<TextView>(R.id.textViewArticleHint)
        articleHint.text = article.getArticleInfo()[article.getArticleInfo().size-1]
        if(findViewById<TextView>(R.id.textViewArticleHint).text == "" || findViewById<TextView>(R.id.textViewArticleHint).text == null)
            articleHint.visibility = View.GONE
        else
            articleHint.visibility = View.VISIBLE

        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            if (article.getArticleInfo()[1] == "middle")
                findViewById<LinearLayout>(R.id.LinearLayoutArticle).removeAllViews()
            else if (article.getArticleInfo()[1] == "long")
                findViewById<LinearLayout>(R.id.LinearLayoutArticle).removeAllViews()
            this.onBackPressed()
        }

        if(article.getArticleInfo()[1] == "short")
        {
            findViewById<TextView>(R.id.textViewArticleDescription).text = article.getArticleInfo()[2]
        } else if (article.getArticleInfo()[1] == "middle")
        {
            val articleContent = article.getContent()
            for (x in 0 until articleContent.size)
            {
                if(x != 0) articleContent[x].setPadding(0,32,0,0)
                findViewById<LinearLayout>(R.id.LinearLayoutArticle).addView(articleContent[x])
                if(x != articleContent.size-1) {
                    val chapterEndLine = View(this)
                    chapterEndLine.setBackgroundColor(Color.BLACK)//ContextCompat.getColor(this, R.color.TextColorArticle))
                    chapterEndLine.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        4
                    )
                    chapterEndLine.setPadding(0,8,0,0)
                    findViewById<LinearLayout>(R.id.LinearLayoutArticle).addView(chapterEndLine)
                }
            }
        } else if (article.getArticleInfo()[1] == "long")
        {
            val articleContent = article.getContent()
            val myMenuLayout = findViewById<TabLayout>(R.id.tabLayoutArticleTabs)
            val myView = findViewById<LinearLayout>(R.id.LinearLayoutArticle)
            for (x in 0 until articleContent.size)
            {
                var title = ""
                for (myTitle in articleContent[x].children)
                {
                    if(myTitle is TextView) {
                        title = myTitle.text.toString()
                        break
                    }
                }
                myMenuLayout.addTab(myMenuLayout.newTab().setText(title))
            }

            myMenuLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    // Perform action when the tab is selected
                    for(x in 0 until articleContent.size)
                    {
                        if(tab.position == x)
                        {
                            myView.removeAllViews()
                            myView.addView(articleContent[x])
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // Perform action when the tab is unselected (optional)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // Perform action when the tab is reselected (optional)
                }
            })
        }
    }


    //READS ALL ARTICLES AND LOADS THEM IN THE MEMORY, WHILE SHOWING A LOADING SCREEN//
    private fun loadDatabase()
    {
        myFiles = FileLoader(this, "articles", "aifa")
        myArticles = mutableListOf()

        setContentView(R.layout.activity_initialize_database)

        val files = myFiles.getFileContentsFromSubfolder()
        val myProgress = findViewById<ProgressBar>(R.id.progressBarInitializeDatabase)

        myProgress.max = files.size
        var value = 1

        for (file in files)
        {
            myArticles += ArticleInterpreter(this, file) //problem!!!!
            myProgress.progress = value
            value++
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