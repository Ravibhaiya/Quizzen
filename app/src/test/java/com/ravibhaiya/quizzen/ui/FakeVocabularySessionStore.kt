package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularySessionStore

/** In-memory stand-in for the file that keeps the unfinished Vocabulary quiz. */
internal class FakeVocabularySessionStore(var text: String? = null) : VocabularySessionStore {
    var clearCount = 0
        private set

    override suspend fun load(): String? = text

    override fun save(text: String) {
        this.text = text
    }

    override fun clear() {
        text = null
        clearCount++
    }
}
