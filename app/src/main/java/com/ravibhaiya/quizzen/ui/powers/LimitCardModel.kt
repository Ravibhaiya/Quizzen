package com.ravibhaiya.quizzen.ui.powers

import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules

/** What a limit card shows. Pure data (no Compose), so the rules can be unit tested. */
data class LimitCardModel(
    val family: PowerRootType.Family,
    /** Largest number this kind can use. */
    val limit: Int,
    val status: Status,
    /** The numbers that will really be used; only for [Status.Using]. */
    val used: IntRange?,
    /** True when the chosen Max is above [limit] and was cut down to it. */
    val cut: Boolean,
    /** True when other kinds are selected but not this one, so the card is shown faded. */
    val dimmed: Boolean,
) {
    enum class Status {
        /** Nothing to compute yet (no kind selected, or the range is not valid): only the limit is shown. */
        Idle,

        /** Selected and the range has numbers for this kind. */
        Using,

        /** Selected, but the range has no numbers for this kind (e.g. Min 25 for cubes). */
        NoNumbers,
    }
}

/** The two cards shown under the Min/Max fields: squares & roots, then cubes & roots. */
fun limitCardModels(types: Set<PowerRootType>, min: Int?, max: Int?): List<LimitCardModel> {
    val rangeIsValid = min != null && max != null &&
        min in PowersRootsRules.MIN_ALLOWED..PowersRootsRules.MAX_ALLOWED &&
        max in PowersRootsRules.MIN_ALLOWED..PowersRootsRules.MAX_ALLOWED &&
        min <= max

    return listOf(PowerRootType.Family.Square, PowerRootType.Family.Cube).map { family ->
        val representative = if (family == PowerRootType.Family.Square) PowerRootType.Squares else PowerRootType.Cubes
        val selected = types.any { it.family == family }
        val dimmed = types.isNotEmpty() && !selected
        if (!selected || !rangeIsValid) {
            return@map LimitCardModel(family, representative.limit, LimitCardModel.Status.Idle, null, cut = false, dimmed = dimmed)
        }
        val used = PowersRootsRules.effectiveRange(representative, min!!, max!!)
        LimitCardModel(
            family = family,
            limit = representative.limit,
            status = if (used == null) LimitCardModel.Status.NoNumbers else LimitCardModel.Status.Using,
            used = used,
            cut = used != null && max > representative.limit,
            dimmed = false,
        )
    }
}
