package com.elmaronstanford.mylittleexpla

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Layout
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import org.w3c.dom.Text
import kotlin.properties.Delegates

class ArticleInterpreter(context: Context, pFile: String) {

    private var file = mutableListOf<String>()

    private var title: String = ""
    private var type: String = ""
    private var description: String = ""
    private lateinit var icon: Drawable
    private var article_hint: String = ""

    private var favourite: Boolean = false
    private var recommended: Boolean = false

    private var interpreterVersion by Delegates.notNull<Int>()

    private lateinit var articleContent: List<LinearLayout>

    //RUNS AFTER THE CLASS IS CALLED FOR THE FIRST TIME. DEFINES, HOW TO SPLIT THE FILE CONTENT AND SPLITS THE FILE CONTENT BEFORE LOADING IT INTO THE APP//
    init {
        val fileSplit = pFile.split("(?<=\\}|\\{)".toRegex())
        for (split in fileSplit) {
            file += split
        }
        if (file.size > 0)
            loadContent(file, context)
    }

    //LOADS THE HEADER AND THE BODY OF THE ARTICLE//
    private fun loadContent(content: List<String>, context: Context) {
        var skipNumber: Int = -1
        for (x in content.indices) {
            if (x > skipNumber) {
                if (content[x].contains("{")) {
                    if (content[x].contains("article-interpreter-for-apps")) {
                        if (content[x + 1].contains("version=")) {
                            interpreterVersion = getValue(content[x + 1], "version").toInt()
                            skipNumber = x + 1
                        }
                    }
                    if (content[x].contains("header")) {
                        Log.d("ArticleInterpreter", "Loading Header")
                        val header = mutableListOf<String>()
                        skipNumber = countCommandLength(content, x + 1)
                        if (skipNumber > -1) {
                            for (z in 0 until skipNumber) {
                                header += content[x + 1 + z]
                            }
                            loadHeader(header)
                            Log.d("ArticleInterpreter", "Header successfully initialized")
                        } else
                            Log.e("ArticleInterpreter", "Header couldn't be initialized")
                    }
                    if (content[x].contains("body")) {
                        Log.d("ArticleInterpreter", "Loading Body")
                        val body = mutableListOf<String>()
                        skipNumber = countCommandLength(content, x + 1)
                        if (skipNumber > -1) {
                            for (z in 0 until skipNumber) {
                                body += content[x + 1 + z]
                            }
                            articleContent = loadBody(context, body)
                            Log.d("ArticleInterpreter", "Body successfully initialized")
                        } else
                            Log.e("ArticleInterpreter", "Body couldn't be initialized")
                    }
                }
            }
        }
    }

    //LOADS THE HEADER AND PUTS IT INTO VARIABLES FOR FURTHER USAGE//
    private fun loadHeader(content: List<String>) {
        Log.d("ArticleInterpreter", "Header Content: $content")
        for (x in content.indices)
            if (content[x].contains("{")) {
                if (content[x].contains("title")) {
                    title = cleanText(content[x + 1], mutableListOf("}"))
                } else if (content[x].contains("type")) {
                    type = getValue(content[x + 1], "")
                } else if (content[x].contains("description")) {
                    description = cleanText(content[x + 1], mutableListOf("}"))
                } else if (content[x].contains("article-hint")) {
                    article_hint = cleanText(content[x + 1], mutableListOf("}"))
                } else if (content[x].contains("favourite")) {
                    favourite = getValue(content[x + 1], "}").toInt()  > 0
                } else if (content[x].contains("recommended")) {
                    recommended = getValue(content[x + 1], "}").toInt() > 0
                }
            }
    }

    //LOADS THE BODY AND PUTS IT INTO VARIABLES FOR FURTHER USAGE//
    private fun loadBody(context: Context, content: List<String>): List<LinearLayout> {
        val myLayout = mutableListOf<LinearLayout>()
        var skipNumber: Int = -1
        for (x in content.indices) {
            if (x > skipNumber)
                if (content[x].contains("{") && content[x].contains("chapter")) {
                    Log.i("ArticleInterpreter", "Chapter found!")
                    val newLayout = LinearLayout(context)
                    var chapterTitle: TextView? = null
                    var chapterHint: TextView? = null
                    if (content[x].contains("(") && content[x].contains(")")) {
                        val chapterInformation =
                            removeFromString(content[x], mutableListOf("chapter", "(", ")", "{"))
                        for (info in chapterInformation.split(";")) {
                            if (info.contains("=")) {
                                if (info.contains("name")) {
                                    chapterTitle = TextView(context)
                                    chapterTitle.setTextAppearance(context, R.style.Article_Content_Header1)
                                    chapterTitle.text = getCleanValue(info, "name")
                                } else if (info.contains("hint")) {
                                    chapterHint = TextView(context)
                                    chapterHint.setTextAppearance(context, R.style.Article_Content_Hint)
                                    chapterHint.setPadding(0,16,0,0)
                                    chapterHint.text = getCleanValue(info, "hint")
                                }
                            }
                        }
                    }
                    newLayout.orientation = LinearLayout.VERTICAL
                    if (chapterTitle != null) {
                        newLayout.addView(chapterTitle)
                        Log.d("ArticleInterpreter", "Initializing Chapter \"${chapterTitle.text}\"")
                    }
                    val myChapter = mutableListOf<String>()
                    skipNumber = countCommandLength(content, x + 1)
                    if (skipNumber > -1) {
                        for (z in 0 until skipNumber) {
                            myChapter += content[x + 1 + z]
                        }
                        for (k in ChapterLoader(context, myChapter, "chapter").getViews()) {
                            newLayout.addView(k)
                            //Log.i("ArticleInterpreter", "Beispiel: ")
                        }
                        Log.d("ArticleInterpreter", "Chapter successfully initialized")
                    } else
                        Log.e("ArticleInterpreter", "Chapter couldn't be initialized")
                    if (chapterHint != null) {
                        newLayout.addView(chapterHint)
                    }
                    myLayout += newLayout
                }
        }
        return myLayout
    }

    //LOADS EACH CHAPTER AND PUTS IT INTO A LINEARLAYOUT FOR FURTHER USAGE IN OTHER CLASSES//
    private class ChapterLoader(pContext: Context, pContent: List<String>, pContentType: String) {

        private var skipNumber: Int = -1 //Fehler: Irgendwie werden die Zahlen nicht übersprungen
        private var views: List<View>

        private var variants = mutableListOf("{", " {")
        private var functions = mutableListOf("text", "unsorted-list", "sorted-list", "header1", "header2", "header3", "card", "description", "lineInfo")

        init {
            Log.d("ArticleInterpreter", "Loading Content")
            if(pContent.size > 1) {
                //Log.e("ArticleInterpreter", "Size of Content: ${pContent.size}")
                views = loadChapter(pContext, pContent, pContentType)
            }
            else {
                //Log.e("ArticleInterpreter", "Size of is smaller than 2")
                views = mutableListOf(loadOnePart(pContext, pContent, pContentType))
            }
            Log.d("ArticleInterpreter", "Content added")
        }

        private fun loadOnePart(context: Context, content: List<String>, contentType: String): View {
            if(contentType.equals("text"))
            {
                val newTextView = TextView(context)
                if(content.isNotEmpty()) {
                    newTextView.text =  cleanText(content[content.size - 1])
                    //Log.e( "ArticleInterpreter","New TextView Content: ${content[content.size - 1]}")
                }
                else {
                    newTextView.text = cleanText(content.toString())
                    Log.e("ArticleInterpreter", "ContentSize is 0 or smaller than 0")
                }
                if(newTextView.text == "") return View(context)
                newTextView.setTextAppearance(context, R.style.Article_Content_Text)
                return newTextView
            } else if(contentType.equals("header1"))
            {
                val newTextView = TextView(context)
                if(content.isNotEmpty()) {
                    newTextView.text =  cleanText(content[content.size - 1])
                    //Log.e( "ArticleInterpreter","New TextView Content: ${content[content.size - 1]}")
                }
                else {
                    newTextView.text = cleanText(content.toString())
                    Log.e("ArticleInterpreter", "ContentSize is 0 or smaller than 0")
                }
                if(newTextView.text == "") return View(context)
                newTextView.setTextAppearance(context, R.style.Article_Content_Header1)
                return newTextView
            } else if(contentType.equals("header2"))
            {
                val newTextView = TextView(context)
                if(content.isNotEmpty()) {
                    newTextView.text =  cleanText(content[content.size - 1])
                    //Log.e( "ArticleInterpreter","New TextView Content: ${content[content.size - 1]}")
                }
                else {
                    newTextView.text = cleanText(content.toString())
                    Log.e("ArticleInterpreter", "ContentSize is 0 or smaller than 0")
                }
                if(newTextView.text == "") return View(context)
                newTextView.setTextAppearance(context, R.style.Article_Content_Header2)
                return newTextView
            } else  if(contentType.equals("header3"))
            {
                val newTextView = TextView(context)
                if(content.isNotEmpty()) {
                    newTextView.text =  cleanText(content[content.size - 1])
                    //Log.e( "ArticleInterpreter","New TextView Content: ${content[content.size - 1]}")
                }
                else {
                    newTextView.text = cleanText(content.toString())
                    Log.e("ArticleInterpreter", "ContentSize is 0 or smaller than 0")
                }
                if(newTextView.text == "") return View(context)
                newTextView.setTextAppearance(context, R.style.Article_Content_Header3)
                return newTextView
            } else if(contentType.equals("description"))
            {
                val newTextView = TextView(context)
                if(content.isNotEmpty()) {
                    newTextView.text =  cleanText(content[content.size - 1])
                    //Log.e( "ArticleInterpreter","New TextView Content: ${content[content.size - 1]}")
                }
                else {
                    newTextView.text = cleanText(content.toString())
                    Log.e("ArticleInterpreter", "ContentSize is 0 or smaller than 0")
                }
                if(newTextView.text == "") return View(context)
                newTextView.setTextAppearance(context, R.style.Article_Content_Description)
                return newTextView
            } else{
                Log.e("ArticleInterpreter", "ContentType does not exist!")
            }
            return View(context)
        }

        private fun loadChapter(context: Context, content: List<String>, contentType: String): List<View> {
            val myViews = mutableListOf<View>()
            for (x in content.indices) {
                Log.i("Loop", "Opening Loop")
                //Log.i("Loop", "Value of X: $x, Value of SkipNumber: $skipNumber")
                if (x > skipNumber) {
                    //Log.d("Loop", "not skipped")
                    Log.d("Loop", "Checking ${content[x]}")
                    if (contentType.equals("chapter")) {
                        Log.d("Loop", "chapter loading")
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("text")) {
                        Log.d("Loop", "text loading")
                        var textContent = content[x]
                        val newTextView = TextView(context)
                        newTextView.setTextAppearance(context, R.style.Article_Content_Text)
                        if (content[x].contains("{")||content.contains("}")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("header1")) {
                        Log.d("Loop", "text loading")
                        var textContent = content[x]
                        val newTextView = TextView(context)
                        newTextView.setTextAppearance(context, R.style.Article_Content_Header1)
                        if (content[x].contains("{")||content.contains("}")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("header2")) {
                        Log.d("Loop", "text loading")
                        var textContent = content[x]
                        val newTextView = TextView(context)
                        newTextView.setTextAppearance(context, R.style.Article_Content_Header2)
                        if (content[x].contains("{")||content.contains("}")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("header3")) {
                        Log.d("Loop", "text loading")
                        var textContent = content[x]
                        val newTextView = TextView(context)
                        newTextView.setTextAppearance(context, R.style.Article_Content_Header3)
                        if (content[x].contains("{")||content.contains("}")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("unsorted-list")) {
                        Log.d("Loop", "unsorted loading")
                        //var textContent = content[x]
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            for(view in loadChapterRepeating(context, content, x))
                            {
                                val newLayout = LinearLayout(context)
                                val space = Space(context)
                                val myLayoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                newLayout.orientation = LinearLayout.HORIZONTAL
                                newLayout.layoutParams = myLayoutParams

                                val mySpaceParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    15F
                                )
                                space.layoutParams = mySpaceParams
                                newLayout.addView(space)

                                val myViewParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1F
                                )
                                view.layoutParams = myViewParams
                                if (view is TextView && !view.text.contains("  - ")) { Log.d("LoopRunningUnsortedList", "Checking view: ${view.text}")
                                    view.text = "- " + view.text }
                                newLayout.addView(view)
                                myViews += newLayout
                            }
                            //textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        }
                    } else if (contentType.equals("sorted-list"))
                    {
                        Log.d("Loop", "sorted loading")
                        var t: Int = 1
                        //var textContent = content[x]
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            for(view in loadChapterRepeating(context, content, x))
                            {
                                val newLayout = LinearLayout(context)
                                val space = Space(context)
                                val myLayoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                newLayout.orientation = LinearLayout.HORIZONTAL
                                newLayout.layoutParams = myLayoutParams

                                val mySpaceParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    15F
                                )
                                space.layoutParams = mySpaceParams
                                newLayout.addView(space)

                                val myViewParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1F
                                )

                                view.layoutParams = myViewParams

                                newLayout.addView(view)
                                myViews += newLayout

                            }
                            //textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        }
                        for(view in myViews)
                            if(view is LinearLayout)
                                for (tView in view.children)
                                    if (tView is TextView && !tView.text.contains("$t. ")) {
                                        tView.text = "$t. " + tView.text
                                        t++
                                    }
                    } else if (contentType == "card") {
                        Log.d("Loop", "card loading")
                        val cardLayout = LinearLayout(ContextThemeWrapper(context, R.style.Article_Content_Card))
                        cardLayout.orientation = LinearLayout.VERTICAL
                        cardLayout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )

                        for (view in ChapterLoader(context, content, "chapter").getViews())
                        {
                            cardLayout.addView(view)
                        }

                        myViews += cardLayout
                    } else if (contentType == "description") {
                        Log.d("Loop", "text loading")
                        var textContent = content[x]
                        val newTextView = TextView(context)
                        newTextView.setTextAppearance(context, R.style.Article_Content_Text)
                        if (content[x].contains("{")||content.contains("}")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType == "lineinfo") {
                        Log.d("Loop", "found {")
                        val myTable = TableLayout(context)
                        var myTableRow = TableRow(context)
                        var zahler = 0
                        for(view in loadChapterRepeating(context, content, x))
                        {
                            if(zahler == 0)
                            {
                                myTableRow = TableRow(context)
                                myTableRow.addView(view)
                                zahler++
                            } else {
                                myTableRow.addView(view)
                                zahler = 0
                                myTable.addView(myTableRow)
                            }
                        }
                        myViews += myTable
                        //textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))

                    }
                }
                //newTextView.paintFlags = newTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }
            return myViews
        }

        private fun loadChapterRepeating(context: Context, content: List<String>, x: Int): List<View> {
            Log.d("Loop", "Reapeating Start")
            val myViews = mutableListOf<View>()
            var foundValue: Boolean = false
            for (i in variants) for (k in functions) {
                if (content[x].contains(k + i) && !foundValue) {
                    //Log.d("Loop", "Openening New Content: $k $i")
                    var myContent = mutableListOf<String>()
                    skipNumber = countCommandLength(content, x + 1) + x
                    if (skipNumber > -1) {
                        if (skipNumber - x != 0) {
                            for (z in 0 until skipNumber - x) {
                                myContent += content[x + 1 + z]
                            }
                            Log.i("CurentContent", "CurrentContent for X ($x): $myContent")
                            for (p in ChapterLoader(context, myContent, k).getViews()) {
                                myViews += p
                            }
                            foundValue = true
                        } else {
                            myContent += content[x + 1]
                            myViews += ChapterLoader(context, myContent, k).getViews()
                            foundValue = true
                        }
                    }
                }
            }
            return myViews
        }

        private fun countCommandLength(content: List<String>, start: Int): Int
        {
            var startOfCommands = 0
            var endOfCommands = 0
            for(x in start until content.size)
            {
                //Log.d("ArticleInterpreter", "Checking ${content[x]}")
                if (content[x].contains("}"))
                {
                    //Log.i("ArticleInterpreter", "found an }")
                    endOfCommands++
                    if(endOfCommands>startOfCommands)
                        return x-start
                }
                if (content[x].contains("{"))
                {
                    //Log.i("ArticleInterpreter", "found an {")
                    startOfCommands++
                }
                //Log.d("ArticleInterpreter", "{ found $startOfCommands times        } found $endOfCommands times")
            }
            Log.e("ArticleInterpreter", "Couldn't count length of content")
            Log.e("ArticleInterpreter", "Length of Content: ${content.size}, Value for Start: $start")
            return -1
        }

        private fun removeFromString(content: String, removale: List<String>): String {
            var newContent = content.replace("  ", "")
            for (remove in removale) {
                newContent = newContent.replace(remove, "")
            }
            newContent = newContent.replace("_", " ")
            return newContent
        }

        fun getViews(): List<View> {
            Log.d("ArticleInterpreter", "Return Content")
            Log.d("ArticleInterpreter", "Size of Views: ${views.size}")
            return views
        }

        private fun cleanText(content: String): String {
            var newContent = content.replace("  ", "")
            newContent = newContent.replace("{", "")
            newContent = newContent.replace("}", "")
            //newContent = newContent.replace("\n", "")
            //Log.d("cleanText", "Content: $newContent")
            return newContent
        }
    } //HINT: The class calls itself if needed to load and split the content further depending on the type of content and the content itself

    //RETURNS ALL CHAPTERS TOGETHER//
    public fun getContent() : List<LinearLayout>
    {
        return articleContent
    }

    //CAN COUNT HOW LONG THE COMMAND HAS TO BE USED AS THE CONTENT TYPE//
    private fun countCommandLength(content: List<String>, start: Int): Int
    {
        var startOfCommands = 0
        var endOfCommands = 0
        for(x in start until content.size)
        {
            //Log.d("ArticleInterpreter", "{ found $startOfCommands times        } found $endOfCommands times") //Content $x: ${content[x]}
            if (content[x].contains("}"))
            {
                //Log.i("ArticleInterpreter", "found an }")
                endOfCommands++
                if(endOfCommands>startOfCommands)
                    return x-start
            }
            if (content[x].contains("{"))
            {
                //Log.i("ArticleInterpreter", "found an {")
                startOfCommands++
            }
        }
        Log.e("ArticleInterpreter", "Couldn't count length of content")
        return -1
    }

    //CAN REMOVE OR REPLACE SPECIFIC STUFF FROM THE STRING//
    private fun removeFromString(content: String, removale: List<String>): String {
        var newContent = content
        newContent = newContent.replace(" ", "")
        for (remove in removale) {
            newContent = newContent.replace(remove, "")
        }
        newContent = newContent.replace("_", "\u0020")
        Log.d("ContentRemoveString", "Result: $newContent")
        return newContent
    }

    //ALMOST LIKE "removeFromString" BUT DOESN'T REMOVE SPACES//
    private fun cleanText(content: String, removale: List<String>): String {
        var newContent = content
        newContent = newContent.replace("  ", "")
        for (remove in removale) {
            newContent = newContent.replace(remove, "")
        }
        newContent = newContent.replace("_", " ")
        Log.d("ContentRemoveString", "Result: $newContent")
        return newContent
    }

    //RETURNS EVERYTHING FROM THE HEADER AS A STRING//
    public fun getArticleInfo() : List<String>
    {
        return mutableListOf(title, type, description, article_hint)
    }

    //RETURNS IF THE ARTICLE IS SET AS RECOMMENDED//
    public fun isRecommended() : Boolean
    {
        return recommended
    }

    //RETURNS IF THE ARTICLE IS SET AS FAVOURITE//
    public fun isFavourite() : Boolean
    {
        return favourite
    }

    //RETURNS THE VALUE FROM A STRING AS A CLEANED UP STRING THAT CAN BE TURNED INTO AN INT IF NECESSARY//
    private fun getValue(content: String, removale: String): String
    {
        val newContent = removeFromString(content, mutableListOf("{", "}"))
        if(newContent.split(removale + "=").size > 0)
            return newContent.split(removale + "=")[newContent.split(removale + "=").size-1]
        else
            return newContent.split(removale + "=")[0]
    }

    //ALMOST LIKE "getValue" BUT USES "cleanText" instead of "removeFromString" WHICH DOESN'T REMOVE NORMAL SPACES//
    private fun getCleanValue(content: String, removale: String): String
    {
        val newContent = cleanText(content, mutableListOf("{", "}"))
        if(newContent.split(removale + "=").size > 0)
            return newContent.split(removale + "=")[newContent.split(removale + "=").size-1]
        else
            return newContent.split(removale + "=")[0]
    }

    //RETURNS THE WHOLE CONTENT IN A UNSORTED WAY. IS USED FOR TESTING PURPOSES ONLY//
    public fun getFileContent(): String
    {
        return removeFromString(file.toString(), mutableListOf("{", "}", "header", "body", "type", "title", "description", "icon",
            "article-hint", "article-interpreter-for-apps", "article", "text", "unsorted-list", "image", "sorted-list", "element", ",", ".", "[", "]"))
    }
}