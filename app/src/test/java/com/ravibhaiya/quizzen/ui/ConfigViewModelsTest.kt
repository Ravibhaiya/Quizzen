package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.multiply.MultiplyConfigViewModel
import com.ravibhaiya.quizzen.ui.powers.PowersRootsConfigViewModel
import com.ravibhaiya.quizzen.ui.tables.TablesConfigViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelsTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    /** In-memory stand-in for the real storage. [gate], when given, holds every load until it is completed. */
    private class FakeRepository(
        var multiply: PracticeConfig.Multiply? = null,
        var tables: PracticeConfig.Tables? = null,
        var powers: PracticeConfig.PowersRoots? = null,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : QuizSettingsRepository {
        var savedMultiply: PracticeConfig.Multiply? = null
        var savedTables: PracticeConfig.Tables? = null
        var savedPowers: PracticeConfig.PowersRoots? = null

        override suspend fun loadMultiply(defaultTimer: Int): PracticeConfig.Multiply? { gate?.await(); return multiply }
        override suspend fun saveMultiply(config: PracticeConfig.Multiply) { savedMultiply = config }
        override suspend fun loadTables(defaultTimer: Int): PracticeConfig.Tables? { gate?.await(); return tables }
        override suspend fun saveTables(config: PracticeConfig.Tables) { savedTables = config }
        override suspend fun loadPowersRoots(defaultTimer: Int): PracticeConfig.PowersRoots? { gate?.await(); return powers }
        override suspend fun savePowersRoots(config: PracticeConfig.PowersRoots) { savedPowers = config }
    }

    // ---- Multiply ----

    @Test
    fun multiply_opensWithTheLastUsedSetting() = runTest(dispatcher) {
        val vm = MultiplyConfigViewModel(FakeRepository(multiply = PracticeConfig.Multiply(4, 3, 45)))
        runCurrent()
        assertEquals(4, vm.state.value.firstDigits)
        assertEquals(3, vm.state.value.secondDigits)
        assertEquals("45", vm.timer.state.value.text)
        assertTrue(vm.state.value.loaded)
    }

    @Test
    fun multiply_withNothingSaved_usesTheDefaults() = runTest(dispatcher) {
        val vm = MultiplyConfigViewModel(FakeRepository())
        runCurrent()
        assertEquals(3, vm.state.value.firstDigits)
        assertEquals(2, vm.state.value.secondDigits)
        assertEquals("20", vm.timer.state.value.text)
        assertTrue(vm.state.value.loaded)
    }

    @Test
    fun multiply_startSavesWhatWasUsed() = runTest(dispatcher) {
        val repository = FakeRepository()
        val vm = MultiplyConfigViewModel(repository)
        runCurrent()
        vm.selectFirstDigits(5)
        vm.selectSecondDigits(4)
        vm.onTimerChanged("30")

        val config = vm.startQuiz()
        runCurrent()
        assertEquals(PracticeConfig.Multiply(5, 4, 30), config)
        assertEquals(config, repository.savedMultiply)
    }

    // ---- Tables ----

    @Test
    fun tables_opensWithTheLastUsedSetting_andSavesOnStart() = runTest(dispatcher) {
        val repository = FakeRepository(tables = PracticeConfig.Tables(listOf(2, 7), 12))
        val vm = TablesConfigViewModel(repository)
        runCurrent()
        assertEquals(setOf(2, 7), vm.state.value.selected)
        assertEquals("12", vm.timer.state.value.text)

        vm.toggle(9)
        val config = vm.startQuiz()
        runCurrent()
        assertEquals(PracticeConfig.Tables(listOf(2, 7, 9), 12), config)
        assertEquals(config, repository.savedTables)
    }

    @Test
    fun tables_withNothingSaved_startsWithNothingSelected() = runTest(dispatcher) {
        val vm = TablesConfigViewModel(FakeRepository())
        runCurrent()
        assertTrue(vm.state.value.selected.isEmpty())
        assertEquals("10", vm.timer.state.value.text)
    }

    // ---- Powers & Roots ----

    @Test
    fun powersRoots_opensWithTheLastUsedSetting_andSavesOnStart() = runTest(dispatcher) {
        val saved = PracticeConfig.PowersRoots(
            types = setOf(PowerRootType.Cubes, PowerRootType.SquareRoots),
            squares = 3..25,
            cubes = 4..18,
            timerSeconds = 15,
        )
        val repository = FakeRepository(powers = saved)
        val vm = PowersRootsConfigViewModel(repository)
        runCurrent()
        assertEquals(saved.types, vm.state.value.types)
        assertEquals(3..25, vm.state.value.squares)
        assertEquals(4..18, vm.state.value.cubes)
        assertEquals("15", vm.timer.state.value.text)

        vm.onRangeChanged(PowerRootType.Family.Cube, 5..12)
        val config = vm.startQuiz()
        runCurrent()
        assertEquals(saved.copy(cubes = 5..12), config)
        assertEquals(config, repository.savedPowers)
    }

    @Test
    fun powersRoots_withNothingSaved_usesTheDefaults() = runTest(dispatcher) {
        val vm = PowersRootsConfigViewModel(FakeRepository())
        runCurrent()
        assertTrue(vm.state.value.types.isEmpty())
        assertEquals(2..30, vm.state.value.squares)
        assertEquals(2..20, vm.state.value.cubes)
        assertEquals("10", vm.timer.state.value.text)
    }

    // ---- loading ----

    @Test
    fun screenIsNotLoadedUntilTheSavedSettingIsRead() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = TablesConfigViewModel(FakeRepository(tables = PracticeConfig.Tables(listOf(4), 15), gate = gate))
        runCurrent()
        assertFalse(vm.state.value.loaded)

        gate.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.loaded)
        assertEquals(setOf(4), vm.state.value.selected)
    }

    @Test
    fun whatTheUserChangedWhileLoading_winsOverTheSavedSetting() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val saved = PracticeConfig.PowersRoots(setOf(PowerRootType.Cubes), 3..25, 4..18, 15)
        val vm = PowersRootsConfigViewModel(FakeRepository(powers = saved, gate = gate))
        runCurrent()

        vm.toggleType(PowerRootType.Squares) // the user is faster than the storage
        gate.complete(Unit)
        runCurrent()

        assertEquals(setOf(PowerRootType.Squares), vm.state.value.types)
        assertEquals(2..30, vm.state.value.squares) // the saved ranges were not applied either
        assertTrue(vm.state.value.loaded)
    }
}
