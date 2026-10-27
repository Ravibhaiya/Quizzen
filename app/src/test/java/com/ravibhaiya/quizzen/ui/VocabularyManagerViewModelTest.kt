package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyRules
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
    fun `edit specific word requires capital CONFIRM and single delete requires capital DELETE`() = runTest(dispatcher) {
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
        assertFalse(editor.canSave)
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

        // Saving edit without CONFIRM or with lowercase confirm fails
        assertFalse(vm.onSaveEntry())
        assertNotNull(vm.uiState.value.editorState!!.validationError)

        vm.onUpdateEditConfirmText("confirm")
        assertFalse(vm.uiState.value.editorState!!.canSave)
        assertFalse(vm.onSaveEntry())

        // Typing CONFIRM in capital letters allows saving the edit
        vm.onUpdateEditConfirmText("CONFIRM")
        assertTrue(vm.uiState.value.editorState!!.canSave)
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        val updated = vm.uiState.value.entries.single()
        assertEquals("Bahadur aur nidar", updated.wordDetails.meaningHinglish)
        assertEquals(1, updated.synonyms.size)
        assertTrue(vm.uiState.value.expandedIds.isEmpty())

        // Deleting requires typing DELETE in capital letters
        vm.onRequestDelete(updated)
        assertEquals(listOf(updated), vm.uiState.value.pendingDelete)
        assertFalse(vm.uiState.value.canConfirmDelete)
        assertFalse(vm.onConfirmDelete())

        vm.onUpdateDeleteConfirmText("delete")
        assertFalse(vm.uiState.value.canConfirmDelete)
        assertFalse(vm.onConfirmDelete())

        vm.onUpdateDeleteConfirmText("DELETE")
        assertTrue(vm.uiState.value.canConfirmDelete)
        assertTrue(vm.onConfirmDelete())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.pendingDelete.isEmpty())
        assertTrue(vm.uiState.value.entries.isEmpty())
    }

    @Test
    fun `hold to select (onLongPressWord) enters selection mode, auto-exits when last word unselected, and deletes with DELETE`() =
        runTest(dispatcher) {
            val repo = FakeVocabularyRepository()
            val w1 = repo.saveEntry(
                VocabularyEntry(id = "w1", wordDetails = WordDetails(word = "Alpha", meaningEnglish = "First")),
            ).getOrThrow()
            val w2 = repo.saveEntry(
                VocabularyEntry(id = "w2", wordDetails = WordDetails(word = "Bravo", meaningEnglish = "Second")),
            ).getOrThrow()
            val w3 = repo.saveEntry(
                VocabularyEntry(id = "w3", wordDetails = WordDetails(word = "Charlie", meaningEnglish = "Third")),
            ).getOrThrow()
            val vm = VocabularyManagerViewModel(repo)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isSelectionMode)

            // Holding (long-pressing) a word card enters selection mode with that word selected
            vm.onLongPressWord(w1.id)
            assertTrue(vm.uiState.value.isSelectionMode)
            assertEquals(setOf("w1"), vm.uiState.value.selectedIds)

            // Unselecting the last selected word automatically exits selection mode
            vm.onToggleSelectWord(w1.id)
            assertFalse(vm.uiState.value.isSelectionMode)
            assertTrue(vm.uiState.value.selectedIds.isEmpty())

            // Re-enter via long-press on w1 and select w2 as well
            vm.onLongPressWord(w1.id)
            vm.onToggleExpand(w1.id)
            vm.onToggleSelectWord(w2.id)
            assertEquals(setOf("w1", "w2"), vm.uiState.value.selectedIds)
            assertFalse(vm.uiState.value.allFilteredSelected)

            // Select All selects all 3; tapping w3 unselects w3 so only w1 & w2 are deleted
            vm.onSelectAllFiltered()
            assertEquals(setOf("w1", "w2", "w3"), vm.uiState.value.selectedIds)
            assertTrue(vm.uiState.value.allFilteredSelected)
            vm.onToggleSelectWord(w3.id)
            assertEquals(setOf("w1", "w2"), vm.uiState.value.selectedIds)

            vm.onRequestDeleteSelected()
            assertEquals(2, vm.uiState.value.pendingDelete.size)

            // Lowercase "delete" is rejected
            vm.onUpdateDeleteConfirmText("delete")
            assertFalse(vm.onConfirmDelete())
            assertEquals(3, vm.uiState.value.entries.size)

            // Capital "DELETE" deletes both selected words and exits selection mode
            vm.onUpdateDeleteConfirmText("DELETE")
            assertTrue(vm.onConfirmDelete())
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isSelectionMode)
            assertTrue(vm.uiState.value.selectedIds.isEmpty())
            assertTrue(vm.uiState.value.expandedIds.isEmpty())
            assertEquals(listOf(w3), vm.uiState.value.entries)
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

    private fun word(
        word: String,
        synonyms: List<String> = emptyList(),
        antonyms: List<String> = emptyList(),
    ): String {
        fun rel(items: List<String>) =
            items.joinToString(",") { """{"word": "$it", "meaningEnglish": "m"}""" }
        return """{"word": "$word", "meaningEnglish": "m", "synonyms": [${rel(synonyms)}], "antonyms": [${rel(antonyms)}]}"""
    }

    private suspend fun repoWithAbate(): FakeVocabularyRepository {
        val repo = FakeVocabularyRepository()
        repo.saveEntry(
            VocabularyEntry(
                id = "a1",
                wordDetails = WordDetails(word = "abate", meaningEnglish = "become less"),
                synonyms = listOf(WordDetails(word = "reduce", meaningEnglish = "make smaller")),
                antonyms = listOf(WordDetails(word = "increase", meaningEnglish = "grow")),
            ),
        ).getOrThrow()
        return repo
    }

    @Test
    fun `adding an existing main word asks to merge and saves nothing until confirmed with CONFIRM`() = runTest(dispatcher) {
        val repo = repoWithAbate()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartAdd()
        vm.onUpdateJsonText(word(" ABATE ", synonyms = listOf("reduce", "subside"), antonyms = listOf("intensify")))
        assertFalse(vm.onSaveEntry())
        advanceUntilIdle()

        val prompt = vm.uiState.value.mergePrompt
        assertNotNull(prompt)
        assertEquals("abate", prompt!!.existingWord)
        assertEquals(1, prompt.newSynonyms)
        assertEquals(1, prompt.newAntonyms)
        assertEquals(listOf("subside"), prompt.items.single().addedSynonyms)
        assertEquals(listOf("intensify"), prompt.items.single().addedAntonyms)
        assertNotNull(vm.uiState.value.editorState) // editor stays open behind the sheet
        assertEquals(1, vm.uiState.value.entries.size)
        assertEquals(1, vm.uiState.value.entries.single().synonyms.size)

        // Confirming without typing CONFIRM or with lowercase confirm is refused
        assertFalse(vm.onConfirmMerge())
        vm.onUpdateMergeConfirmText("confirm")
        assertFalse(vm.uiState.value.mergePrompt!!.canConfirm)
        assertFalse(vm.onConfirmMerge())
        assertEquals(1, vm.uiState.value.entries.single().synonyms.size)

        // Typing CONFIRM in capital letters approves and executes the merge
        vm.onUpdateMergeConfirmText("CONFIRM")
        assertTrue(vm.uiState.value.mergePrompt!!.canConfirm)
        assertTrue(vm.onConfirmMerge())
        advanceUntilIdle()

        assertNull(vm.uiState.value.mergePrompt)
        assertNull(vm.uiState.value.editorState)
        val merged = vm.uiState.value.entries.single()
        assertEquals("a1", merged.id)
        assertEquals(listOf("reduce", "subside"), merged.synonyms.map { it.word })
        assertEquals(listOf("increase", "intensify"), merged.antonyms.map { it.word })
        assertNotNull(vm.uiState.value.noticeMessage)
    }

    @Test
    fun `cancelling the merge keeps the editor text and changes nothing`() = runTest(dispatcher) {
        val repo = repoWithAbate()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartAdd()
        val json = word("abate", synonyms = listOf("subside"))
        vm.onUpdateJsonText(json)
        assertFalse(vm.onSaveEntry())
        vm.onDismissMerge()
        advanceUntilIdle()

        assertNull(vm.uiState.value.mergePrompt)
        assertEquals(json, vm.uiState.value.editorState!!.jsonText)
        assertEquals(1, vm.uiState.value.entries.single().synonyms.size)
    }

    @Test
    fun `adding an existing word with nothing new shows an error instead of a merge`() = runTest(dispatcher) {
        val repo = repoWithAbate()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartAdd()
        vm.onUpdateJsonText(word("abate", synonyms = listOf("reduce"), antonyms = listOf("increase")))
        assertFalse(vm.onSaveEntry())

        assertNull(vm.uiState.value.mergePrompt)
        assertTrue(vm.uiState.value.editorState!!.validationError!!.contains("already exists"))
    }

    @Test
    fun `renaming a word onto another saved word is refused`() = runTest(dispatcher) {
        val repo = repoWithAbate()
        repo.saveEntry(
            VocabularyEntry(id = "b1", wordDetails = WordDetails(word = "abet", meaningEnglish = "help")),
        ).getOrThrow()
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartEdit(vm.uiState.value.entries.first { it.id == "a1" })
        vm.onUpdateJsonText(word("abet"))
        vm.onUpdateEditConfirmText("CONFIRM")
        assertFalse(vm.onSaveEntry())

        assertTrue(vm.uiState.value.editorState!!.validationError!!.contains("already exists"))
        assertEquals(2, vm.uiState.value.entries.size)
    }

    @Test
    fun `bulk import never auto-merges repeated words in JSON and requires CONFIRM before updating existing words`() =
        runTest(dispatcher) {
            val repo = repoWithAbate()
            val vm = VocabularyManagerViewModel(repo)
            advanceUntilIdle()

            // 1. Repeated words inside the same JSON input are rejected instead of silently combined
            vm.onStartAdd()
            vm.onUpdateJsonText(
                "[" + listOf(
                    word("abandon", synonyms = listOf("desert")),
                    word("Abandon", synonyms = listOf("leave")),
                ).joinToString(",") + "]",
            )
            assertFalse(vm.onSaveEntry())
            assertNull(vm.uiState.value.mergePrompt)
            assertTrue(vm.uiState.value.editorState!!.validationError!!.contains("Duplicate word(s) in JSON input"))
            assertEquals(1, vm.uiState.value.entries.size)

            // 2. Bulk import with a new word ("abandon") and an existing word ("abate") pauses for user approval
            vm.onUpdateJsonText(
                "[" + listOf(
                    word("abandon", synonyms = listOf("desert", "leave")),
                    word("abate", synonyms = listOf("subside", "wane")),
                ).joinToString(",") + "]",
            )
            assertFalse(vm.onSaveEntry())
            advanceUntilIdle()

            // Nothing has been saved or merged yet!
            assertEquals(1, vm.uiState.value.entries.size)
            assertEquals(listOf("reduce"), vm.uiState.value.entries.single().synonyms.map { it.word })

            val prompt = vm.uiState.value.mergePrompt
            assertNotNull(prompt)
            assertEquals(1, prompt!!.newEntriesToSave.size)
            assertEquals("abandon", prompt.newEntriesToSave.single().wordDetails.word)
            assertEquals("abate", prompt.existingWord)
            assertEquals(listOf("subside", "wane"), prompt.items.single().addedSynonyms)

            // Approve with CONFIRM in capital letters
            vm.onUpdateMergeConfirmText("CONFIRM")
            assertTrue(vm.onConfirmMerge())
            advanceUntilIdle()

            assertNull(vm.uiState.value.mergePrompt)
            assertEquals(2, vm.uiState.value.entries.size)
            assertEquals(
                listOf("reduce", "subside", "wane"),
                vm.uiState.value.entries.first { it.id == "a1" }.synonyms.map { it.word },
            )
            val abandon = vm.uiState.value.entries.first {
                it.wordDetails.word.equals("abandon", ignoreCase = true)
            }
            assertEquals(listOf("desert", "leave"), abandon.synonyms.map { it.word })
            assertNotNull(vm.uiState.value.noticeMessage)
            vm.onDismissNotice()
            assertNull(vm.uiState.value.noticeMessage)
        }

    @Test
    fun `legacy duplicate database documents and AI-added database issues surface in notifications and require approval`() =
        runTest(dispatcher) {
            val repo = repoWithAbate()
            repo.saveEntry(
                VocabularyEntry(
                    id = "a2",
                    wordDetails = WordDetails(word = "Abate", meaningEnglish = "become less"),
                    synonyms = listOf(WordDetails(word = "subside", meaningEnglish = "die down")),
                ),
            ).getOrThrow()
            val vm = VocabularyManagerViewModel(repo)
            advanceUntilIdle()

            // Both entries stay untouched until the user opens notifications and chooses to review & approve
            assertEquals(2, vm.uiState.value.entries.size)
            assertEquals(listOf("abate"), vm.uiState.value.duplicateDatabaseWords)
            assertEquals(1, vm.uiState.value.pendingApprovalCount)

            vm.onOpenNotifications()
            assertTrue(vm.uiState.value.isNotificationsOpen)

            vm.onRequestMergeDatabaseDuplicates("abate")
            assertFalse(vm.uiState.value.isNotificationsOpen)
            val prompt = vm.uiState.value.mergePrompt
            assertNotNull(prompt)
            assertEquals(setOf("a2"), prompt!!.redundantDuplicateIdsToDelete)
            assertEquals(listOf("subside"), prompt.items.single().addedSynonyms)

            vm.onUpdateMergeConfirmText("CONFIRM")
            assertTrue(vm.onConfirmMerge())
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.entries.size)
            assertTrue(vm.uiState.value.duplicateDatabaseWords.isEmpty())
            assertEquals(0, vm.uiState.value.pendingApprovalCount)
            assertEquals(listOf("reduce", "subside"), vm.uiState.value.entries.single().synonyms.map { it.word })
        }

    @Test
    fun `exact duplicate database word with nothing new to merge only allows deleting duplicate so one word remains`() =
        runTest(dispatcher) {
            val repo = repoWithAbate()
            val duplicate = repo.saveEntry(
                VocabularyEntry(
                    id = "zz_dup_abate",
                    wordDetails = WordDetails(word = "abate", meaningEnglish = "become less"),
                    synonyms = listOf(WordDetails(word = "reduce", meaningEnglish = "make smaller")),
                    antonyms = listOf(WordDetails(word = "increase", meaningEnglish = "grow")),
                ),
            ).getOrThrow()
            val vm = VocabularyManagerViewModel(repo)
            advanceUntilIdle()

            assertEquals(2, vm.uiState.value.entries.size)
            val request = vm.uiState.value.pendingApprovalRequests
                .filterIsInstance<com.ravibhaiya.quizzen.ui.vocabulary.DatabaseApprovalRequest.MergeDuplicateWord>()
                .single()
            assertFalse(request.mergeItem.hasChanges)
            assertEquals(listOf(duplicate), request.redundantEntries)

            // Attempting to open a merge prompt on an exact duplicate with no changes does nothing
            vm.onRequestMergeDatabaseDuplicates("abate")
            assertNull(vm.uiState.value.mergePrompt)

            // Deleting the duplicate removes only the redundant entry and keeps the primary entry
            vm.onRequestDeleteDuplicateEntries(request.redundantEntries)
            assertEquals(listOf(duplicate), vm.uiState.value.pendingDelete)
            vm.onUpdateDeleteConfirmText("DELETE")
            assertTrue(vm.onConfirmDelete())
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.entries.size)
            assertEquals("a1", vm.uiState.value.entries.single().id)
            assertEquals(0, vm.uiState.value.pendingApprovalCount)
        }

    private class FakeVocabularyRepository(
        private val failWrites: Boolean = false,
    ) : VocabularyRepository {
        private val flow = MutableStateFlow<List<VocabularyEntry>>(emptyList())
        override val entries: Flow<List<VocabularyEntry>> = flow
        private val mistakesFlow = MutableStateFlow<Set<String>>(emptySet())
        override val mistakeWords: Flow<Set<String>> = mistakesFlow
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

        override suspend fun mergeIntoExisting(
            incoming: List<VocabularyEntry>,
        ): Result<VocabularyRules.MergeSummary> {
            val current = flow.value
            val byId = current.filter { it.id.isNotBlank() }.associateBy { it.id }
            val byKey = LinkedHashMap<String, VocabularyEntry>().apply {
                current.forEach { putIfAbsent(VocabularyRules.wordKey(it.wordDetails.word), it) }
            }
            val changed = mutableListOf<VocabularyEntry>()
            var syn = 0
            var ant = 0
            var skipped = 0
            var enriched = 0
            var limit = false
            for (entry in incoming) {
                val target = (if (entry.id.isNotBlank()) byId[entry.id] else null)
                    ?: byKey[VocabularyRules.wordKey(entry.wordDetails.word)]
                    ?: continue
                val result = VocabularyRules.mergeRelations(target, entry)
                syn += result.addedSynonyms
                ant += result.addedAntonyms
                skipped += result.skipped
                enriched += result.enrichedRelations
                limit = limit || result.limitReached
                if (result.changed) changed += result.entry
            }
            if (changed.isNotEmpty()) saveEntries(changed)
            return Result.success(VocabularyRules.MergeSummary(changed.size, syn, ant, skipped, limit, enriched))
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

        override suspend fun recordMistakeWords(wordKeys: Set<String>) {
            mistakesFlow.value = mistakesFlow.value + wordKeys
        }

        override suspend fun removeMistakeWords(wordKeys: Set<String>) {
            mistakesFlow.value = mistakesFlow.value - wordKeys
        }

        override fun clearSyncError() {
            errorFlow.value = null
        }
    }
}
