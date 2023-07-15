package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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


    private lateinit var chapters: List<String>


    init {
        val fileSplit = pFile.split("(?<=\\{)".toRegex())
        for (split in fileSplit) {
            if (split.contains("}")) {
                val anotherSplit = split.split("(?=\\})".toRegex())
                for (aSplit in anotherSplit) {
                    file += aSplit
                }
            } else {
                file += split
            }
        }
        if (file.size > 0)
            loadContent(file, context)
    }

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

    private fun loadHeader(content: List<String>) {
        Log.d("ArticleInterpreter", "Header Content: $content")
        for (x in content.indices)
            if (content[x].contains("{")) {
                if (content[x].contains("title")) {
                    title = content[x + 1]
                } else if (content[x].contains("type")) {
                    type = content[x + 1]
                } else if (content[x].contains("description")) {
                    description = content[x + 1]
                } else if (content[x].contains("article-hint")) {
                    article_hint = content[x + 1]
                } else if (content[x].contains("favourite")) {
                    favourite = content[x + 1].toInt() > 0
                } else if (content[x].contains("recommended")) {
                    recommended = content[x + 1].toInt() > 0
                }
            }
    }

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
                                    chapterTitle.text = getValue(info, "name")
                                } else if (info.contains("hint")) {
                                    chapterHint = TextView(context)
                                    chapterHint.text = getValue(info, "hint")
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

    private class ChapterLoader(pContext: Context, pContent: List<String>, pContentType: String) {

        private var skipNumber: Int = -1 //Fehler: Irgendwie werden die Zahlen nicht übersprungen
        private var views: List<View>

        private var variants = mutableListOf("{", " {")
        private var functions = mutableListOf("text", "unsorted-list", "sorted-list")

        init {
            Log.d("ArticleInterpreter", "Loading Content")
            if(pContent.size > 1)
                views = loadChapter(pContext, pContent, pContentType)
            else
                views = mutableListOf(loadOnePart(pContext, pContent, pContentType))
            Log.d("ArticleInterpreter", "Content added")
        }

        private fun loadOnePart(context: Context, content: List<String>, contentType: String): View {
            if(contentType.equals("text"))
            {
                val newTextView = TextView(context)
                newTextView.setText(content.toString())
                return newTextView
            }
            return View(context)
        }

        private fun loadChapter(context: Context, content: List<String>, contentType: String): List<View> {
            val myViews = mutableListOf<View>()
            for (x in content.indices) {
                Log.i("Loop", "Opening Loop")
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
                        if (content[x].contains("{")) textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        newTextView.setText(textContent)
                        myViews += newTextView
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                        }
                    } else if (contentType.equals("unsorted-list")||contentType.equals("sorted-list")) {
                        Log.d("Loop", "unsorted loading")
                        //var textContent = content[x]
                        if (content[x].contains("{")) {
                            Log.d("Loop", "found {")
                            myViews += loadChapterRepeating(context, content, x)
                            //textContent = removeFromString(content[x], functions + variants + mutableListOf("  ", "}"))
                        }
                        if(contentType.equals("unsorted-list")) {
                            for (view in myViews) {
                                if (view is TextView) view.text = "- " + view.text
                            }
                        }
                        else {
                            var t: Int = 1
                            for (view in myViews) {
                                if (view is TextView) view.text = "$t " + view.text
                                t++
                            }
                        }
                    }
                }
            }
            return myViews
        }

        private fun loadChapterRepeating(context: Context, content: List<String>, x: Int): List<View> {
            Log.d("Loop", "Reapeating Start")
            val myViews = mutableListOf<View>()
            for (i in variants) for (k in functions){
                if (content[x].contains(k + i)) {
                    //Log.d("Loop", "Openening New Content: $k $i")
                    var myContent = mutableListOf<String>()
                    skipNumber = countCommandLength(content, x + 1)
                    if (skipNumber > -1) {
                        for (z in 0 until skipNumber)
                            myContent += content[x + 1 + z]
                        for (k in ChapterLoader(context, myContent, k).getViews())
                            myViews += k
                    } else if (skipNumber == -1)
                    {

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
                Log.d("ArticleInterpreter", "Checking ${content[x]}")
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
                Log.d("ArticleInterpreter", "{ found $startOfCommands times        } found $endOfCommands times")
            }
            Log.e("ArticleInterpreter", "Couldn't count length of content")
            Log.e("ArticleInterpreter", "Length of Content: ${content.size}, Value for Start: $start")
            return -1
        }

        private fun removeFromString(content: String, removale: List<String>): String {
            content.replace(" ", "")
            for (remove in removale) {
                content.replace(remove, "")
            }
            content.replace("_", " ")
            return content
        }

        fun getViews(): List<View> {
            Log.d("ArticleInterpreter", "Return Content")
            Log.d("ArticleInterpreter", "Size of Views: ${views.size}")
            return views
        }
    }

    public fun getContent() : List<LinearLayout>
    {
        return articleContent
    }


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

    private fun removeFromString(content: String, removale: List<String>): String {
        content.replace(" ", "")
        for (remove in removale) {
            content.replace(remove, "")
        }
        content.replace("_", " ")
        return content
    }

    public fun getArticleInfo() : List<String>
    {
        return mutableListOf(title, type, description, article_hint)
    }

    public fun isRecommended() : Boolean
    {
        return recommended
    }

    public fun isFavourite() : Boolean
    {
        return favourite
    }

    private fun getValue(content: String, removale: String): String
    {
        return content.split(removale + "=")[1]
    }

    public fun getFileContent(): String
    {
        return removeFromString(file.toString(), mutableListOf("{", "}", "header", "body", "type", "title", "description", "icon",
            "article-hint", "article-interpreter-for-apps", "article", "text", "unsorted-list", "image", "sorted-list", "element", ",", ".", "[", "]"))
    }

    private fun getElements(content: String, keyword: String): Boolean {
        return content.contains("{") && content.contains(keyword)
    }

    public fun getTestFile() : List<String> { return file }

    private data class preDefined(
        val articleType: List<String> = mutableListOf("short", "middle", "long"),
    )
}