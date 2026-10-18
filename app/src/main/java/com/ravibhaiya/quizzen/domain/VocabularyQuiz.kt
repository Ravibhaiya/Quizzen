package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

/**
 * The two relationship modes offered in the Vocabulary quiz.
 * One or both modes may be selected at the same time.
 */
enum class VocabularyQuizMode(val code: String) {
    Synonyms("syn"),
    Antonyms("ant"),
    ;

    companion object {
        val DEFAULT: Set<VocabularyQuizMode> = setOf(Synonyms, Antonyms)

        fun fromCode(code: String?): VocabularyQuizMode? = entries.firstOrNull { it.code == code }

        /** Encodes selected modes in a deterministic order (`syn`, `ant`, or `syn-ant`). */
        fun encode(modes: Set<VocabularyQuizMode>): String =
            entries.filter { it in modes }.joinToString("-") { it.code }

        /** Decodes modes from text; unknown codes are ignored. */
        fun decode(text: String?): Set<VocabularyQuizMode> =
            text.orEmpty().split('-', ',').mapNotNull(::fromCode).toSet()
    }
}

/**
 * Configuration for a Vocabulary quiz session.
 * Persisted via `SettingsCodec` and passed through navigation arguments.
 */
data class VocabularyQuizConfig(
    val modes: Set<VocabularyQuizMode> = VocabularyQuizMode.DEFAULT,
    val wordCount: Int = VocabularyQuizRules.DEFAULT_WORDS,
    val timerSeconds: Int = VocabularyQuizRules.DEFAULT_TIMER_SECONDS,
) {
    init {
        require(modes.isNotEmpty()) { "at least one vocabulary quiz mode must be chosen" }
    }
}

/**
 * A single 4-option multiple-choice question in a finite Vocabulary quiz.
 *
 * Neither [promptWord] nor [correctAnswer] is restricted to a main headword:
 * - In [VocabularyQuizMode.Synonyms], two synonyms of the same word cluster can test each other directly
 *   (`Synonym A -> Synonym B`), or pair with the main word in either direction.
 * - In [VocabularyQuizMode.Antonyms], an antonym can pair with a synonym or the main word in either direction.
 */
data class VocabularyQuizItem(
    val clusterId: String,
    val mode: VocabularyQuizMode,
    val promptWord: String,
    val correctAnswer: String,
    val options: List<String>,
    val promptDetails: WordDetails = WordDetails(word = promptWord),
    val optionDetails: List<WordDetails> = options.map { WordDetails(word = it) },
    val clusterWord: String = "",
) {
    fun isCorrect(selectedOption: String): Boolean =
        selectedOption.trim().equals(correctAnswer.trim(), ignoreCase = true)

    fun detailsForOption(option: String): WordDetails =
        optionDetails.firstOrNull { it.word.equals(option.trim(), ignoreCase = true) }
            ?: WordDetails(word = option)
}

/**
 * Rules and bounds for Vocabulary quiz configuration and study word selection.
 */
object VocabularyQuizRules {
    const val MIN_WORDS = 5
    const val MAX_WORDS = 25
    const val DEFAULT_WORDS = 10
    const val DEFAULT_TIMER_SECONDS = 10
    const val OPTION_COUNT = 4

    fun coerceWordCount(count: Int): Int = count.coerceIn(MIN_WORDS, MAX_WORDS)

    /** Normalized lowercase key used to match a vocabulary word or ID in local mistake storage. */
    fun mistakeKey(wordOrId: String): String = wordOrId.trim().lowercase()

    /** Canonical mistake key for a [VocabularyEntry] (normalized headword, or normalized ID if blank). */
    fun mistakeKey(entry: VocabularyEntry): String =
        mistakeKey(entry.wordDetails.word).ifEmpty { mistakeKey(entry.id) }

    /** All normalized keys (both headword and entry ID) that identify [entry] in local mistake storage. */
    fun mistakeKeysForEntry(entry: VocabularyEntry): Set<String> = buildSet {
        val wordKey = mistakeKey(entry.wordDetails.word)
        val idKey = mistakeKey(entry.id)
        if (wordKey.isNotEmpty()) add(wordKey)
        if (idKey.isNotEmpty()) add(idKey)
    }

    /** True if [entry] matches any key in [mistakeWords] (by normalized headword or entry ID). */
    fun isMistakeEntry(entry: VocabularyEntry, mistakeWords: Set<String>): Boolean {
        if (mistakeWords.isEmpty()) return false
        val normalized = mistakeWords.map(::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (normalized.isEmpty()) return false
        return mistakeKeysForEntry(entry).any { it in normalized }
    }

    /**
     * True if [entry] has at least one valid relation for the chosen [modes].
     */
    fun isEligible(entry: VocabularyEntry, modes: Set<VocabularyQuizMode>): Boolean {
        val clean = entry.sanitized()
        if (!clean.wordDetails.isValid) return false
        val hasSyn = VocabularyQuizMode.Synonyms in modes && clean.synonyms.any { it.word.isNotBlank() }
        val hasAnt = VocabularyQuizMode.Antonyms in modes && clean.antonyms.any { it.word.isNotBlank() }
        return hasSyn || hasAnt
    }

    /**
     * Selects up to [wordCount] (clamped to `5..25`) vocabulary entries eligible for [modes].
     *
     * When [mistakeWords] is non-empty, locally saved mistake entries are chosen first (up to [wordCount]),
     * and any remaining slots are filled with randomly chosen non-mistake entries.
     * Uses the user's stored [entries] when available, falling back to [FALLBACK_ENTRIES] if the user's
     * database has no eligible words yet.
     */
    fun selectStudyWords(
        entries: List<VocabularyEntry>,
        modes: Set<VocabularyQuizMode>,
        wordCount: Int,
        random: Random = Random.Default,
        mistakeWords: Set<String> = emptySet(),
    ): List<VocabularyEntry> {
        val targetCount = coerceWordCount(wordCount)
        val userEligible = entries
            .map { it.sanitized() }
            .filter { isEligible(it, modes) }
        val pool = userEligible.ifEmpty {
            FALLBACK_ENTRIES.filter { isEligible(it, modes) }
        }
        val normalizedMistakes = mistakeWords.map(::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (normalizedMistakes.isEmpty()) {
            if (pool.size <= targetCount) return pool.shuffled(random)
            return pool.shuffled(random).take(targetCount)
        }

        val (mistakePool, regularPool) = pool.partition { isMistakeEntry(it, normalizedMistakes) }
        if (mistakePool.isEmpty()) {
            if (pool.size <= targetCount) return pool.shuffled(random)
            return pool.shuffled(random).take(targetCount)
        }

        val chosenMistakes = mistakePool.shuffled(random).take(targetCount)
        val remainingSlots = (targetCount - chosenMistakes.size).coerceAtLeast(0)
        val chosenRegular = if (remainingSlots > 0) {
            regularPool.shuffled(random).take(remainingSlots)
        } else {
            emptyList()
        }
        return chosenMistakes + chosenRegular
    }

    /**
     * Starter vocabulary entries (25 complete words with English/Hinglish meanings, examples, 2 synonyms,
     * and 2 antonyms each) seeded directly into Firebase Firestore and used as a fallback pool.
     */
    val FALLBACK_ENTRIES: List<VocabularyEntry> = listOf(
        VocabularyEntry(
            id = "seed_abundant",
            wordDetails = WordDetails(
                word = "Abundant",
                meaningEnglish = "Existing or available in large quantities",
                meaningHinglish = "Bahut zyada matra mein hona",
                exampleSentence = "Rainfall is abundant in tropical regions.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Plentiful",
                    meaningEnglish = "Existing in great quantities",
                    meaningHinglish = "Kaafi matra mein uplabdh",
                    exampleSentence = "Fresh fruits are plentiful in summer.",
                ),
                WordDetails(
                    word = "Copious",
                    meaningEnglish = "Abundant in supply or quantity",
                    meaningHinglish = "Bharpoor matra mein",
                    exampleSentence = "She took copious notes during the lecture.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Scarce",
                    meaningEnglish = "Insufficient for the demand",
                    meaningHinglish = "Bahut kam ya durlabh",
                    exampleSentence = "Water becomes scarce in the desert.",
                ),
                WordDetails(
                    word = "Meager",
                    meaningEnglish = "Lacking in quantity or quality",
                    meaningHinglish = "Alpa ya bahut thoda",
                    exampleSentence = "They survived on a meager ration of food.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_candid",
            wordDetails = WordDetails(
                word = "Candid",
                meaningEnglish = "Truthful and straightforward; frank",
                meaningHinglish = "Saaf aur sach bolne wala",
                exampleSentence = "She gave a candid interview about her career.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Frank",
                    meaningEnglish = "Open, honest, and direct in speech",
                    meaningHinglish = "Khulkar sach kehne wala",
                    exampleSentence = "Let us have a frank discussion.",
                ),
                WordDetails(
                    word = "Forthright",
                    meaningEnglish = "Direct and outspoken",
                    meaningHinglish = "Seedha aur spasht",
                    exampleSentence = "He is known for his forthright manner.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Deceitful",
                    meaningEnglish = "Guilty of or involving deceit; misleading",
                    meaningHinglish = "Dhokebaaz ya jhootha",
                    exampleSentence = "Such deceitful behavior broke their trust.",
                ),
                WordDetails(
                    word = "Evasive",
                    meaningEnglish = "Tending to avoid commitment or self-revelation",
                    meaningHinglish = "Baat ghumane wala",
                    exampleSentence = "He gave an evasive answer to the question.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_diligent",
            wordDetails = WordDetails(
                word = "Diligent",
                meaningEnglish = "Having or showing care and conscientiousness in one's work",
                meaningHinglish = "Mehnati aur lagansheel",
                exampleSentence = "A diligent student always prepares ahead of time.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Industrious",
                    meaningEnglish = "Diligent and hard-working",
                    meaningHinglish = "Kadi mehnat karne wala",
                    exampleSentence = "The industrious team completed the project early.",
                ),
                WordDetails(
                    word = "Assiduous",
                    meaningEnglish = "Showing great care and perseverance",
                    meaningHinglish = "Lagaatar prayas karne wala",
                    exampleSentence = "She was assiduous in her research.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Indolent",
                    meaningEnglish = "Wanting to avoid activity or exertion; lazy",
                    meaningHinglish = "Aalsi ya kaamchor",
                    exampleSentence = "An indolent attitude leads to missed opportunities.",
                ),
                WordDetails(
                    word = "Negligent",
                    meaningEnglish = "Failing to take proper care in doing something",
                    meaningHinglish = "Laaparwah",
                    exampleSentence = "The report found that the driver was negligent.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_ephemeral",
            wordDetails = WordDetails(
                word = "Ephemeral",
                meaningEnglish = "Lasting for a very short time",
                meaningHinglish = "Kshanik ya thode samay ka",
                exampleSentence = "Fame in the digital age can be ephemeral.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Fleeting",
                    meaningEnglish = "Lasting for a very short time",
                    meaningHinglish = "Pal bhar rehne wala",
                    exampleSentence = "We caught a fleeting glimpse of the comet.",
                ),
                WordDetails(
                    word = "Transient",
                    meaningEnglish = "Lasting only for a short time; impermanent",
                    meaningHinglish = "Asthayi",
                    exampleSentence = "The storm caused only transient disruption.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Permanent",
                    meaningEnglish = "Lasting or intended to last indefinitely",
                    meaningHinglish = "Sthayi ya hamesha rehne wala",
                    exampleSentence = "They are looking for a permanent solution.",
                ),
                WordDetails(
                    word = "Enduring",
                    meaningEnglish = "Continuing or long-lasting",
                    meaningHinglish = "Lamba chalne wala",
                    exampleSentence = "Tagore left an enduring legacy in literature.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_lucid",
            wordDetails = WordDetails(
                word = "Lucid",
                meaningEnglish = "Expressed clearly; easy to understand",
                meaningHinglish = "Spasht aur aasani se samajh aane wala",
                exampleSentence = "The professor gave a lucid explanation of quantum physics.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Coherent",
                    meaningEnglish = "Logical and consistent",
                    meaningHinglish = "Tarkik aur spasht",
                    exampleSentence = "She presented a coherent argument.",
                ),
                WordDetails(
                    word = "Articulate",
                    meaningEnglish = "Able to speak fluently and coherently",
                    meaningHinglish = "Saaf aur prabhavshali dhang se bolne wala",
                    exampleSentence = "He is an articulate speaker.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Obscure",
                    meaningEnglish = "Uncertain and unclear in meaning",
                    meaningHinglish = "Aspasht ya dhundhla",
                    exampleSentence = "The meaning of the ancient text remains obscure.",
                ),
                WordDetails(
                    word = "Ambiguous",
                    meaningEnglish = "Open to more than one interpretation",
                    meaningHinglish = "Dviarthi ya uljha hua",
                    exampleSentence = "Avoid ambiguous wording in contracts.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_benevolent",
            wordDetails = WordDetails(
                word = "Benevolent",
                meaningEnglish = "Well meaning and kindly",
                meaningHinglish = "Dayalu aur paropkari",
                exampleSentence = "A benevolent donor funded the new library.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Compassionate",
                    meaningEnglish = "Feeling or showing sympathy and concern for others",
                    meaningHinglish = "Sahanubhuti rakhne wala",
                    exampleSentence = "She is a compassionate physician.",
                ),
                WordDetails(
                    word = "Altruistic",
                    meaningEnglish = "Showing a disinterested and selfless concern for others",
                    meaningHinglish = "Niswarth bhav se madad karne wala",
                    exampleSentence = "His altruistic deeds inspired the village.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Malevolent",
                    meaningEnglish = "Having or showing a wish to do evil to others",
                    meaningHinglish = "Durbhavna rakhne wala",
                    exampleSentence = "The story featured a malevolent villain.",
                ),
                WordDetails(
                    word = "Callous",
                    meaningEnglish = "Showing an insensitive and cruel disregard for others",
                    meaningHinglish = "Kathor ya bedard",
                    exampleSentence = "His callous remarks hurt everyone.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_resilient",
            wordDetails = WordDetails(
                word = "Resilient",
                meaningEnglish = "Able to withstand or recover quickly from difficult conditions",
                meaningHinglish = "Mushkilon se jaldi ubharne wala",
                exampleSentence = "Local communities proved resilient after the flood.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Hardy",
                    meaningEnglish = "Robust and capable of enduring difficult conditions",
                    meaningHinglish = "Majboot aur sahansheel",
                    exampleSentence = "Mountain goats are hardy animals.",
                ),
                WordDetails(
                    word = "Buoyant",
                    meaningEnglish = "Cheerful and optimistic; able to recover quickly",
                    meaningHinglish = "Utsahi aur jaldi sambhalne wala",
                    exampleSentence = "The team stayed in a buoyant mood.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Fragile",
                    meaningEnglish = "Easily broken or damaged",
                    meaningHinglish = "Nazuk ya kamzor",
                    exampleSentence = "Handle the fragile glassware with care.",
                ),
                WordDetails(
                    word = "Vulnerable",
                    meaningEnglish = "Susceptible to physical or emotional harm",
                    meaningHinglish = "Asurakshit ya chot pahunchne yogya",
                    exampleSentence = "Young seedlings are vulnerable to frost.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_pragmatic",
            wordDetails = WordDetails(
                word = "Pragmatic",
                meaningEnglish = "Dealing with things sensibly and realistically",
                meaningHinglish = "Vyavaharik aur yatharthvadi",
                exampleSentence = "We need a pragmatic approach to solve traffic congestion.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Practical",
                    meaningEnglish = "Concerned with actual use rather than theory",
                    meaningHinglish = "Kaam aane layak aur vyavaharik",
                    exampleSentence = "She offered practical advice for the exam.",
                ),
                WordDetails(
                    word = "Realistic",
                    meaningEnglish = "Having a sensible and achievable idea of what can be done",
                    meaningHinglish = "Vastavikta par aadharit",
                    exampleSentence = "Set realistic goals for the semester.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Idealistic",
                    meaningEnglish = "Unrealistically aiming for perfection",
                    meaningHinglish = "Kalpanik aadarshon wala",
                    exampleSentence = "His idealistic plan ignored budget limits.",
                ),
                WordDetails(
                    word = "Impractical",
                    meaningEnglish = "Not adapted for use or action; not sensible",
                    meaningHinglish = "Avyavaharik",
                    exampleSentence = "High heels are impractical for hiking.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_meticulous",
            wordDetails = WordDetails(
                word = "Meticulous",
                meaningEnglish = "Showing great attention to detail; very careful and precise",
                meaningHinglish = "Har chhoti baat ka bariki se dhyan rakhne wala",
                exampleSentence = "The watchmaker did meticulous work on the gears.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Fastidious",
                    meaningEnglish = "Very attentive to and concerned about accuracy and detail",
                    meaningHinglish = "Bahut savdhan aur barik",
                    exampleSentence = "He is fastidious about keeping his desk tidy.",
                ),
                WordDetails(
                    word = "Punctilious",
                    meaningEnglish = "Showing great attention to detail or correct behavior",
                    meaningHinglish = "Niyamon aur barikiyon ka pakka",
                    exampleSentence = "She was punctilious in following every safety rule.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Careless",
                    meaningEnglish = "Not giving sufficient attention or thought to avoiding harm or errors",
                    meaningHinglish = "Beparwah",
                    exampleSentence = "A careless mistake cost him the match.",
                ),
                WordDetails(
                    word = "Sloppy",
                    meaningEnglish = "Careless and unsystematic; excessively casual",
                    meaningHinglish = "Bina dhyan ke kiya gaya",
                    exampleSentence = "The editor rejected the sloppy draft.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_audacious",
            wordDetails = WordDetails(
                word = "Audacious",
                meaningEnglish = "Showing a willingness to take surprisingly bold risks",
                meaningHinglish = "Sahasi aur nidar",
                exampleSentence = "The mountaineer made an audacious climb in winter.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Intrepid",
                    meaningEnglish = "Fearless; adventurous",
                    meaningHinglish = "Nirbhik",
                    exampleSentence = "Intrepid explorers mapped the uncharted island.",
                ),
                WordDetails(
                    word = "Daring",
                    meaningEnglish = "Adventurous or audaciously bold",
                    meaningHinglish = "Himmatwala",
                    exampleSentence = "The rescue was a daring operation.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Timid",
                    meaningEnglish = "Showing a lack of courage or confidence",
                    meaningHinglish = "Darpok ya sankochi",
                    exampleSentence = "The timid kitten hid behind the curtain.",
                ),
                WordDetails(
                    word = "Cowardly",
                    meaningEnglish = "Lacking courage",
                    meaningHinglish = "Kayar",
                    exampleSentence = "Running away from responsibility is a cowardly act.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_tenacious",
            wordDetails = WordDetails(
                word = "Tenacious",
                meaningEnglish = "Tending to keep a firm hold of something; persistent",
                meaningHinglish = "Dridh nishchayi aur haar na maanne wala",
                exampleSentence = "Her tenacious defense won the championship.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Persistent",
                    meaningEnglish = "Continuing firmly in a course of action despite difficulty",
                    meaningHinglish = "Lagatar date rehne wala",
                    exampleSentence = "Persistent effort leads to mastery.",
                ),
                WordDetails(
                    word = "Resolute",
                    meaningEnglish = "Admirably purposeful, determined, and unwavering",
                    meaningHinglish = "Atal aur pakke irade wala",
                    exampleSentence = "She remained resolute in her decision.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Irresolute",
                    meaningEnglish = "Showing or feeling hesitancy; uncertain",
                    meaningHinglish = "Duvidha mein rehne wala",
                    exampleSentence = "An irresolute leader struggles in a crisis.",
                ),
                WordDetails(
                    word = "Yielding",
                    meaningEnglish = "Giving way under pressure; submissive",
                    meaningHinglish = "Jaldi haar maan lene wala",
                    exampleSentence = "He was too yielding during the negotiation.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_frugal",
            wordDetails = WordDetails(
                word = "Frugal",
                meaningEnglish = "Sparing or economical with regard to money or food",
                meaningHinglish = "Kifayati ya soch-samajhkar kharch karne wala",
                exampleSentence = "Living a frugal lifestyle helped them save for a house.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Thrifty",
                    meaningEnglish = "Using money and other resources carefully and not wastefully",
                    meaningHinglish = "Bachat karne wala",
                    exampleSentence = "My grandmother was thrifty and never wasted food.",
                ),
                WordDetails(
                    word = "Economical",
                    meaningEnglish = "Giving good value or service in relation to the amount of money spent",
                    meaningHinglish = "Kam kharchila",
                    exampleSentence = "Small cars are more economical on fuel.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Extravagant",
                    meaningEnglish = "Lacking restraint in spending money or using resources",
                    meaningHinglish = "Fizulkharch",
                    exampleSentence = "They threw an extravagant party at the palace.",
                ),
                WordDetails(
                    word = "Prodigal",
                    meaningEnglish = "Spending money or resources freely and recklessly",
                    meaningHinglish = "Andhadhundh kharch karne wala",
                    exampleSentence = "Prodigal habits quickly drained the treasury.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_gregarious",
            wordDetails = WordDetails(
                word = "Gregarious",
                meaningEnglish = "Fond of company; sociable",
                meaningHinglish = "Milansaar aur logon mein ghulne-milne wala",
                exampleSentence = "Dolphins are gregarious creatures that travel in pods.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Sociable",
                    meaningEnglish = "Willing to talk and engage in activities with other people",
                    meaningHinglish = "Samajik aur vyavaharik",
                    exampleSentence = "He is a sociable host who welcomes everyone.",
                ),
                WordDetails(
                    word = "Convivial",
                    meaningEnglish = "Friendly, lively, and enjoyable",
                    meaningHinglish = "Khushmizaaj aur milansaar",
                    exampleSentence = "A convivial atmosphere filled the festival hall.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Reclusive",
                    meaningEnglish = "Avoiding the company of other people; solitary",
                    meaningHinglish = "Ekantpriya ya akela rehne wala",
                    exampleSentence = "The reclusive author rarely gave interviews.",
                ),
                WordDetails(
                    word = "Aloof",
                    meaningEnglish = "Not friendly or forthcoming; cool and distant",
                    meaningHinglish = "Doori banakar rakhne wala",
                    exampleSentence = "He stood aloof from the noisy crowd.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_mitigate",
            wordDetails = WordDetails(
                word = "Mitigate",
                meaningEnglish = "Make less severe, serious, or painful",
                meaningHinglish = "Gambhirta ya dard ko kam karna",
                exampleSentence = "Planting trees helps mitigate soil erosion.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Alleviate",
                    meaningEnglish = "Make suffering, deficiency, or a problem less severe",
                    meaningHinglish = "Takleef kam karna",
                    exampleSentence = "The medicine helped alleviate her headache.",
                ),
                WordDetails(
                    word = "Assuage",
                    meaningEnglish = "Make an unpleasant feeling less intense",
                    meaningHinglish = "Shant ya halka karna",
                    exampleSentence = "His kind words helped assuage their fears.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Aggravate",
                    meaningEnglish = "Make a problem, injury, or offense worse or more serious",
                    meaningHinglish = "Stithi ko aur bigaad dena",
                    exampleSentence = "Scratching the rash will only aggravate it.",
                ),
                WordDetails(
                    word = "Exacerbate",
                    meaningEnglish = "Make a problem, bad situation, or negative feeling worse",
                    meaningHinglish = "Badhtar banana",
                    exampleSentence = "The drought exacerbated the food shortage.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_ostentatious",
            wordDetails = WordDetails(
                word = "Ostentatious",
                meaningEnglish = "Characterized by vulgar or pretentious display; designed to impress",
                meaningHinglish = "Dikhawati ya shaan-o-shaukat dikhane wala",
                exampleSentence = "He wore an ostentatious diamond watch.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Flamboyant",
                    meaningEnglish = "Tending to attract attention because of exuberance or stylishness",
                    meaningHinglish = "Bhadkila aur aakarshak",
                    exampleSentence = "The singer was famous for his flamboyant costumes.",
                ),
                WordDetails(
                    word = "Pretentious",
                    meaningEnglish = "Attempting to impress by affecting greater importance than is possessed",
                    meaningHinglish = "Bada banne ka dikhawa karne wala",
                    exampleSentence = "The movie had a pretentious script.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Modest",
                    meaningEnglish = "Unassuming in the estimation of one's abilities or achievements",
                    meaningHinglish = "Vinamra aur saada",
                    exampleSentence = "Despite her success, she lived in a modest home.",
                ),
                WordDetails(
                    word = "Unpretentious",
                    meaningEnglish = "Not attempting to impress others with an appearance of superiority",
                    meaningHinglish = "Saadgi bhara",
                    exampleSentence = "We enjoyed a warm, unpretentious meal.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_prudent",
            wordDetails = WordDetails(
                word = "Prudent",
                meaningEnglish = "Acting with or showing care and thought for the future",
                meaningHinglish = "Samajhdaar aur durdarshi",
                exampleSentence = "It is prudent to keep an emergency fund.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Judicious",
                    meaningEnglish = "Having, showing, or done with good judgment or sense",
                    meaningHinglish = "Vivekpoorna",
                    exampleSentence = "Judicious use of time improves productivity.",
                ),
                WordDetails(
                    word = "Circumspect",
                    meaningEnglish = "Wary and unwilling to take risks",
                    meaningHinglish = "Chaukanna aur savdhan",
                    exampleSentence = "Diplomats must be circumspect in their public statements.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Reckless",
                    meaningEnglish = "Without thinking or caring about the consequences of an action",
                    meaningHinglish = "Laaparwah aur utavala",
                    exampleSentence = "Reckless driving endangers everyone on the road.",
                ),
                WordDetails(
                    word = "Imprudent",
                    meaningEnglish = "Not showing care for the consequences of an action; rash",
                    meaningHinglish = "asamajhdaari bhara",
                    exampleSentence = "It would be imprudent to invest without research.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_scrupulous",
            wordDetails = WordDetails(
                word = "Scrupulous",
                meaningEnglish = "Diligent, thorough, and extremely attentive to moral details",
                meaningHinglish = "Imaandaar aur naitik roop se satark",
                exampleSentence = "The auditor was scrupulous in checking every receipt.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Conscientious",
                    meaningEnglish = "Wishing to do what is right, especially to do one's work well",
                    meaningHinglish = "Kartavyanishth",
                    exampleSentence = "A conscientious teacher cares for every student.",
                ),
                WordDetails(
                    word = "Honest",
                    meaningEnglish = "Free of deceit and untruthfulness; sincere",
                    meaningHinglish = "Imaandaar",
                    exampleSentence = "He earned a reputation as an honest merchant.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Unscrupulous",
                    meaningEnglish = "Having or showing no moral principles; not honest or fair",
                    meaningHinglish = "Beimaan aur anaitik",
                    exampleSentence = "Unscrupulous traders overcharged during the shortage.",
                ),
                WordDetails(
                    word = "Dishonest",
                    meaningEnglish = "Behaving or prone to behave in an untrustworthy or fraudulent way",
                    meaningHinglish = "Kapti ya jhootha",
                    exampleSentence = "Dishonest practices ruin a company's name.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_tranquil",
            wordDetails = WordDetails(
                word = "Tranquil",
                meaningEnglish = "Free from disturbance; calm",
                meaningHinglish = "Shant aur sukoon bhara",
                exampleSentence = "The lake looked tranquil at sunrise.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Serene",
                    meaningEnglish = "Calm, peaceful, and untroubled",
                    meaningHinglish = "Nirmal aur shant",
                    exampleSentence = "She kept a serene smile throughout the ceremony.",
                ),
                WordDetails(
                    word = "Placid",
                    meaningEnglish = "Not easily upset or excited; calm and peaceful",
                    meaningHinglish = "Shant swabhav ka",
                    exampleSentence = "The boat glided across the placid water.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Turbulent",
                    meaningEnglish = "Characterized by conflict, disorder, or confusion",
                    meaningHinglish = "Ashant aur uthal-puthal bhara",
                    exampleSentence = "The airplane passed through turbulent skies.",
                ),
                WordDetails(
                    word = "Agitated",
                    meaningEnglish = "Feeling or appearing troubled or nervous",
                    meaningHinglish = "Vyakul ya pareshan",
                    exampleSentence = "The crowd grew agitated after the delay.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_verbose",
            wordDetails = WordDetails(
                word = "Verbose",
                meaningEnglish = "Using or expressed in more words than are needed",
                meaningHinglish = "Zaroorat se zyada shabdon wala",
                exampleSentence = "The legal contract was needlessly verbose.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Loquacious",
                    meaningEnglish = "Tending to talk a great deal; talkative",
                    meaningHinglish = "Bahut baatuni",
                    exampleSentence = "The loquacious guide shared stories for hours.",
                ),
                WordDetails(
                    word = "Garrulous",
                    meaningEnglish = "Excessively talkative, especially on trivial matters",
                    meaningHinglish = "Bina ruke bolne wala",
                    exampleSentence = "A garrulous neighbor delayed him at the gate.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Concise",
                    meaningEnglish = "Giving a lot of information clearly and in a few words",
                    meaningHinglish = "Sankshipt aur spasht",
                    exampleSentence = "Write a concise summary of the chapter.",
                ),
                WordDetails(
                    word = "Succinct",
                    meaningEnglish = "Briefly and clearly expressed",
                    meaningHinglish = "Kam shabdon mein saaf baat",
                    exampleSentence = "Her email was succinct and to the point.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_zealous",
            wordDetails = WordDetails(
                word = "Zealous",
                meaningEnglish = "Having or showing great energy or enthusiasm in pursuit of a cause",
                meaningHinglish = "Utsahi aur josh se bhara",
                exampleSentence = "The volunteers were zealous in cleaning the beach.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Fervent",
                    meaningEnglish = "Having or displaying a passionate intensity",
                    meaningHinglish = "Gahre utsah wala",
                    exampleSentence = "He is a fervent supporter of public education.",
                ),
                WordDetails(
                    word = "Ardent",
                    meaningEnglish = "Enthusiastic or passionate",
                    meaningHinglish = "Joshiya aur lagansheel",
                    exampleSentence = "An ardent fan waited in the rain for hours.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Apathetic",
                    meaningEnglish = "Showing or feeling no interest, enthusiasm, or concern",
                    meaningHinglish = "Udasin ya beparwah",
                    exampleSentence = "Voters cannot afford to be apathetic.",
                ),
                WordDetails(
                    word = "Indifferent",
                    meaningEnglish = "Having no particular interest or sympathy; unconcerned",
                    meaningHinglish = "Nishpaksh ya ruchi na rakhne wala",
                    exampleSentence = "He seemed indifferent to the cold weather.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_amicable",
            wordDetails = WordDetails(
                word = "Amicable",
                meaningEnglish = "Characterized by friendliness and absence of discord",
                meaningHinglish = "Mitrapoorna aur shantipoorna",
                exampleSentence = "The two partners reached an amicable settlement.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Cordial",
                    meaningEnglish = "Warm and friendly",
                    meaningHinglish = "Snehpoorna aur dostana",
                    exampleSentence = "They maintained cordial relations with their neighbors.",
                ),
                WordDetails(
                    word = "Harmonious",
                    meaningEnglish = "Free from disagreement or dissent",
                    meaningHinglish = "Talmel aur samanjasya wala",
                    exampleSentence = "A harmonious team works without friction.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Hostile",
                    meaningEnglish = "Unfriendly; antagonistic",
                    meaningHinglish = "Shatrutapoorna",
                    exampleSentence = "The explorers faced a hostile environment.",
                ),
                WordDetails(
                    word = "Belligerent",
                    meaningEnglish = "Hostile and aggressive",
                    meaningHinglish = "Ladaaku aur aakramak",
                    exampleSentence = "His belligerent tone escalated the argument.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_eloquent",
            wordDetails = WordDetails(
                word = "Eloquent",
                meaningEnglish = "Fluent or persuasive in speaking or writing",
                meaningHinglish = "Suvakta ya prabhavshali dhang se bolne wala",
                exampleSentence = "She delivered an eloquent speech at the assembly.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Expressive",
                    meaningEnglish = "Effectively conveying thought or feeling",
                    meaningHinglish = "Bhavpoorna",
                    exampleSentence = "His expressive poetry moved the audience.",
                ),
                WordDetails(
                    word = "Silver-tongued",
                    meaningEnglish = "Persuasive and fluent in speaking",
                    meaningHinglish = "Meethi aur prabhavshali vaani wala",
                    exampleSentence = "The silver-tongued orator won the debate.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Inarticulate",
                    meaningEnglish = "Unable to speak distinctly or express oneself clearly",
                    meaningHinglish = "Apni baat saaf na keh paane wala",
                    exampleSentence = "Shock left him momentarily inarticulate.",
                ),
                WordDetails(
                    word = "Hesitant",
                    meaningEnglish = "Tentative, unsure, or slow in acting or speaking",
                    meaningHinglish = "Hichkichane wala",
                    exampleSentence = "He gave a hesitant reply to the question.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_immutable",
            wordDetails = WordDetails(
                word = "Immutable",
                meaningEnglish = "Unchanging over time or unable to be changed",
                meaningHinglish = "Aparivartansheel ya jo badal na sake",
                exampleSentence = "The laws of physics are treated as immutable.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Invariable",
                    meaningEnglish = "Never changing",
                    meaningHinglish = "Sada ek jaisa rehne wala",
                    exampleSentence = "His morning routine was invariable.",
                ),
                WordDetails(
                    word = "Steadfast",
                    meaningEnglish = "Resolutely or dutifully firm and unwavering",
                    meaningHinglish = "Atal aur sthir",
                    exampleSentence = "She remained a steadfast friend through hard times.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Mutable",
                    meaningEnglish = "Liable to change",
                    meaningHinglish = "Badalne yogya",
                    exampleSentence = "Public opinion is often mutable.",
                ),
                WordDetails(
                    word = "Volatile",
                    meaningEnglish = "Liable to change rapidly and unpredictably",
                    meaningHinglish = "Tezi se badalne wala",
                    exampleSentence = "Stock prices were volatile all week.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_magnanimous",
            wordDetails = WordDetails(
                word = "Magnanimous",
                meaningEnglish = "Generous or forgiving, especially toward a rival or less powerful person",
                meaningHinglish = "Udaar aur bada dil rakhne wala",
                exampleSentence = "The champion was magnanimous in victory.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Generous",
                    meaningEnglish = "Showing a readiness to give more of something than is strictly necessary",
                    meaningHinglish = "Dani aur udaar",
                    exampleSentence = "Thank you for your generous support.",
                ),
                WordDetails(
                    word = "Chivalrous",
                    meaningEnglish = "Courteous and gallant",
                    meaningHinglish = "Sammanjanak aur udaar",
                    exampleSentence = "He made a chivalrous gesture to help his rival.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Vindictive",
                    meaningEnglish = "Having or showing a strong or unreasoning desire for revenge",
                    meaningHinglish = "Badla lene ki bhavna rakhne wala",
                    exampleSentence = "A good leader is never vindictive.",
                ),
                WordDetails(
                    word = "Petty",
                    meaningEnglish = "Of little importance; mean or ungenerous in small matters",
                    meaningHinglish = "Chhoti soch wala",
                    exampleSentence = "They wasted time on petty quarrels.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_sagacious",
            wordDetails = WordDetails(
                word = "Sagacious",
                meaningEnglish = "Having or showing keen mental discernment and good judgment",
                meaningHinglish = "Buddhiman aur samajhdaar",
                exampleSentence = "The sagacious mentor guided the young entrepreneurs.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Astute",
                    meaningEnglish = "Having or showing an ability to accurately assess situations",
                    meaningHinglish = "Chatur aur teekhi samajh wala",
                    exampleSentence = "She made an astute observation about the market.",
                ),
                WordDetails(
                    word = "Perspicacious",
                    meaningEnglish = "Having a ready insight into and understanding of things",
                    meaningHinglish = "Gahri parakh rakhne wala",
                    exampleSentence = "The detective offered a perspicacious analysis.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Foolish",
                    meaningEnglish = "Lacking good sense or judgment; unwise",
                    meaningHinglish = "Moorkhtapoorna",
                    exampleSentence = "It was foolish to ignore the warning signs.",
                ),
                WordDetails(
                    word = "Naive",
                    meaningEnglish = "Showing a lack of experience, wisdom, or judgment",
                    meaningHinglish = "Bhola ya anubhavheen",
                    exampleSentence = "He was too naive to spot the scam.",
                ),
            ),
        ),
    )
}

/**
 * Dedicated Vocabulary Quiz Generator & Scheduling Algorithm (distinct from Math's infinite `PracticeSession`).
 *
 * How it works:
 * 1. **Semantic Cluster Graph Construction**:
 *    Each studied [VocabularyEntry] forms a semantic cluster with two sets:
 *    - Same-meaning set `S+ = { mainWord } U synonyms` (every pair of distinct words in `S+` are synonyms).
 *    - Opposite-meaning set `S- = antonyms` (every word in `S-` is an antonym of every word in `S+`, including both
 *      the main word and all synonyms).
 * 2. **Full-Coverage Derangement & Cross-Cluster Pairing**:
 *    - Every active word in the studied entries (main word, each synonym, and each antonym) is guaranteed to be asked
 *      as a question prompt once, making the quiz finite and ending right after all words are covered.
 *    - For `S+` in Synonym mode, words are paired using a shuffled single-cycle derangement `w_i -> w_{(i+1) mod k}`.
 *      Whenever a word has 2+ synonyms (`|S+| >= 3`), this directly creates `Synonym A -> Synonym B` questions where
 *      the main word is neither the question nor the correct option.
 *    - For Antonym questions, words in `S-` are paired across a shuffled `S+` (and vice-versa in Antonym-only mode),
 *      so antonyms pair directly with synonyms as well as with the main word.
 * 3. **Stratified Cluster-Distance Interleaving**:
 *    - Questions are grouped by cluster and interleaved greedily by picking from the largest remaining cluster that
 *      differs from the previously asked cluster, preventing back-to-back questions from the same word family.
 * 4. **Conflict-Free Smart Distractors**:
 *    - Excludes all words in the cluster (and across all entries) that also satisfy the asked relation with the prompt,
 *      guaranteeing a single unambiguous correct option among the 4 choices.
 *    - Injects up to 1 opposite-relation word from the same cluster as a comprehension trap when available, then fills
 *      remaining distractors from other studied words first before falling back to the wider database pool.
 */
object VocabularyQuizEngine {

    private data class SemanticCluster(
        val id: String,
        val sameMeaning: List<String>,
        val oppositeMeaning: List<String>,
    )

    private data class RawQuestion(
        val cluster: SemanticCluster,
        val mode: VocabularyQuizMode,
        val promptWord: String,
        val correctAnswer: String,
    )

    fun generateQuiz(
        studyEntries: List<VocabularyEntry>,
        allEntries: List<VocabularyEntry> = studyEntries,
        modes: Set<VocabularyQuizMode>,
        random: Random = Random.Default,
    ): List<VocabularyQuizItem> {
        if (studyEntries.isEmpty() || modes.isEmpty()) return emptyList()

        val clusters = studyEntries.mapIndexedNotNull { index, entry ->
            toCluster(entry, fallbackId = "cluster_$index")
        }
        if (clusters.isEmpty()) return emptyList()

        val allSourceEntries = studyEntries + allEntries + VocabularyQuizRules.FALLBACK_ENTRIES
        val allClusters = allSourceEntries
            .mapIndexedNotNull { index, entry -> toCluster(entry, fallbackId = "all_$index") }
        val detailsByWord = buildWordDetailsLookup(allSourceEntries)

        val rawByCluster: MutableMap<String, MutableList<RawQuestion>> = LinkedHashMap()
        for (cluster in clusters) {
            val questions = buildQuestionsForCluster(cluster, modes, random)
            if (questions.isNotEmpty()) {
                rawByCluster[cluster.id] = questions.shuffled(random).toMutableList()
            }
        }
        if (rawByCluster.isEmpty()) return emptyList()

        val interleaved = interleaveClusters(rawByCluster, random)
        val studiedWordPool = clusters.flatMap { it.sameMeaning + it.oppositeMeaning }.distinctBy { it.lowercase() }
        val globalWordPool = allClusters.flatMap { it.sameMeaning + it.oppositeMeaning }.distinctBy { it.lowercase() }

        return interleaved.map { raw ->
            val options = buildOptions(
                raw = raw,
                allClusters = allClusters,
                studiedWordPool = studiedWordPool,
                globalWordPool = globalWordPool,
                random = random,
            )
            val promptDetails = detailsByWord[raw.promptWord.lowercase()]
                ?: WordDetails(word = raw.promptWord)
            val optionDetails = options.map { option ->
                detailsByWord[option.lowercase()] ?: WordDetails(word = option)
            }
            VocabularyQuizItem(
                clusterId = raw.cluster.id,
                mode = raw.mode,
                promptWord = raw.promptWord,
                correctAnswer = raw.correctAnswer,
                options = options,
                promptDetails = promptDetails,
                optionDetails = optionDetails,
                clusterWord = raw.cluster.sameMeaning.firstOrNull().orEmpty(),
            )
        }
    }

    private fun buildWordDetailsLookup(entries: List<VocabularyEntry>): Map<String, WordDetails> {
        val map = LinkedHashMap<String, WordDetails>()
        fun register(details: WordDetails) {
            val clean = details.trimmed()
            val key = clean.word.lowercase()
            if (key.isEmpty()) return
            val existing = map[key]
            if (existing == null) {
                map[key] = clean
            } else {
                map[key] = WordDetails(
                    word = existing.word.ifBlank { clean.word },
                    meaningEnglish = existing.meaningEnglish.ifBlank { clean.meaningEnglish },
                    meaningHinglish = existing.meaningHinglish.ifBlank { clean.meaningHinglish },
                    exampleSentence = existing.exampleSentence.ifBlank { clean.exampleSentence },
                )
            }
        }
        for (entry in entries) {
            val cleanEntry = entry.sanitized()
            register(cleanEntry.wordDetails)
            cleanEntry.synonyms.forEach(::register)
            cleanEntry.antonyms.forEach(::register)
        }
        return map
    }

    private fun toCluster(entry: VocabularyEntry, fallbackId: String): SemanticCluster? {
        val clean = entry.sanitized()
        val main = clean.wordDetails.word.trim()
        if (main.isEmpty()) return null

        val seen = HashSet<String>()
        val sameMeaning = ArrayList<String>()
        sameMeaning.add(main)
        seen.add(main.lowercase())

        for (syn in clean.synonyms) {
            val word = syn.word.trim()
            if (word.isNotEmpty() && seen.add(word.lowercase())) {
                sameMeaning.add(word)
            }
        }

        val oppositeMeaning = ArrayList<String>()
        for (ant in clean.antonyms) {
            val word = ant.word.trim()
            if (word.isNotEmpty() && seen.add(word.lowercase())) {
                oppositeMeaning.add(word)
            }
        }

        val id = clean.id.ifBlank { "${fallbackId}_${main.lowercase()}" }
        return SemanticCluster(
            id = id,
            sameMeaning = sameMeaning,
            oppositeMeaning = oppositeMeaning,
        )
    }

    private fun buildQuestionsForCluster(
        cluster: SemanticCluster,
        modes: Set<VocabularyQuizMode>,
        random: Random,
    ): List<RawQuestion> {
        val canAskSynonyms = VocabularyQuizMode.Synonyms in modes && cluster.sameMeaning.size >= 2
        val canAskAntonyms = VocabularyQuizMode.Antonyms in modes && cluster.oppositeMeaning.isNotEmpty()
        val result = ArrayList<RawQuestion>()

        when {
            canAskSynonyms && canAskAntonyms -> {
                // Every word in S+ (main word + all synonyms) is asked in a single-cycle derangement over S+.
                // When |S+| >= 3, this guarantees Synonym -> Synonym questions without involving the main word.
                val cycle = cluster.sameMeaning.shuffled(random)
                for (i in cycle.indices) {
                    val prompt = cycle[i]
                    val target = cycle[(i + 1) % cycle.size]
                    result.add(
                        RawQuestion(
                            cluster = cluster,
                            mode = VocabularyQuizMode.Synonyms,
                            promptWord = prompt,
                            correctAnswer = target,
                        ),
                    )
                }
                // Every antonym in S- is asked in Antonyms mode, paired across all members of S+ (both synonyms and main word).
                val shuffledAntonyms = cluster.oppositeMeaning.shuffled(random)
                val shuffledSame = cluster.sameMeaning.shuffled(random)
                for (i in shuffledAntonyms.indices) {
                    val antonym = shuffledAntonyms[i]
                    val partner = shuffledSame[i % shuffledSame.size]
                    result.add(
                        RawQuestion(
                            cluster = cluster,
                            mode = VocabularyQuizMode.Antonyms,
                            promptWord = antonym,
                            correctAnswer = partner,
                        ),
                    )
                }
            }

            canAskSynonyms -> {
                val cycle = cluster.sameMeaning.shuffled(random)
                for (i in cycle.indices) {
                    val prompt = cycle[i]
                    val target = cycle[(i + 1) % cycle.size]
                    result.add(
                        RawQuestion(
                            cluster = cluster,
                            mode = VocabularyQuizMode.Synonyms,
                            promptWord = prompt,
                            correctAnswer = target,
                        ),
                    )
                }
            }

            canAskAntonyms -> {
                // In Antonyms-only mode (or when the entry has no synonyms), every word in S+ asks for an antonym in S-,
                // and every antonym in S- asks for a partner in S+ (main word or synonym).
                val shuffledSame = cluster.sameMeaning.shuffled(random)
                val shuffledAntonyms = cluster.oppositeMeaning.shuffled(random)
                for (i in shuffledSame.indices) {
                    result.add(
                        RawQuestion(
                            cluster = cluster,
                            mode = VocabularyQuizMode.Antonyms,
                            promptWord = shuffledSame[i],
                            correctAnswer = shuffledAntonyms[i % shuffledAntonyms.size],
                        ),
                    )
                }
                val partnersForAntonyms = cluster.sameMeaning.shuffled(random)
                for (j in shuffledAntonyms.indices) {
                    result.add(
                        RawQuestion(
                            cluster = cluster,
                            mode = VocabularyQuizMode.Antonyms,
                            promptWord = shuffledAntonyms[j],
                            correctAnswer = partnersForAntonyms[j % partnersForAntonyms.size],
                        ),
                    )
                }
            }
        }

        return result
    }

    /**
     * Interleaves questions across clusters so no two consecutive questions come from the same word cluster
     * whenever at least one other cluster still has questions remaining.
     */
    private fun interleaveClusters(
        queues: Map<String, MutableList<RawQuestion>>,
        random: Random,
    ): List<RawQuestion> {
        val result = ArrayList<RawQuestion>(queues.values.sumOf { it.size })
        var lastClusterId: String? = null

        while (true) {
            val nonEmpty = queues.entries.filter { it.value.isNotEmpty() }
            if (nonEmpty.isEmpty()) break

            val differentCluster = nonEmpty.filter { it.key != lastClusterId }
            val candidates = differentCluster.ifEmpty { nonEmpty }
            val maxRemaining = candidates.maxOf { it.value.size }
            val topCandidates = candidates.filter { it.value.size == maxRemaining }
            val chosen = topCandidates[random.nextInt(topCandidates.size)]

            val question = chosen.value.removeAt(0)
            result.add(question)
            lastClusterId = chosen.key
        }

        return result
    }

    /**
     * Builds 4 shuffled options (1 correct answer + 3 conflict-free distractors).
     */
    private fun buildOptions(
        raw: RawQuestion,
        allClusters: List<SemanticCluster>,
        studiedWordPool: List<String>,
        globalWordPool: List<String>,
        random: Random,
    ): List<String> {
        val forbiddenLower = HashSet<String>()
        forbiddenLower.add(raw.promptWord.lowercase())
        forbiddenLower.add(raw.correctAnswer.lowercase())

        // Exclude every word in any cluster that would also be a valid answer for (promptWord, mode).
        for (cluster in allClusters) {
            val inSame = cluster.sameMeaning.any { it.equals(raw.promptWord, ignoreCase = true) }
            val inOpposite = cluster.oppositeMeaning.any { it.equals(raw.promptWord, ignoreCase = true) }
            if (!inSame && !inOpposite) continue

            when (raw.mode) {
                VocabularyQuizMode.Synonyms -> {
                    if (inSame) {
                        cluster.sameMeaning.forEach { forbiddenLower.add(it.lowercase()) }
                    }
                    if (inOpposite) {
                        cluster.oppositeMeaning.forEach { forbiddenLower.add(it.lowercase()) }
                    }
                }
                VocabularyQuizMode.Antonyms -> {
                    if (inSame) {
                        cluster.oppositeMeaning.forEach { forbiddenLower.add(it.lowercase()) }
                    }
                    if (inOpposite) {
                        cluster.sameMeaning.forEach { forbiddenLower.add(it.lowercase()) }
                    }
                }
            }
        }

        val distractors = ArrayList<String>(VocabularyQuizRules.OPTION_COUNT - 1)
        val needed = VocabularyQuizRules.OPTION_COUNT - 1

        // 1. Opposite-relation cognitive trap from the same cluster (at most 1).
        val trapCandidates = when (raw.mode) {
            VocabularyQuizMode.Synonyms -> raw.cluster.oppositeMeaning
            VocabularyQuizMode.Antonyms -> {
                val promptInSame = raw.cluster.sameMeaning.any { it.equals(raw.promptWord, ignoreCase = true) }
                if (promptInSame) raw.cluster.sameMeaning else raw.cluster.oppositeMeaning
            }
        }.filter { it.lowercase() !in forbiddenLower }

        if (trapCandidates.isNotEmpty()) {
            val trap = trapCandidates[random.nextInt(trapCandidates.size)]
            distractors.add(trap)
            forbiddenLower.add(trap.lowercase())
        }

        // 2. Words from the other studied entries in the current quiz batch.
        for (candidate in studiedWordPool.shuffled(random)) {
            if (distractors.size >= needed) break
            if (forbiddenLower.add(candidate.lowercase())) {
                distractors.add(candidate)
            }
        }

        // 3. Fallback to the full vocabulary database + starter words if the studied pool had < 4 distinct words.
        for (candidate in globalWordPool.shuffled(random)) {
            if (distractors.size >= needed) break
            if (forbiddenLower.add(candidate.lowercase())) {
                distractors.add(candidate)
            }
        }

        return (listOf(raw.correctAnswer) + distractors).shuffled(random)
    }
}

/**
 * Local persistence action emitted by [VocabularyMistakeTracker] as questions in a Vocabulary quiz are resolved.
 */
sealed interface VocabularyMistakeAction {
    val wordKeys: Set<String>

    /** Save [wordKeys] locally because the user selected a wrong option or timed out on a question for this word. */
    data class SaveMistake(override val wordKeys: Set<String>) : VocabularyMistakeAction

    /**
     * Automatically delete [wordKeys] from local mistake storage because the user answered every question
     * related to that word correctly in this quiz session.
     */
    data class DeleteMastered(override val wordKeys: Set<String>) : VocabularyMistakeAction

    /** No local storage update needed yet. */
    data object None : VocabularyMistakeAction {
        override val wordKeys: Set<String> = emptySet()
    }
}

/**
 * Pure-Kotlin per-session tracker that evaluates whether a vocabulary word should be saved as a local mistake
 * (on wrong selection or timeout) or automatically deleted upon full mastery (when all questions related to that
 * word in the session are answered correctly).
 */
class VocabularyMistakeTracker(
    private val questions: List<VocabularyQuizItem>,
    studyEntries: List<VocabularyEntry> = emptyList(),
) {
    private val outcomes = HashMap<Int, FeedbackType>()
    private val entryKeysByCluster: Map<String, Set<String>> = buildMap {
        for (entry in studyEntries) {
            val keys = VocabularyQuizRules.mistakeKeysForEntry(entry)
            if (keys.isEmpty()) continue
            val idKey = VocabularyQuizRules.mistakeKey(entry.id)
            val wordKey = VocabularyQuizRules.mistakeKey(entry.wordDetails.word)
            if (idKey.isNotEmpty()) put(idKey, keys)
            if (wordKey.isNotEmpty()) put(wordKey, keys)
        }
    }

    fun onQuestionResolved(questionIndex: Int, type: FeedbackType): VocabularyMistakeAction {
        val item = questions.getOrNull(questionIndex) ?: return VocabularyMistakeAction.None
        outcomes[questionIndex] = type

        val clusterKeys = keysForItem(item)
        if (clusterKeys.isEmpty()) return VocabularyMistakeAction.None

        if (type == FeedbackType.Incorrect || type == FeedbackType.Timeout) {
            return VocabularyMistakeAction.SaveMistake(clusterKeys)
        }

        val clusterIndices = questions.indices.filter { idx ->
            isSameCluster(questions[idx], item)
        }
        val allAnsweredCorrectly = clusterIndices.isNotEmpty() &&
            clusterIndices.all { idx -> outcomes[idx] == FeedbackType.Correct }

        return if (allAnsweredCorrectly) {
            VocabularyMistakeAction.DeleteMastered(clusterKeys)
        } else {
            VocabularyMistakeAction.None
        }
    }

    private fun isSameCluster(a: VocabularyQuizItem, b: VocabularyQuizItem): Boolean {
        val idA = VocabularyQuizRules.mistakeKey(a.clusterId)
        val idB = VocabularyQuizRules.mistakeKey(b.clusterId)
        if (idA.isNotEmpty() && idB.isNotEmpty() && idA == idB) return true
        val wordA = VocabularyQuizRules.mistakeKey(a.clusterWord)
        val wordB = VocabularyQuizRules.mistakeKey(b.clusterWord)
        return wordA.isNotEmpty() && wordA == wordB
    }

    private fun keysForItem(item: VocabularyQuizItem): Set<String> {
        val result = LinkedHashSet<String>()
        val wordKey = VocabularyQuizRules.mistakeKey(item.clusterWord)
        val idKey = VocabularyQuizRules.mistakeKey(item.clusterId)
        if (wordKey.isNotEmpty()) result.add(wordKey)
        if (idKey.isNotEmpty()) result.add(idKey)
        if (idKey.isNotEmpty()) entryKeysByCluster[idKey]?.let(result::addAll)
        if (wordKey.isNotEmpty()) entryKeysByCluster[wordKey]?.let(result::addAll)
        return result
    }
}

