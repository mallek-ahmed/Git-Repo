#!/bin/bash

# Répertoire cible (par défaut: répertoire courant)
BASE_DIR="${1:-.}"

# Extensions ciblées
EXTENSIONS=("*.logo" "*.out")

# Motifs à rechercher (exceptions Java/Oracle)
INCLUDE_PATTERNS=(
  "Exception"
  "ORA-[0-9]\{5\}"
  "java\.lang\."
)

# Motifs à exclure (exceptions connues)
EXCLUDE_PATTERNS=(
  "ORA-00001"         # Duplicate key
  "java\.lang\.NumberFormatException"
  "java\.lang\.IllegalArgumentException: Invalid status"
)

# Fonction : construit les options grep -E avec exclusion
build_exclude_pattern() {
  local pattern=""
  for excl in "${EXCLUDE_PATTERNS[@]}"; do
    pattern+="|$excl"
  done
  echo "${pattern:1}"  # retire le premier "|"
}

echo "🔍 Recherche d'exceptions dans $BASE_DIR (fichiers .logo et .out)..."

# Construit le motif d’exclusion
EXCLUDE_REGEX=$(build_exclude_pattern)

# Parcours les fichiers par extensions
for ext in "${EXTENSIONS[@]}"; do
  find "$BASE_DIR" -type f -name "$ext"
done | while read -r file; do
  for pattern in "${INCLUDE_PATTERNS[@]}"; do
    grep -E "$pattern" "$file" | grep -Ev "$EXCLUDE_REGEX" | while read -r line; do
      echo -e "\n📄 Fichier : $file"
      echo "👉 $line"
    done
  done
done
