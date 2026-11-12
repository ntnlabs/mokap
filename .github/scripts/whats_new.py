#!/usr/bin/env python3
"""„Čo je nové“ pre Play Console: overí docs/play/whats-new.txt (blok <sk>, ≤ 500 znakov). S --summary
ho zapíše do súhrnu behu v GitHub Actions, nech sa dá pri nahrávaní AAB rovno skopírovať do Release notes."""
import os
import re
import sys

PATH = "docs/play/whats-new.txt"
LIMIT = 500

text = open(PATH, encoding="utf-8").read().strip()
blocks = re.findall(r"<([a-z]{2}(?:-[A-Z]{2})?)>\n(.*?)\n</\1>", text, re.S)
if not blocks:
    print(f"::error file={PATH}::Chýba jazykový blok <sk>…</sk>")
    sys.exit(1)
too_long = False
for lang, body in blocks:
    print(f"{lang}: {len(body)}/{LIMIT} znakov")
    if len(body) > LIMIT:
        print(f"::error file={PATH}::Čo je nové ({lang}) má {len(body)} znakov, Play dovolí {LIMIT}")
        too_long = True
summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary and "--summary" in sys.argv[1:]:
    with open(summary, "a", encoding="utf-8") as f:
        f.write("### Čo je nové – skopíruj do Play Console → Release notes\n\n```\n" + text + "\n```\n")
sys.exit(1 if too_long else 0)
