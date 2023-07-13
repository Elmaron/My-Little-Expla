package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


object Converters {
    @TypeConverter
    fun fromString(value: String?): Drawable {
        return BitmapDrawable(BitmapFactory.decodeFile(value))
    }

    @TypeConverter
    fun drawableToString(drawable: Drawable?): String? {
        if(drawable == null) return null
        val bitmap = (drawable as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapBytes = stream.toByteArray()
        return Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
    }
}


@Database(entities = [Article::class, Tag::class, ArticleTags::class, Chapter::class, ChapterContent::class, ContentType::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDatabase : RoomDatabase() {

    //------------ADDING-INTERFACES------------//
    abstract fun tagDao(): TagDao
    abstract fun articleDao(): ArticleDao
    abstract fun chapterDao(): ChapterDao
    abstract fun chapterContentDao(): ChapterContentDao
    abstract fun contentTypeDao(): ContentTypeDao


    //-----------CREATION-OF-TABLE-----------//
    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "localDatabase"
                )
                    .addCallback(AppDatabaseCallback(context))// Add the callback here
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(private val context: Context) : Callback() {

            @OptIn(DelicateCoroutinesApi::class)
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                Log.d("AppDatabaseCallback", "onCreate method called")

                //-----DATA-INSERTION-----//

                // ö = \u00F6
                // ä = \u00E4
                // ü = \u00FC

                createTags(context)
                createContentTypes(context)
                createArticles(context)
                createChapters(context)
                createChapterContents(context)

            }

            @OptIn(DelicateCoroutinesApi::class)
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                GlobalScope.launch(Dispatchers.IO) {
                    if (getInstance(context).articleDao().getArticles().first().isEmpty())
                    {
                        createArticles(context)
                    }
                    if(getInstance(context).chapterDao().getChapters().first().isEmpty() && getInstance(context).articleDao().getArticles().first().isNotEmpty())
                    {
                        createChapters(context)
                    }
                    if(getInstance(context).chapterContentDao().getAllChapterContents().first().isEmpty() && getInstance(context).chapterDao().getChapters().first().isNotEmpty() && getInstance(context).articleDao().getArticles().first().isNotEmpty())
                    {
                        createChapterContents(context)
                    }
                }

            }

            @OptIn(DelicateCoroutinesApi::class)
            private fun createArticles(context: Context)
            {
                GlobalScope.launch (Dispatchers.IO) {
                    //Hinweise zur Länge: [1 -> activity_article_short][2 -> activity_article_middle][3 -> activity_article_long]
                    //Hinweise zur Länge Short: Beschreibung ist der Inhalt des Artikels

                    val myArticles = listOf(
                        Article(title = "Dateifreigabe", description = "M\u00F6glichkeiten der Dateifreigabe auf allen m\u00F6glichen Geräten", icon = null, length = 2, hint = null),
                        Article(title = "Tastenkombinationen", description = "Tastenkombinationen für alle m\u00F6glichen Plattformen", icon = null, length = 3, hint = null)
                    )

                    for (myArticle in myArticles)
                    {
                        getInstance(context).articleDao().insert(myArticle)
                    }

                    Log.d("AppDataBaseCallback", "Articles inserted successfully")
                }
            }

            @OptIn(DelicateCoroutinesApi::class)
            private fun createChapters(context: Context)
            {
                GlobalScope.launch (Dispatchers.IO) {
                    val myChapters = listOf(

                        //Artikel: Dateifreigabe
                        getArticleID(context, "Dateifreigabe")?.let { Chapter(idArticle = it, title = "M\u00F6glichkeiten der Dateifreigabe", description = null, icon = null) },
                        getArticleID(context, "Dateifreigabe")?.let { Chapter(idArticle = it, title = "Freigabe per Direktverbindung", description = null, icon = null) },
                        getArticleID(context, "Dateifreigabe")?.let { Chapter(idArticle = it, title = "Dateifreigabe per App", description = null, icon = null) },
                        getArticleID(context, "Dateifreigabe")?.let { Chapter(idArticle = it, title = "Dateien per Cloud freigeben", description = null, icon = null) },

                        //Artikel: Tastenkobinationen
                        getArticleID(context, "Tastenkombinationen")?.let { Chapter(idArticle = it, title = "Was ist das?", description = null, icon = null) },
                        getArticleID(context, "Tastenkombinationen")?.let { Chapter(idArticle = it, title = "Grundlagen", description = null, icon = null) },
                        getArticleID(context, "Tastenkombinationen")?.let { Chapter(idArticle = it, title = "Word", description = null, icon = null) },

                        //


                    )


                    for (myChapter in myChapters)
                    {
                        if (myChapter != null) {
                            getInstance(context).chapterDao().insert(myChapter)
                        }
                    }

                    Log.d("AppDataBaseCallback", "Chapters inserted successfully")
                }
            }

            @OptIn(DelicateCoroutinesApi::class)
            private fun createChapterContents(context: Context)
            {
                GlobalScope.launch (Dispatchers.IO) {
                    val contents = listOf(

                        //Artikel: Dateifreigabe

                        //Kapitel: Möglichkeiten der Dateifreigabe
                        getChapterID(context, "M\u00F6glichkeiten der Dateifreigabe", "Dateifreigabe")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Es gibt einige grundlegende Dinge, die man sich fragen sollte, bevor man eine Datei versendet. Dazu gehören folgende Fragen:")
                            }
                        },
                        getChapterID(context, "M\u00F6glichkeiten der Dateifreigabe", "Dateifreigabe")?.let {
                            getContentTypeID(context, "unsorted-list-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "- Wie schnell muss die Datei ankommen?\n- Wie viele Empf\u00E4nger hat die Datei?\n-Wie groß ist die Datei?\n- Wie viele Dateien sollen versendet werden?\n- Wie lange soll die Datei verf\u00FCgbar sein?")
                            }
                        },
                        getChapterID(context, "M\u00F6glichkeiten der Dateifreigabe", "Dateifreigabe")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Im Hinblick auf diese Fragen k\u00F6nnen unsere M\u00F6glichkeiten in 3 verschiedene Kategorien eingeteilt werden:")
                            }
                        },
                        getChapterID(context, "M\u00F6glichkeiten der Dateifreigabe", "Dateifreigabe")?.let {
                            getContentTypeID(context, "unsorted-list-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "- Dateifreigabe per Direktverbindung\n- Dateifreigabe per App\n- Dateifreigabe per Cloud")
                            }
                        },

                        //Kapitel: Freigabe per Direktverbindung
                        getChapterID(context, "Freigabe per Direktverbindung", "Dateifreigabe")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Mit Direktverbindung sind sowohl klassische Methoden (wie bspw. per Kabel oder USB-Stick), als auch kabellose Verbindungen wie bspw. über Bluetooth gemeint.\n" +
                                            "Direktverbindungen eignen sich für eine schnelle und sichere Freigabe von einzelnen Dateien an einen einzelnen Empfänger am besten. Die Dateien bekommen in " +
                                            "diesem Fall (bis auf Quick Share) auch keine zeitliche Begrenzung.\nFolgende Direktverbindungen werden in eigenen Artikeln erklärt:")
                            }
                        },
                        getChapterID(context, "Freigabe per Direktverbindung", "Dateifreigabe")?.let {
                            getContentTypeID(context, "unsorted-list-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "- Kabelgebunden\n- Kabellose Übtertragunsmöglichkeiten:\n  - Bluetooth\n  - Nearby Share\n  - Quick Share (Samsung)")
                            }
                        },
                        getChapterID(context, "Freigabe per Direktverbindung", "Dateifreigabe")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Sollten mir in Zukunft noch mehr und bessere Möglichkeiten für eine Direktverbindung bekannt sein, werde ich diese hier ergänzen.")
                            }
                        },

                        //Kapitel: Dateifreigabe per App
                        getChapterID(context, "Dateifreigabe per App", "Dateifreigabe")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Mit der Dateifreigabe per App sind vor allem Apps wie Whatsapp, Signal, aber auch Instagram gemeint. Wie die Dateifreigabe in diesen funktioniert, " +
                                            "wird in den jeweiligen Artikeln zu den Apps behandelt.\nPer App können zumeist dauerhafte Freigaben von einzelnen oder mehreren Dateien an einen " +
                                            "oder mehrere Empfänger erstellt werden. Manche Apps bieten sogar zusätzliche Möglichkeiten zur Einstellung, was eine bessere Kontrolle über die Verwendung und " +
                                            "weitere Verbreitung von Dateien ermöglicht.\nFolgende Apps werden in eigenen Artikeln behandelt:")
                            }
                        },
                        getChapterID(context, "Dateifreigabe per App", "Dateifreigabe")?.let {
                            getContentTypeID(context, "unsorted-list-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "- Whatsapp\n- Signal\n- Instagram\n- Nachrichten (Samsung)\n- Messages\n- Gmail")
                            }
                        },
                        getChapterID(context, "Dateifreigabe per App", "Dateifreigabe")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Da sich die Apps ständig verändern, ist es möglich, das die Artikel nicht immer ganz auf dem neuesten Stand sind. Allerdings versuche ich sie so " +
                                            "bald wie möglich zu aktualisieren. Die Funktionsweise der Freigabe sollte in den entsprechenden Apps aber gleich bleiben.")
                            }
                        },

                        //Kapitel: Dateien per Cloud freigeben
                        getChapterID(context, "Dateien per Cloud freigeben", "Dateifreigabe")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Clouds haben viele Funktionen. Während die Wolken (deutsche Übersetzung von &quot;Cloud&quot;) allerdings vor allem dafür geeignet sind, Dateien wie Fotos, Videos " +
                                            "und andere Dokumente langfristig zu speichern, sodass man aber Zugriff von jedem Gerät aus hat, ist es auch möglich Dateien an andere freizugeben.\nClouds eignen sich " +
                                            "in diesem Fall für jede der zu Beginn gestellten Fragen, solange Sender und Empfänger über Internet verfügen.\nFolgende Cloud-Speicher werden in dieser App behandelt:")
                            }
                        },
                        getChapterID(context, "Dateien per Cloud freigeben", "Dateifreigabe")?.let {
                            getContentTypeID(context, "unsorted-list-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "- Google Drive\n- Microsoft OneDrive\n- Dropbox")
                            }
                        },
                        getChapterID(context, "Dateien per Cloud freigeben", "Dateifreigabe")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Die Oberfläche der Cloud-Services kann sich ab und an mal ändern, aber ich versuche die Artikel so gut es geht aktuell zu halten.")
                            }
                        },


                        //!!!IMPORTANT ARTICLES!!!//

                        //Artikel: Tastenkombinationen

                        //Kapitel: Was sind Tastenkombinationen?
                        getChapterID(context, "Was ist das?", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "text")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Um bspw. in Programmen auf dem PC schneller arbeiten zu können, gibt es sogenannte Tastekombinationen (auch Kürzel oder Shortcuts). " +
                                            "Daf\u00F6r m\u00F6ssen nur ein paar Tasten auf der Tastatur gleichzeitig gedr\u00F6t sein. Das heißt, man kann sie auch nacheinander dr\u00F6cken, " +
                                            "solange man die bereits gedr\u00F6ckten Tasten solange gedr\u00F6ckt h\u00E4lt, bis man alle gedr\u00F6ckt wurden.\n" +
                                            "In diesem Artikel findet sich eine Tabelle mit allerlei möglichen Shortcuts. Seien es allseits bekannte Dinge, wie kopieren und einfügen, " +
                                            "aber auch Dinge wie die Schriftart in Word zu \u00E4ndern (programmspezifischere Funktionen) sind mit dabei.")
                            }
                        },

                        //Kapitel: Grundlagen
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "chapter-hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "In diesem Bereich finden sich Shortcuts für den allgemeinen Gebrauch von Computern und Smartphones.")
                            }
                        },

                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-start")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "header3")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "R\u00F6ckg\u00E4ngig")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "In fast jedem Programm und auch auf dem Desktop oder dem Explorer kann man mit dieser Funktion jeden Fehler rückgängig machen. Funktioniert auch in vielen Android Apps.")
                            }
                        },
                        getContentTypeID(context, "one-line-info")?.let {
                            getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let { it1 ->
                                ChapterContent(idChapter = it1, idContentType = it,
                                    content = "Tastenkombination: ")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "STRG + Z")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Plattformen: ")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Windows, Linux, Mac, Android, IOS")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-end")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },

                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-start")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "header3")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Wiederherstellen")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Genauso, wie man Schritte zurück gehen kann, lassen sich die zurückgegangen Schritte auch wiederherstellen. Dies funktioniert ebenfalls in fast allen Programmen. " +
                                            "Auf Computern kann in manchen Fällen das Programm aber auch auf die Alternative zurückgreifen, jedoch wird eine dieser Kombinationen immer gehen.")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Tastenkombination: ")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "STRG + Y")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Alternative Kombination: ")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "STRG + SHIFT + Z")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Plattformen: ")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Windows, Linux, Mac, Android, IOS")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-end")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },

                        getContentTypeID(context, "card-start")?.let {
                            getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let { it1 ->
                                ChapterContent(idChapter = it1, idContentType = it,
                                    content = "<empty>")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "header3")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Alles Markieren")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Durch diese Tastenkombination wird der gesamte Inhalt im bearbeitbaren Bereich markiert.")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Tastenkombination: ")
                            }
                        },
                        getContentTypeID(context, "one-line-info-element")?.let {
                            getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let { it1 ->
                                ChapterContent(idChapter = it1, idContentType = it,
                                    content = "STRG + A")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Plattformen: ")
                            }
                        },
                        getContentTypeID(context, "one-line-info-element")?.let {
                            getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let { it1 ->
                                ChapterContent(idChapter = it1, idContentType = it,
                                    content = "Windows, Linux, Mac, Android, IOS")
                            }
                        },
                        getChapterID(context, "Grundlagen", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-end")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },


                        //Kapitel: Word
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "chapter-hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "In diesem Bereich finden sich Shortcuts für speziell für Word.")
                            }
                        },

                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-start")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "header3")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Fetter Text")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "hint")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Mit dieser Tastenkombination kann der markierte Text fett gemacht oder dieser Textstil wieder entfernt werden.")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Tastenkombination: ")
                            }
                        },
                        getContentTypeID(context, "one-line-info-element")?.let {
                            getChapterID(context, "Word", "Tastenkombinationen")?.let { it1 ->
                                ChapterContent(idChapter = it1, idContentType = it,
                                    content = "STRG + F")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Plattformen: ")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "one-line-info-element")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "Windows, Linux, Mac, Android, IOS")
                            }
                        },
                        getChapterID(context, "Word", "Tastenkombinationen")?.let {
                            getContentTypeID(context, "card-end")?.let { it1 ->
                                ChapterContent(idChapter = it, idContentType = it1,
                                    content = "<empty>")
                            }
                        },


                        )

                    for (content in contents)
                    {
                        if (content != null) {
                            getInstance(context).chapterContentDao().insert(content)
                        }
                    }

                    Log.d("AppDataBaseCallback", "Content inserted successfully")
                }
            }

            @OptIn(DelicateCoroutinesApi::class)
            private fun createTags(context: Context)
            {
                GlobalScope.launch (Dispatchers.IO) {
                    val appTags = listOf(
                        Tag(title = "favourites"),
                        Tag(title = "recommended"),
                        Tag(title = "android"),
                        Tag(title = "windows")
                    )

                    for (appTag in appTags) {
                        getInstance(context).tagDao().insert(appTag)
                    }

                    for (instance in getInstance(context).tagDao().getTags().first()) {
                        Log.d(
                            "AppDataBaseCallback",
                            "Tag: " + instance.tagID + "(Title: " + instance.title + ")"
                        )
                    }
                    Log.d("AppDataBaseCallback", "Tags inserted successfully")
                }
            }

            @OptIn(DelicateCoroutinesApi::class)
            private fun createContentTypes(context: Context)
            {
                GlobalScope.launch (Dispatchers.IO) {
                    val allContentTypes = listOf(
                        ContentType(type = "text"),
                        ContentType(type = "header1"),
                        ContentType(type = "header2"),
                        ContentType(type = "header3"),
                        ContentType(type = "description"),
                        ContentType(type = "one-line-info"),            //[Beispiel] Tastenkombination:
                        ContentType(type = "one-line-info-element"),    //[Beispiel]                        STRG + V
                        ContentType(type = "one-line-info-end"),
                        ContentType(type = "unsorted-list-element"),    //[Beispiel]  - blabla
                        ContentType(type = "sorted-list-element"),      //[Beispiel] 1. blabla
                        ContentType(type = "image"),
                        ContentType(type = "hint"),
                        ContentType(type = "chapter-hint"),
                        ContentType(type = "card-start"),
                        ContentType(type = "card-end")
                    )

                    for (allContentType in allContentTypes)
                    {
                        getInstance(context).contentTypeDao().insert(allContentType)
                    }

                    Log.d("AppDataBaseCallback", "Contenttypes inserted successfully")
                }
            }


            //--------------CONNECTIONS-BETWEEN-TABLES--------------//

            private fun getArticleID(context: Context, title: String): Long?
            {
                return getInstance(context).articleDao().getArticleID(title)
            }

            private fun getChapterID(context: Context, chapter: String, article: String): Long?
            {
                return getInstance(context).chapterDao().getChapterFromArticle(chapter, getArticleID(context, article))
            }

            private fun getContentTypeID(context: Context, title: String): Long?
            {
                return getInstance(context).contentTypeDao().getContentTypeID(title)
            }
        }
    }
}


//--------------TABLES--------------//

@Entity(tableName="article")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val articleID: Long = 0,
    val title: String,
    val description: String,
    val icon: Drawable?,
    val length: Short,
    val hint: String?
)

@Entity(tableName = "tag")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val tagID: Long = 0,
    val title: String
)

@Entity(tableName = "articleTags", foreignKeys = [
    ForeignKey(entity = Article::class, parentColumns = ["articleID"], childColumns = ["idArticle"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ForeignKey(entity = Tag::class, parentColumns = ["tagID"], childColumns = ["idTag"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
],
    indices = [Index(value = ["idArticle"]), Index(value = ["idTag"])])
data class ArticleTags(
    @PrimaryKey(autoGenerate = true)
    val articleTagsID: Long = 0,
    @ColumnInfo(name = "idArticle")
    val idArticle: Long,
    val idTag: Long
)

@Entity(tableName = "chapter", foreignKeys = [
    ForeignKey(entity = Article::class, parentColumns = ["articleID"], childColumns = ["idArticle"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
],
    indices = [Index(value = ["idArticle"])])
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val chapterID: Long = 0,
    @ColumnInfo(name = "idArticle")
    val idArticle: Long,
    val title: String,
    val description: String?,
    val icon: Drawable?
)

@Entity(tableName = "chapterContent", foreignKeys = [
    ForeignKey(entity = Chapter::class, parentColumns = ["chapterID"], childColumns = ["idChapter"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ForeignKey(entity = ContentType::class, parentColumns = ["contentTypeID"], childColumns = ["idContentType"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
],
    indices = [Index(value = ["idChapter"]), Index(value = ["idContentType"])])
data class ChapterContent(
    @PrimaryKey(autoGenerate = true)
    val chapterContentID: Long = 0,
    @ColumnInfo(name = "idChapter")
    val idChapter: Long,
    @ColumnInfo(name = "idContentType")
    val idContentType: Long,
    val content: String
)

@Entity(tableName = "contentType")
data class ContentType(
    @PrimaryKey(autoGenerate = true)
    val contentTypeID: Long = 0,
    val type: String
)


//--------------INTERFACES--------------//

@Dao
interface TagDao {
    @Insert
    suspend fun insert(tag: Tag) : Long

    @Query("SELECT * FROM tag")
    fun getTags() : Flow<List<Tag>>

    @Query("SELECT tag.tagID FROM tag WHERE tag.title = :tagTitle")
    fun getTagID(tagTitle: String): Long
}

@Dao
interface ArticleDao {
    @Insert
    suspend fun insert(article: Article): Long

    @Query("SELECT * FROM article")
    fun getArticles() : Flow<List<Article>>

    @Query("SELECT article.articleID FROM article WHERE article.title = :articleTitle")
    fun getArticleID(articleTitle: String) : Long

    @Query("SELECT article.* FROM article inner join articleTags inner join tag ON article.articleID == articleTags.idArticle AND tag.tagID == articleTags.idTag WHERE tag.title == \"favourites\"")
    fun getFavouritesID() : Flow<List<Article>>

    @Query("SELECT article.* FROM article inner join articleTags inner join tag ON article.articleID == articleTags.idArticle AND tag.tagID == articleTags.idTag WHERE tag.title == \"recommended\"")
    fun getRecommendedID() : Flow<List<Article>>
}

@Dao
interface ChapterDao {
    @Insert
    suspend fun insert(chapter: Chapter): Long?

    @Query("SELECT * FROM chapter")
    fun getChapters() : Flow<List<Chapter>>

    @Query("SELECT chapter.chapterID FROM chapter, article WHERE chapter.title = :chapterTitle AND article.articleID = :articleID")
    fun getChapterFromArticle(chapterTitle: String, articleID: Long?) : Long?

    @Query("SELECT chapter.* FROM chapter, article WHERE article.articleID = :articleID")
    fun getChaptersFromArticle(articleID: Long) : Flow<List<Chapter>>
}

@Dao
interface ChapterContentDao {
    @Insert
    suspend fun insert(chapterContent: ChapterContent): Long?

    @Query("SELECT chapterContent.* FROM chapterContent, chapter WHERE chapter.chapterID = :chapter")
    fun getChapterContent(chapter: Long) : Flow<List<ChapterContent>>

    @Query("SELECT * FROM chapterContent")
    fun getAllChapterContents() : Flow<List<ChapterContent>>
}

@Dao
interface ContentTypeDao {
    @Insert
    suspend fun insert(contentType: ContentType) : Long?

    @Query("SELECT * FROM contentType")
    fun getContentTypes() : Flow<List<ContentType>>

    @Query("SELECT type FROM contentType WHERE contentTypeID = :ID")
    fun getContentType(ID: Long) : String?

    @Query("SELECT contentType.contentTypeID FROM contentType WHERE contentType.type = :type")
    fun getContentTypeID(type: String): Long?
}