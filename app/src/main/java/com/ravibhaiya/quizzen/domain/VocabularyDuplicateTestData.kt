package com.ravibhaiya.quizzen.domain

/**
 * 25 duplicate test entries matching the 25 starter words in [VocabularyFallbackData.ENTRIES]:
 * - 12 entries are 100% identical to their starter counterpart (only the document [VocabularyEntry.id] differs).
 * - 13 entries share the same main word as their starter counterpart but include additional synonyms and antonyms
 *   so they can be reviewed and merged from the Database Manager's Approval Requests notification sheet.
 */
internal object VocabularyDuplicateTestData {

    private data class ExtraRelations(
        val extraSynonyms: List<WordDetails> = emptyList(),
        val extraAntonyms: List<WordDetails> = emptyList(),
    )

    private val DIFFERENCES_BY_WORD: Map<String, ExtraRelations> = mapOf(
        "candid" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Sincere",
                    meaningEnglish = "Free from pretense or deceit; proceeding from genuine feeling",
                    meaningHinglish = "Sachcha aur dil se bolne wala",
                    exampleSentence = "She offered a sincere apology to her friend.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Guarded",
                    meaningEnglish = "Cautious and having possible reservations",
                    meaningHinglish = "Satark aur baat chhupakar bolne wala",
                    exampleSentence = "The spokesperson gave a guarded response to the press.",
                ),
            ),
        ),
        "diligent" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Sedulous",
                    meaningEnglish = "Showing dedication and diligence",
                    meaningHinglish = "Lagatar mehnat karne wala",
                    exampleSentence = "He paid sedulous attention to every client request.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Slothful",
                    meaningEnglish = "Lazy; sluggish",
                    meaningHinglish = "Sust aur aalsi",
                    exampleSentence = "Slothful habits slowed down the entire project.",
                ),
            ),
        ),
        "lucid" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Pellucid",
                    meaningEnglish = "Translucently clear; easily understood",
                    meaningHinglish = "Bilkul saaf aur spasht",
                    exampleSentence = "She writes in a pellucid prose style.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Muddled",
                    meaningEnglish = "In a state of bewildered or bewildering confusion or disorder",
                    meaningHinglish = "Uljha hua aur aspasht",
                    exampleSentence = "The speaker presented a muddled explanation.",
                ),
            ),
        ),
        "resilient" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Tough",
                    meaningEnglish = "Able to endure hardship or pain",
                    meaningHinglish = "Kathin paristhiti jhelne mein saksham",
                    exampleSentence = "Desert plants are tough enough to survive dry spells.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Brittle",
                    meaningEnglish = "Hard but liable to break or shatter easily",
                    meaningHinglish = "Jaldi tootne wala",
                    exampleSentence = "Dry branches become brittle in winter.",
                ),
            ),
        ),
        "meticulous" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Thorough",
                    meaningEnglish = "Performed or written with great care and completeness",
                    meaningHinglish = "Poori bariki se kiya gaya",
                    exampleSentence = "The detectives conducted a thorough search of the room.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Haphazard",
                    meaningEnglish = "Lacking any obvious principle of organization",
                    meaningHinglish = "Bina yojana ya dhang ke",
                    exampleSentence = "Books were piled in a haphazard manner on the shelf.",
                ),
            ),
        ),
        "tenacious" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Stalwart",
                    meaningEnglish = "Loyal, reliable, and hardworking",
                    meaningHinglish = "Majboot aur dridh",
                    exampleSentence = "He remained a stalwart supporter of the team.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Wavering",
                    meaningEnglish = "Moving back and forth; undecided or unsteady",
                    meaningHinglish = "Dagmagane wala ya asthir",
                    exampleSentence = "Her wavering commitment worried the organizers.",
                ),
            ),
        ),
        "gregarious" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Affable",
                    meaningEnglish = "Friendly, good-natured, or easy to talk to",
                    meaningHinglish = "Milansaar aur vinamra",
                    exampleSentence = "The affable shopkeeper greeted every customer warmly.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Introverted",
                    meaningEnglish = "Shy, reticent, and preferring solitary activities",
                    meaningHinglish = "Antarmukhi ya kam bolne wala",
                    exampleSentence = "The introverted artist preferred working alone.",
                ),
            ),
        ),
        "ostentatious" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Gaudy",
                    meaningEnglish = "Extravagantly bright or showy, typically so as to be tasteless",
                    meaningHinglish = "Chamak-damak aur dikhawe wala",
                    exampleSentence = "The signboard was painted in gaudy neon colors.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Reserved",
                    meaningEnglish = "Slow to reveal emotion or opinions; restrained",
                    meaningHinglish = "Sanyamit aur saadgi pasand",
                    exampleSentence = "He kept a reserved demeanor at the formal dinner.",
                ),
            ),
        ),
        "scrupulous" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Upright",
                    meaningEnglish = "Strictly honorable or honest",
                    meaningHinglish = "Nek aur imaandaar",
                    exampleSentence = "Everyone trusted the upright judge.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Corrupt",
                    meaningEnglish = "Having or showing a willingness to act dishonestly",
                    meaningHinglish = "Bhrasht aur beimaan",
                    exampleSentence = "Corrupt dealings were exposed by the audit.",
                ),
            ),
        ),
        "verbose" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Wordy",
                    meaningEnglish = "Using or expressed in too many words",
                    meaningHinglish = "Lamba-chauda aur shabdon se bhara",
                    exampleSentence = "The editor trimmed the wordy introduction.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Terse",
                    meaningEnglish = "Sparing in the use of words; abrupt",
                    meaningHinglish = "Toote-phoote ya bahut kam shabdon mein",
                    exampleSentence = "He issued a terse statement after the meeting.",
                ),
            ),
        ),
        "amicable" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Genial",
                    meaningEnglish = "Friendly and cheerful",
                    meaningHinglish = "Hasmukh aur dostana",
                    exampleSentence = "Our genial host made us feel right at home.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Quarrelsome",
                    meaningEnglish = "Given to or characterized by quarreling",
                    meaningHinglish = "Jhagralu swabhav ka",
                    exampleSentence = "A quarrelsome attitude disrupts team harmony.",
                ),
            ),
        ),
        "immutable" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Changeless",
                    meaningEnglish = "Remaining the same; unchanging",
                    meaningHinglish = "Jo kabhi na badle",
                    exampleSentence = "The desert landscape appeared timeless and changeless.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Fickle",
                    meaningEnglish = "Changing frequently, especially as regards one's loyalties or affections",
                    meaningHinglish = "Pal-pal badalne wala",
                    exampleSentence = "Mountain weather is notoriously fickle.",
                ),
            ),
        ),
        "sagacious" to ExtraRelations(
            extraSynonyms = listOf(
                WordDetails(
                    word = "Discerning",
                    meaningEnglish = "Having or showing good judgment",
                    meaningHinglish = "Sahi-galat ki parakh rakhne wala",
                    exampleSentence = "Discerning readers appreciated the subtle humor.",
                ),
            ),
            extraAntonyms = listOf(
                WordDetails(
                    word = "Shortsighted",
                    meaningEnglish = "Lacking imagination or foresight",
                    meaningHinglish = "Aage ki na sochne wala",
                    exampleSentence = "Cutting the research budget was a shortsighted move.",
                ),
            ),
        ),
    )

    /**
     * 25 duplicate entries corresponding 1-to-1 with [VocabularyFallbackData.ENTRIES]:
     * - 12 are 100% identical (only [VocabularyEntry.id] is `zz_dup_<word>`).
     * - 13 have extra synonyms and antonyms (`DIFFERENCES_BY_WORD`).
     */
    val ENTRIES: List<VocabularyEntry> = VocabularyFallbackData.ENTRIES.map { base ->
        val key = VocabularyRules.wordKey(base.wordDetails.word)
        val dupId = "zz_dup_${base.id.removePrefix("seed_")}"
        val diff = DIFFERENCES_BY_WORD[key]
        if (diff == null) {
            base.copy(id = dupId)
        } else {
            base.copy(
                id = dupId,
                synonyms = base.synonyms + diff.extraSynonyms,
                antonyms = base.antonyms + diff.extraAntonyms,
            )
        }
    }
}
