package com.ravibhaiya.quizzen.ui.components

/**
 * Largest font size on the grid `maxSize, maxSize - step, ...` (never below [minSize]) whose text width is at most
 * [availableWidth]. [widthAt] returns the text width for a font size and must not shrink as the size grows.
 *
 * Binary search, so a long text costs about log2(steps) text measurements in one pass instead of one relayout per step
 * (the previous approach re-laid-out the screen up to ~20 times, which janked on low-end phones). Pure Kotlin so it is
 * unit tested on the JVM.
 */
internal fun fitFontSize(
    maxSize: Float,
    minSize: Float,
    step: Float,
    availableWidth: Float,
    widthAt: (Float) -> Float,
): Float {
    require(step > 0f && maxSize >= minSize) { "invalid font size range" }
    fun sizeAt(index: Int) = (maxSize - index * step).coerceAtLeast(minSize)
    fun fits(index: Int) = widthAt(sizeAt(index)) <= availableWidth

    if (fits(0)) return maxSize
    val lastIndex = ((maxSize - minSize) / step).toInt()
    if (lastIndex == 0 || !fits(lastIndex)) return minSize

    var tooBig = 0 // known not to fit
    var fitting = lastIndex // known to fit
    while (fitting - tooBig > 1) {
        val middle = (tooBig + fitting) / 2
        if (fits(middle)) fitting = middle else tooBig = middle
    }
    return sizeAt(fitting)
}
