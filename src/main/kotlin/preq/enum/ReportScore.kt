package preq.enum

enum class ReportScore {
    VALID,
    PENDING_REVIEW,
    INVALID,
    ;

    companion object {
        private const val VALID_THRESHOLD = 0.7
        private const val PENDING_THRESHOLD = 0.4

        fun fromScore(score: Double): ReportScore =
            when {
                score >= VALID_THRESHOLD -> VALID
                score >= PENDING_THRESHOLD -> PENDING_REVIEW
                else -> INVALID
            }

        const val VALID_MIN = VALID_THRESHOLD
        const val PENDING_MIN = PENDING_THRESHOLD
    }
}
