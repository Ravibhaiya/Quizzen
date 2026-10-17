package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.vocabulary.VocabularyManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyManagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bulk JSON add saves multiple words and keeps cards collapsed`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepository()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartAdd()
        assertNotNull(vm.uiState.value.editorState)

        // Blank save fails validation
        assertFalse(vm.onSaveEntry())
        assertNotNull(vm.uiState.value.editorState!!.validationError)

        val bulkJson = """
            [
              {
                "word": "Diligent",
                "meaningEnglish": "Showing care and conscientiousness in one's work",
                "meaningHinglish": "Mehanti aur lagansheel",
                "exampleSentence": "She is a diligent student.",
                "synonyms": [
                  {
                    "word": "Industrious",
                    "meaningEnglish": "Hard-working",
                    "meaningHinglish": "Parishrami",
                    "exampleSentence": "An industrious worker."
                  }
                ],
                "antonyms": [
                  {
                    "word": "Lazy",
                    "meaningEnglish": "Unwilling to work or use energy",
                    "meaningHinglish": "Aalsi",
                    "exampleSentence": "He felt too lazy to cook."
                  }
                ]
              },
              {
                "word": "Tenacious",
                "meaningEnglish": "Tending to keep a firm hold of something",
                "meaningHinglish": "Dridh nishchayi",
                "exampleSentence": "A tenacious negotiator."
              }
            ]
        """.trimIndent()

        vm.onUpdateJsonText(bulkJson)
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        assertNull(vm.uiState.value.editorState)
        assertEquals(2, vm.uiState.value.entries.size)
        // Newly added cards stay collapsed until the user taps the expand arrow
        assertTrue(vm.uiState.value.expandedIds.isEmpty())

        val diligent = vm.uiState.value.entries.first { it.wordDetails.word == "Diligent" }
        assertEquals("Industrious", diligent.synonyms.single().word)
        assertEquals("Lazy", diligent.antonyms.single().word)

        // Clicking the arrow toggles expansion
        vm.onToggleExpand(diligent.id)
        assertTrue(diligent.id in vm.uiState.value.expandedIds)
        vm.onToggleExpand(diligent.id)
        assertFalse(diligent.id in vm.uiState.value.expandedIds)
    }

    @Test
    fun `edit specific word shows only its JSON and updates repository`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepository()
        val initial = repo.saveEntry(
            VocabularyEntry(
                id = "word-1",
                wordDetails = WordDetails(
                    word = "Audacious",
                    meaningEnglish = "Willing to take bold risks",
                    meaningHinglish = "Diler ya sahasi",
                    exampleSentence = "An audacious plan.",
                ),
            ),
        ).getOrThrow()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartEdit(initial)
        val editor = vm.uiState.value.editorState
        assertNotNull(editor)
        assertTrue(editor!!.isEditing)
        assertTrue(editor.jsonText.contains("\"Audacious\""))

        // Update JSON for this specific word
        val updatedJson = """
            {
              "word": "Audacious",
              "meaningEnglish": "Willing to take bold risks",
              "meaningHinglish": "Bahadur aur nidar",
              "exampleSentence": "An audacious plan.",
              "synonyms": [
                {
                  "word": "Bold",
                  "meaningEnglish": "Fearless",
                  "meaningHinglish": "Nidar",
                  "exampleSentence": "A bold move."
                }
              ],
              "antonyms": []
            }
        """.trimIndent()

        vm.onUpdateJsonText(updatedJson)
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        val updated = vm.uiState.value.entries.single()
        assertEquals("Bahadur aur nidar", updated.wordDetails.meaningHinglish)
        assertEquals(1, updated.synonyms.size)
        assertTrue(vm.uiState.value.expandedIds.isEmpty())

        vm.onRequestDelete(updated)
        assertEquals(updated, vm.uiState.value.pendingDelete)
        vm.onConfirmDelete()
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingDelete)
        assertTrue(vm.uiState.value.entries.isEmpty())
    }

    @Test
    fun `search toggle and firebase save failure error surface in state`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepository(failWrites = true)
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSearchActive)
        vm.onToggleSearch()
        assertTrue(vm.uiState.value.isSearchActive)
        vm.onSearchQueryChange("test")
        assertEquals("test", vm.uiState.value.searchQuery)
        vm.onToggleSearch()
        assertFalse(vm.uiState.value.isSearchActive)
        assertEquals("", vm.uiState.value.searchQuery)

        vm.onStartAdd()
        vm.onUpdateJsonText("""{"word": "Apex", "meaningEnglish": "The top or highest part"}""")
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        vm.onDismissError()
        assertNull(vm.uiState.value.errorMessage)
    }

    private class FakeVocabularyRepository(
        private val failWrites: Boolean = false,
    ) : VocabularyRepository {
        private val flow = MutableStateFlow<List<VocabularyEntry>>(emptyList())
        override val entries: Flow<List<VocabularyEntry>> = flow
        private val errorFlow = MutableStateFlow<String?>(null)
        override val syncError: Flow<String?> = errorFlow
        private var counter = 0

        override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> =
            saveEntries(listOf(entry)).map { it.first() }

        override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> {
            if (failWrites) {
                val msg = "Failed to save vocabulary to Firebase: Permission denied"
                errorFlow.value = msg
                return Result.failure(IllegalStateException(msg))
            }
            val savedList = entries.map { entry ->
                val id = entry.id.ifBlank { "id-${++counter}" }
                entry.copy(id = id)
            }
            val savedIds = savedList.map { it.id }.toSet()
            flow.value = flow.value.filterNot { it.id in savedIds } + savedList
            return Result.success(savedList)
        }

        override suspend fun deleteEntry(id: String): Result<Unit> {
            if (failWrites) {
                val msg = "Failed to delete word from Firebase"
                errorFlow.value = msg
                return Result.failure(IllegalStateException(msg))
            }
            flow.value = flow.value.filterNot { it.id == id }
            return Result.success(Unit)
        }

        override fun clearSyncError() {
            errorFlow.value = null
        }
    }
}
