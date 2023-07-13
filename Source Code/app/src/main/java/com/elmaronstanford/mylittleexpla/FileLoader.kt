package com.elmaronstanford.mylittleexpla

import android.content.Context
import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class FileLoader(context: Context, pFolderPath: String, fileExtension: String) {

    private val assetManager = context.assets
    private var pathTofile: List<String>
    private var folderPath: String

    init {
        pathTofile = getFilesFromSubfolder(assetManager, pFolderPath)
        folderPath = pFolderPath
    }



    private fun getFilesFromSubfolder(assetManager: AssetManager, subfolderPath: String): List<String> {
        val files = mutableListOf<String>()
        try {
            val subfolders = assetManager.list(subfolderPath)
            subfolders?.forEach { subfolder ->
                val fullPath = "$subfolderPath/$subfolder"
                files.addAll(getFilesFromSubfolder(assetManager, fullPath))
            }

            val subfolderFiles = assetManager.list(subfolderPath)?.map { "$subfolderPath/$it" }
            subfolderFiles?.let { files.addAll(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return files
    }

    public fun getPathFiles() : List<String> { return pathTofile }

    public fun getFile(position: Int): String
    {
        return assetManager.open(pathTofile[position]).toString()
    }

    public fun getFileContentsFromSubfolder(): List<String> {
        val fileContents = mutableListOf<String>()
        try {
            val files = assetManager.list(folderPath)
            files?.forEach { fileName ->
                val filePath = "$folderPath/$fileName"
                val inputStream = assetManager.open(filePath)
                val fileContent = inputStream.bufferedReader().use { it.readText() }
                fileContents.add(fileContent)
                inputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fileContents
    }



}