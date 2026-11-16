package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.DecisionPendingRetryException
import com.ravibhaiya.quizzen.data.VocabularyDecisionClient
import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.DecisionDuplicateKind
import com.ravibhaiya.quizzen.domain.VocabularyDecisionFinding
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyRules
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.vocabulary.DatabaseApprovalRequest
import com.ravibhaiya.quizzen.ui.vocabulary.FocusedRelationAction
import com.ravibhaiya.quizzen.ui.vocabulary.VocabularyManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyManagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `edit word keeps Save Word inactive until CONFIRM is typed and updates partOfSpeech`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepository()
        val entry = repo.saveEntry(
            VocabularyEntry(
                id = "w1",
                wordDetails = WordDetails(
                    word = "Repudiate",
                    partOfSpeech = "Noun",
                    meaningEnglish = "Refuse to accept",
                ),
                synonyms = listOf(WordDetails(word = "Reject", partOfSpeech = "", meaningEnglish = "Dismiss")),
            ),
        ).getOrThrow()

        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartEdit(entry)
        val initialEditor = vm.uiState.value.editorState!!
        assertTrue(initialEditor.isEditing)
        assertFalse(initialEditor.canSave)
        // Both main word and synonym JSON include "partOfSpeech"
        assertEquals(2, "\"partOfSpeech\":".toRegex().findAll(initialEditor.jsonText).count())

        // Editing JSON alone does NOT activate canSave until CONFIRM is typed
        val updatedJson = initialEditor.jsonText
            .replace("\"partOfSpeech\": \"Noun\"", "\"partOfSpeech\": \"Verb\"")
            .replace("\"partOfSpeech\": \"\"", "\"partOfSpeech\": \"Verb\"")
        vm.onUpdateJsonText(updatedJson)
        assertFalse(vm.uiState.value.editorState!!.canSave)
        assertFalse(vm.onSaveEntry())

        // Typing lowercase "confirm" still keeps canSave false
        vm.onUpdateEditConfirmText("confirm")
        assertFalse(vm.uiState.value.editorState!!.canSave)

        // Typing "CONFIRM" activates canSave and saves the updated partOfSpeech
        vm.onUpdateEditConfirmText("CONFIRM")
        assertTrue(vm.uiState.value.editorState!!.canSave)
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        assertNull(vm.uiState.value.editorState)
        val saved = vm.uiState.value.entries.single()
        assertEquals("Verb", saved.wordDetails.partOfSpeech)
        assertEquals("Verb", saved.synonyms.single().partOfSpeech)
    }

    @Test
    fun `same word with different partOfSpeech saves separately while same partOfSpeech requires merge approval`() =
        runTest(dispatcher) {
            val repo = FakeVocabularyRepository()
            val vm = VocabularyManagerViewModel(repo)
            advanceUntilIdle()

            // 1. Add "Present" (Noun) and "Present" (Verb) in the same JSON array -> saved as 2 separate entries
            vm.onStartAdd()
            vm.onUpdateJsonText(
                """
                [
                  {
                    "word": "Present",
                    "partOfSpeech": "Noun",
                    "meaningEnglish": "A gift",
                    "synonyms": [{"word": "Gift", "partOfSpeech": "Noun", "meaningEnglish": "Something given"}]
                  },
                  {
                    "word": "Present",
                    "partOfSpeech": "Verb",
                    "meaningEnglish": "Give formally",
                    "synonyms": [{"word": "Award", "partOfSpeech": "Verb", "meaningEnglish": "Grant officially"}]
                  }
                ]
                """.trimIndent(),
            )
            assertTrue(vm.onSaveEntry())
            advanceUntilIdle()

            assertNull(vm.uiState.value.mergePrompt)
            assertEquals(2, vm.uiState.value.entries.size)
            assertEquals(setOf("Noun", "Verb"), vm.uiState.value.entries.map { it.wordDetails.partOfSpeech }.toSet())

            // 2. Adding another "Present" with the SAME partOfSpeech ("Verb") and a new synonym opens MergePrompt
            vm.onStartAdd()
            vm.onUpdateJsonText(
                """
                {
                  "word": "Present",
                  "partOfSpeech": "Verb",
                  "meaningEnglish": "Give formally",
                  "synonyms": [{"word": "Bestow", "partOfSpeech": "Verb", "meaningEnglish": "Confer as a gift"}]
                }
                """.trimIndent(),
            )
            assertFalse(vm.onSaveEntry())
            advanceUntilIdle()

            val prompt = vm.uiState.value.mergePrompt
            assertNotNull(prompt)
            assertEquals(listOf("Bestow"), prompt!!.items.single().addedSynonyms)

            vm.onUpdateMergeConfirmText("CONFIRM")
            assertTrue(vm.onConfirmMerge())
            advanceUntilIdle()

            assertEquals(2, vm.uiState.value.entries.size)
            val verbEntry = vm.uiState.value.entries.first { it.wordDetails.partOfSpeech == "Verb" }
            assertEquals(listOf("Award", "Bestow"), verbEntry.synonyms.map { it.word })
        }

    @Test
    fun `importing JSON file strips UTF-8 BOM, validates syntax, and deleting all words clears saved quiz session`() =
        runTest(dispatcher) {
            val repo = FakeVocabularyRepository()
            val sessionStore = FakeVocabularySessionStore(text = "active_unfinished_session")
            val vm = VocabularyManagerViewModel(repo, sessionStore = sessionStore)
            advanceUntilIdle()

            vm.onStartAdd()
            vm.onImportJsonFile("   ")
            assertNotNull(vm.uiState.value.editorState!!.validationError)

            val bomJson = "\uFEFF" + """[{"word": "Lucid", "partOfSpeech": "Adjective", "meaningEnglish": "Clear"}]"""
            vm.onImportJsonFile(bomJson)
            assertNull(vm.uiState.value.editorState!!.validationError)
            assertTrue(vm.onSaveEntry())
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.entries.size)
            assertEquals("active_unfinished_session", sessionStore.text)

            // Deleting all words from database clears the saved unfinished quiz session
            vm.onRequestDelete(vm.uiState.value.entries.single())
            vm.onUpdateDeleteConfirmText("DELETE")
            assertTrue(vm.onConfirmDelete())
            advanceUntilIdle()

            assertTrue(vm.uiState.value.entries.isEmpty())
            assertNull(sessionStore.text)
        }

    @Test
    fun `AI partOfSpeech conflict opens focused relation editor and rate limit retries after 60 seconds`() =
        runTest(dispatcher) {
            val repo = FakeVocabularyRepository()
            val badAntonym = WordDetails(
                word = "Embrace",
                partOfSpeech = "Adjective",
                meaningEnglish = "Accept willingly",
                exampleSentence = "We embrace new ideas.",
            )
            val repudiate = repo.saveEntry(
                VocabularyEntry(
                    id = "word_repudiate",
                    wordDetails = WordDetails(
                        word = "Repudiate",
                        partOfSpeech = "Verb",
                        meaningEnglish = "Refuse to accept",
                    ),
                    antonyms = listOf(badAntonym),
                ),
            ).getOrThrow()

            var attempts = 0
            val fakeDecision = object : VocabularyDecisionClient {
                override val isConfigured: Boolean = true
                override suspend fun analyze(
                    entries: List<VocabularyEntry>,
                    dismissedKeys: Set<String>,
                ): Result<List<VocabularyDecisionFinding>> {
                    attempts++
                    if (attempts == 1) {
                        return Result.failure(DecisionPendingRetryException("Rate limit", retrySeconds = 60))
                    }
                    return Result.success(
                        listOf(
                            VocabularyDecisionFinding.MeaningConflict(
                                entryId = "word_repudiate",
                                probability = 0.97,
                                confidence = 0.98,
                                conflictingTarget = VocabularyRules.ConflictingRelationTarget(
                                    isAntonym = true,
                                    index = 0,
                                    details = badAntonym,
                                    conflictingWordLabel = "Antonym \"Embrace\" part of speech",
                                    reason = "Wrong part of speech \"Adjective\" for antonym \"Embrace\".",
                                ),
                            ),
                        ),
                    )
                }
            }

            val vm = VocabularyManagerViewModel(repo, fakeDecision)
            runCurrent()

            assertTrue(vm.uiState.value.isDecisionPendingRetry)
            assertEquals(60, vm.uiState.value.decisionRetryRemainingSeconds)

            advanceTimeBy(60_000L)
            runCurrent()

            assertFalse(vm.uiState.value.isDecisionPendingRetry)
            assertEquals(1, vm.uiState.value.pendingApprovalCount)
            val issue = vm.uiState.value.pendingApprovalRequests
                .filterIsInstance<DatabaseApprovalRequest.EntryIssue>()
                .single()
            assertEquals("Antonym \"Embrace\" part of speech", issue.conflictingWord)

            // Clicking Edit JSON opens focused relation editor with "partOfSpeech": "Adjective"
            vm.onStartEditIssue(repudiate)
            val editor = vm.uiState.value.editorState!!
            assertTrue(editor.isFocusedWordMode)
            assertEquals(FocusedRelationAction.EDIT_WORD, editor.focusedRelation!!.action)
            assertTrue(editor.jsonText.contains("\"partOfSpeech\": \"Adjective\""))

            vm.onUpdateJsonText(editor.jsonText.replace("\"Adjective\"", "\"Verb\""))
            vm.onUpdateEditConfirmText("CONFIRM")
            assertTrue(vm.onSaveEntry())
            advanceUntilIdle()

            assertEquals("Verb", vm.uiState.value.entries.single().antonyms.single().partOfSpeech)
        }

    @Test
    fun `write failures keep editor open and preserve typed JSON`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepository(failWrites = true)
        val vm = VocabularyManagerViewModel(repo)
        advanceUntilIdle()

        vm.onStartAdd()
        val typedJson = """[{"word": "Audacious", "partOfSpeech": "Adjective", "meaningEnglish": "Bold"}]"""
        vm.onUpdateJsonText(typedJson)
        assertTrue(vm.onSaveEntry())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.entries.isEmpty())
        val editor = vm.uiState.value.editorState
        assertNotNull(editor)
        assertEquals(typedJson, editor!!.jsonText)
        assertNotNull(editor.validationError)
    }

    @Test
    fun `buildDatabaseApprovalRequests ignores duplicate findings when entries have different parts of speech`() {
        val nounEntry = VocabularyEntry(
            id = "p_noun",
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Noun", meaningEnglish = "A gift"),
        )
        val verbEntry = VocabularyEntry(
            id = "p_verb",
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Verb", meaningEnglish = "Give formally"),
        )
        val findings = listOf(
            VocabularyDecisionFinding.DuplicateOrVariant(
                primaryId = "p_noun",
                duplicateId = "p_verb",
                kind = DecisionDuplicateKind.EXACT_DUPLICATE,
                probability = 0.99,
                confidence = 0.99,
            ),
        )

        val requests = com.ravibhaiya.quizzen.ui.vocabulary.buildDatabaseApprovalRequests(
            entries = listOf(nounEntry, verbEntry),
            decisionFindings = findings,
        )
        assertTrue(requests.isEmpty())
    }

    private class FakeVocabularyRepository(
        var failWrites: Boolean = false,
        var failMergeOnly: Boolean = false,
    ) : VocabularyRepository {
        private val flow = MutableStateFlow<List<VocabularyEntry>>(emptyList())
        override val entries: Flow<List<VocabularyEntry>> = flow
        private val mistakesFlow = MutableStateFlow<Set<String>>(emptySet())
        override val mistakeWords: Flow<Set<String>> = mistakesFlow
        private val errorFlow = MutableStateFlow<String?>(null)
        override val syncError: Flow<String?> = errorFlow

        override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> =
            saveEntries(listOf(entry)).map { it.first() }

        override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> {
            if (failWrites) return Result.failure(IOException("No internet connection. Nothing was saved."))
            for (entry in entries) {
                val lengthError = VocabularyRules.validateFieldLengths(entry)
                if (lengthError != null) return Result.failure(IllegalArgumentException(lengthError))
            }
            val prepared = VocabularyRules.prepareEntriesForSave(
                entries = entries,
                existing = flow.value,
                now = System.currentTimeMillis(),
            )
            val savedList = prepared.entries
            val savedIds = savedList.map { it.id }.toSet()
            flow.value = flow.value.filterNot { it.id in savedIds } + savedList
            return Result.success(savedList)
        }

        override suspend fun mergeIntoExisting(
            incoming: List<VocabularyEntry>,
        ): Result<VocabularyRules.MergeSummary> {
            if (failWrites || failMergeOnly) return Result.failure(IOException("No internet connection. Nothing was saved."))
            val current = flow.value
            val byId = current.filter { it.id.isNotBlank() }.associateBy { it.id }
            val byIdentityKey = LinkedHashMap<String, VocabularyEntry>().apply {
                current.forEach { putIfAbsent(VocabularyRules.wordIdentityKey(it.wordDetails), it) }
            }
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
                    ?: byIdentityKey[VocabularyRules.wordIdentityKey(entry.wordDetails)]
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
            if (failWrites) return Result.failure(IOException("No internet connection. Nothing was saved."))
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
