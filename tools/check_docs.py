#!/usr/bin/env python3
"""Fails when the docs mention code, files or numbers that no longer exist (keeps AI-written docs honest).

Run from the repository root: python3 tools/check_docs.py
Checks (pure standard library, takes a second):
1. Every `CamelCaseName` or `path/file.ext` written in backticks in AGENTS.md, README.md or docs/*.md must exist in the
   code, the Gradle/CI files, the Firestore rules or the list of tracked files.
2. The synonym/antonym limit in `VocabularyRules.MAX_RELATIONS` must equal the one in `firestore.rules`.
"""
import glob
import re
import subprocess
import sys

DOCS = ["AGENTS.md", "README.md"] + sorted(glob.glob("docs/*.md"))
# Names that are deliberately not in this repository: placeholders and platform/library names.
ALLOWED = {
    "XScreen.kt", "XViewModel.kt", "app/google-services.json",
    "FrameTimingMetric", "StartupTimingMetric", "RenderEffect",
}

try:
    tracked = subprocess.check_output(["git", "ls-files"], text=True, stderr=subprocess.DEVNULL).split("\n")
except Exception:
    tracked = [
        p for p in (glob.glob("**/*", recursive=True) + glob.glob(".github/**/*", recursive=True))
        if not p.startswith(("build/", "app/build/", ".gradle/", ".aistudio/"))
    ]
tracked = [t for t in tracked if t]
stems = {t.split("/")[-1].rsplit(".", 1)[0] for t in tracked} | {t.split("/")[-1] for t in tracked}

corpus = ""
for pattern in ("app/src/**/*.kt", "app/src/**/*.xml", "*.kts", "app/*.kts", "tools/*", ".github/workflows/*"):
    for path in glob.glob(pattern, recursive=True):
        corpus += open(path, errors="ignore").read() + "\n"
corpus += open("firestore.rules").read()

problems = []
for doc in DOCS:
    text = open(doc).read()
    for token in sorted(set(re.findall(r"`([^`\n]{3,80})`", text))):
        if token in ALLOWED:
            continue
        if re.fullmatch(r"[\w./-]+\.(kt|md|py|xml|json|yml|kts|ttf|svg|txt|html)", token):
            if token not in tracked and token.split("/")[-1] not in stems and token not in corpus:
                problems.append(f"{doc}: file `{token}` does not exist")
            continue
        m = re.fullmatch(r"([A-Z][A-Za-z0-9]{4,})(?:\.[A-Za-z_]\w*)?(?:\(.*\))?", token)
        if m and m.group(1) not in ALLOWED and m.group(1) not in corpus and m.group(1) not in stems:
            problems.append(f"{doc}: `{token}` is not in the code any more")

kotlin_limit = re.search(r"const val MAX_RELATIONS = (\d+)", corpus)
rules_limits = set(re.findall(r"(?:synonyms|antonyms)\.size\(\) <= (\d+)", corpus))
if not kotlin_limit or rules_limits != {kotlin_limit.group(1)}:
    problems.append(f"MAX_RELATIONS ({kotlin_limit and kotlin_limit.group(1)}) differs from firestore.rules {rules_limits}")

word_limit = re.search(r"const val MAX_WORD_LENGTH = (\d+)", corpus)
rules_word = set(re.findall(r"details\.word\.size\(\) <= (\d+)", corpus))
if not word_limit or rules_word != {word_limit.group(1)}:
    problems.append(f"MAX_WORD_LENGTH ({word_limit and word_limit.group(1)}) differs from firestore.rules {rules_word}")

meaning_limit = re.search(r"const val MAX_MEANING_LENGTH = (\d+)", corpus)
rules_meaning = set(re.findall(r"details\.meaning(?:English|Hinglish)\.size\(\) <= (\d+)", corpus))
if not meaning_limit or rules_meaning != {meaning_limit.group(1)}:
    problems.append(f"MAX_MEANING_LENGTH ({meaning_limit and meaning_limit.group(1)}) differs from firestore.rules {rules_meaning}")

example_limit = re.search(r"const val MAX_EXAMPLE_LENGTH = (\d+)", corpus)
rules_example = set(re.findall(r"details\.exampleSentence\.size\(\) <= (\d+)", corpus))
if not example_limit or rules_example != {example_limit.group(1)}:
    problems.append(f"MAX_EXAMPLE_LENGTH ({example_limit and example_limit.group(1)}) differs from firestore.rules {rules_example}")

if problems:
    print("Docs are out of date:\n  " + "\n  ".join(problems))
    sys.exit(1)
print("Docs check passed.")
