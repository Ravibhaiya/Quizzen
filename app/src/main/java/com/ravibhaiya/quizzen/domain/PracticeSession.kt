package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

/**
 * Decides which question comes next in one practice session, and brings mistakes back.
 *
 * Quizzes with a limited set of questions (Tables, Powers & Roots) are asked in **rounds**: all their questions are shuffled
 * into a random order and asked one after the other, so every question comes up once before any question comes up again.
 * When a round is finished the questions are shuffled again and the quiz simply goes on, forever. Multiply has far too many
 * possible questions to list, so each question is drawn at random.
 *
 * Mistakes are added to that order (only for limited quizzes, see [PracticeConfig.repeatsMistakes]):
 *  - a wrong answer (or a time-out) makes the same question come back [WRONG_REPEATS] times,
 *  - a correct but slow answer makes it come back [SLOW_REPEATS] times,
 *  - every comeback lands somewhere in the next [WINDOW] questions, never as the very next one, and two comebacks of the
 *    same question are never next to each other,
 *  - a fresh question is never the same as the question just before it or as a comeback due right after it.
 *
 * Example: you get 17 squared wrong on question 4. It then returns three times among questions 6 to 14, e.g. at 6, 9 and 13.
 *
 * Everything lives in this object, so closing the quiz (or starting a new one) starts from nothing. Not thread-safe; used
 * from the main thread only.
 *
 * Call [next] to get the first question, then after every answer call [report] for the question that was on screen and
 * [next] again.
 */
class PracticeSession(
    private val generator: QuestionGenerator,
    private val config: PracticeConfig,
    private val random: Random = Random.Default,
    /** Every question of the quiz (see [QuestionPool]), or null to draw each question at random from [generator]. */
    private val questions: List<Question>? = QuestionPool.of(config),
) {
    /** The questions still to be asked in the current round, in the order they will come. */
    private val round = ArrayDeque<Question>()

    /** Number of the question on screen: 1 for the first, 0 before the first. */
    private var position = 0
    private var previous: Question? = null

    /** Comebacks that are due: question number -> question. */
    private val scheduled = HashMap<Int, Question>()

    fun next(): Question {
        position++
        val question = scheduled.remove(position) ?: freshQuestion()
        previous = question
        return question
    }

    /** Reports how [question] (the one currently on screen) was answered. */
    fun report(question: Question, outcome: AnswerOutcome) {
        if (!config.repeatsMistakes) return
        val repeats = when (outcome) {
            AnswerOutcome.Wrong -> WRONG_REPEATS
            AnswerOutcome.Slow -> SLOW_REPEATS
            AnswerOutcome.Fast -> return
        }
        // A new mistake replaces the comebacks that were still waiting for the same question.
        scheduled.values.removeAll { it == question }
        schedule(question, repeats)
    }

    private fun freshQuestion(): Question {
        val blocked = setOfNotNull(previous, scheduled[position + 1])
        return if (questions == null) drawAtRandom(blocked) else nextOfRound(questions, blocked)
    }

    private fun drawAtRandom(blocked: Set<Question>): Question {
        var candidate = generator.next(config)
        var attempts = 0
        // With a single possible question (or two, both blocked) there is nothing else to pick: give up after a while.
        while (candidate in blocked && attempts < MAX_REDRAWS) {
            candidate = generator.next(config)
            attempts++
        }
        return candidate
    }

    private fun nextOfRound(all: List<Question>, blocked: Set<Question>): Question {
        if (round.isEmpty()) round.addAll(all.shuffled(random))
        takeUnblocked(blocked)?.let { return it }
        // Only blocked questions are left in this round (they are being asked right now anyway): start the next round early.
        round.clear()
        round.addAll(all.shuffled(random))
        takeUnblocked(blocked)?.let { return it }
        // Tiny quiz (one or two questions): nothing else exists, so an immediate repeat cannot be avoided.
        return round.removeFirst()
    }

    /** The first question of the round that is not [blocked]; blocked ones move to the end so they are still asked later. */
    private fun takeUnblocked(blocked: Set<Question>): Question? {
        repeat(round.size) {
            val question = round.removeFirst()
            if (question !in blocked) return question
            round.addLast(question)
        }
        return null
    }

    private fun schedule(question: Question, repeats: Int) {
        val windowStart = position + 2 // position + 1 would be "immediately"
        val windowEnd = position + WINDOW
        val free = (windowStart..windowEnd).filter { it !in scheduled }
        val chosen = pickSpreadOut(free, repeats)
        chosen.forEach { scheduled[it] = question }

        // Only when many mistakes pile up is the window full; the rest then comes back just after it instead of being lost.
        var missing = repeats - chosen.size
        var slot = windowEnd + 1
        while (missing > 0) {
            if (slot !in scheduled && scheduled[slot - 1] != question && scheduled[slot + 1] != question) {
                scheduled[slot] = question
                missing--
            }
            slot++
        }
    }

    /** A random choice of up to [count] of the [free] numbers with no two of them next to each other (as many as fit). */
    private fun pickSpreadOut(free: List<Int>, count: Int): List<Int> {
        for (size in count downTo 1) {
            val options = combinations(free, size).filter { combo -> combo.zipWithNext().all { (a, b) -> b - a >= 2 } }
            if (options.isNotEmpty()) return options.random(random)
        }
        return emptyList()
    }

    private fun combinations(items: List<Int>, size: Int): List<List<Int>> = when {
        size == 0 -> listOf(emptyList())
        items.size < size -> emptyList()
        else -> {
            val head = items.first()
            val tail = items.drop(1)
            combinations(tail, size - 1).map { listOf(head) + it } + combinations(tail, size)
        }
    }

    companion object {
        const val WRONG_REPEATS = 3
        const val SLOW_REPEATS = 2

        /** Comebacks land among the next this-many questions (but never the very next one). */
        const val WINDOW = 10

        private const val MAX_REDRAWS = 30
    }
}
