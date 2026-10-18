package com.ravibhaiya.quizzen.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Keeps the one unfinished **Vocabulary** quiz on the device so it can be resumed after the app was closed. It is the only
 * place where a running quiz is stored; Math quizzes never are (see `AGENTS.md`, "Vocabulary session resume").
 *
 * The text is produced and read by `VocabularyQuizSavedState`; this class only moves it to and from storage.
 * [save] and [clear] return immediately and are applied in call order, even when the quiz screen is already gone.
 * [load] gives `null` when nothing is stored or the storage cannot be read.
 */
interface VocabularySessionStore {
    suspend fun load(): String?
    fun save(text: String)
    fun clear()
}

/** Used where resuming is not wanted (and by tests that do not care about it). */
object NoVocabularySessionStore : VocabularySessionStore {
    override suspend fun load(): String? = null
    override fun save(text: String) = Unit
    override fun clear() = Unit
}

/** One small file in app storage (`vocabulary_session_v1.txt`), written atomically so a crash never leaves half a quiz. */
class FileVocabularySessionStore(context: Context) : VocabularySessionStore {

    private val file = File(context.applicationContext.filesDir, FILENAME)

    override suspend fun load(): String? = withContext(writerDispatcher) {
        runCatching { file.takeIf { it.exists() }?.readText(Charsets.UTF_8) }
            .onFailure { Log.w(TAG, "Failed reading the saved vocabulary quiz", it) }
            .getOrNull()
    }

    override fun save(text: String) {
        scope.launch { write(text) }
    }

    override fun clear() {
        scope.launch { delete() }
    }

    private fun write(text: String) {
        runCatching {
            val temp = File(file.parentFile, "$FILENAME.tmp")
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                file.delete()
                check(temp.renameTo(file)) { "rename failed" }
            }
        }.onFailure { Log.w(TAG, "Failed saving the vocabulary quiz", it) } // never crash because of a lost save
    }

    private fun delete() {
        runCatching { file.delete() }.onFailure { Log.w(TAG, "Failed clearing the saved vocabulary quiz", it) }
    }

    private companion object {
        const val TAG = "VocabularySessionStore"
        const val FILENAME = "vocabulary_session_v1.txt"

        /** One thread for the whole app, so writes and clears happen in the order they were asked for. */
        val writerDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "vocabulary-session-writer").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        /** Not tied to a screen or ViewModel: a save asked for while leaving a quiz must still finish. */
        val scope = CoroutineScope(SupervisorJob() + writerDispatcher)
    }
}

/** The store for ViewModels created through `viewModelFactory { initializer { ... } }`. */
internal fun CreationExtras.vocabularySessionStore(): VocabularySessionStore =
    FileVocabularySessionStore(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
