#!/usr/bin/env bash
# Checks that the French catalogs are in sync with the English sources:
#  1. catalogs are up to date (no change after extracting messages again),
#  2. no untranslated or fuzzy entry remains,
#  3. English and French builds succeed without warning.
# Requires the packages of docs/requirements.txt. Run from anywhere.
set -euo pipefail

cd "$(dirname "$0")/.."
POTDIR=_build/gettext
LOCALES=locales/fr/LC_MESSAGES

# Hash of the catalogs ignoring headers that change with versions/dates
snapshot() {
  find "$LOCALES" -name '*.po' | sort | while read -r f; do
    echo "$f"
    grep -v -E '^"(Project-Id-Version|POT-Creation-Date)' "$f"
  done | sha256sum
}

before=$(snapshot)
make gettext-update > /dev/null
after=$(snapshot)
if [[ "$before" != "$after" ]]; then
  echo "ERROR: the French catalogs are outdated. Run 'make -C docs gettext-update', translate the new entries and commit the .po files." >&2
  exit 1
fi

stat=$(sphinx-intl stat -d locales -l fr | grep ' translated, ')
echo "$stat"
if echo "$stat" | grep -v -E ' 0 fuzzy, 0 untranslated\.$' | grep -q .; then
  echo "ERROR: untranslated or fuzzy entries found (see above). Translate them or remove the fuzzy flag once reviewed." >&2
  exit 1
fi

sphinx-build -W --keep-going -q -b html . _build/html-en
sphinx-build -W --keep-going -q -b html -D language=fr . _build/html-fr
echo "Translations are up to date."
