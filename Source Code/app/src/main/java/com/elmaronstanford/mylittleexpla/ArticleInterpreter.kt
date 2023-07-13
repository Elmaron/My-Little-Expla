package com.elmaronstanford.mylittleexpla

import android.graphics.drawable.Drawable
import kotlin.properties.Delegates

class ArticleInterpreter(pTitle: String, pFile: String) {

    private var file = mutableListOf<String>()

    private var title: String
    private lateinit var type: String
    private lateinit var description: String
    private lateinit var icon: Drawable
    private lateinit var article_hint: String

    private var interpreterVersion by Delegates.notNull<Int>()


    private lateinit var chapters: List<String>


    init {
        title = pTitle
        val fileSplit = pFile.split("(?<=\\{)".toRegex())
        for (split in fileSplit)
        {
            if(split.contains("}"))
            {
                val anotherSplit = split.split("(?=\\})".toRegex())
                for (aSplit in anotherSplit)
                {
                    file += aSplit
                }
            } else
            {
                file += split
            }
        }
        if(file.size > 0)
        for (x in 0..file.size)
        {
            if (getElements(file[x], "type"))
            {
                type = file[x+1]
            } else if (getElements(file[x], "description"))
            {
                description = file[x+1]
            }
        }
    }

    private fun getElements(content: String, keyword: String): Boolean {
        return (content.contains("{") || content.contains("}")) && content.contains(keyword)
    }

    public fun getTestFile() : List<String> { return file }

    private data class preDefined(
        val articleType: List<String> = mutableListOf("short", "middle", "long"),
    )
}