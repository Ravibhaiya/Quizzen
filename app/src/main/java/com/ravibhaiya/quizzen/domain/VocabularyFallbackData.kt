package com.ravibhaiya.quizzen.domain

/**
 * Curated starter vocabulary entries used when the user's cloud/local database has fewer than the requested word count.
 */
internal object VocabularyFallbackData {

    val ENTRIES: List<VocabularyEntry> = listOf(
        VocabularyEntry(
            id = "seed_abundant",
            wordDetails = WordDetails(
                word = "Abundant",
                meaningEnglish = "Existing or available in large quantities; plentiful",
                meaningHinglish = "Prachur matra mein uplabdh",
                exampleSentence = "Rainfall is abundant in the monsoon season.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Plentiful",
                    meaningEnglish = "Existing in or yielding great quantities",
                    meaningHinglish = "Bahut adhik matra mein",
                    exampleSentence = "Fresh fruits are plentiful in the summer market.",
                ),
                WordDetails(
                    word = "Copious",
                    meaningEnglish = "Abundant in supply or quantity",
                    meaningHinglish = "Bharpoor ya prachur",
                    exampleSentence = "She took copious notes during the lecture.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Scarce",
                    meaningEnglish = "Insufficient for the demand; hard to find",
                    meaningHinglish = "Durlabh ya kam matra mein",
                    exampleSentence = "Clean water became scarce during the drought.",
                ),
                WordDetails(
                    word = "Meager",
                    meaningEnglish = "Lacking in quantity or quality",
                    meaningHinglish = "Alp ya bahut kam",
                    exampleSentence = "They survived on a meager ration of food.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_candid",
            wordDetails = WordDetails(
                word = "Candid",
                meaningEnglish = "Truthful and straightforward; frank",
                meaningHinglish = "Nishkapat aur spasht bolne wala",
                exampleSentence = "He gave a candid interview about his mistakes.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Frank",
                    meaningEnglish = "Open, honest, and direct in speech",
                    meaningHinglish = "Khulkar sach bolne wala",
                    exampleSentence = "Let us have a frank discussion about the budget.",
                ),
                WordDetails(
                    word = "Forthright",
                    meaningEnglish = "Direct and outspoken; straightforward",
                    meaningHinglish = "Seedha aur spasht",
                    exampleSentence = "Everyone respected her forthright manner.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Evasive",
                    meaningEnglish = "Tending to avoid commitment or self-revelation",
                    meaningHinglish = "Taal-matol karne wala",
                    exampleSentence = "The witness gave evasive answers in court.",
                ),
                WordDetails(
                    word = "Deceitful",
                    meaningEnglish = "Guilty of or involving deceit; misleading",
                    meaningHinglish = "Dhokhebaaz ya chhalpoorna",
                    exampleSentence = "His deceitful behavior broke their trust.",
                ),
            ),
        ),
        VocabularyEntry(
            id = "seed_diligent",
            wordDetails = WordDetails(
                word = "Diligent",
                meaningEnglish = "Having or showing care and conscientiousness in one's work",
                meaningHinglish = "Mehanti aur lagansheel",
                exampleSentence = "Diligent students revise their notes every day.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Industrious",
                    meaningEnglish = "Diligent and hard-working",
                    meaningHinglish = "Parishrami",
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
