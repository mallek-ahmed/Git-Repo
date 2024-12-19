#!/bin/bash

# Liste des répertoires à exclure
EXCLUDED_DIRS=("__pycache__")

# Liste des fichiers à exclure
EXCLUDED_FILES=("file1.txt" "file2.txt")

# Liste des extensions à exclure
EXCLUDED_EXTENSIONS=("pyc" "cfg")

# Fonction pour vérifier si un élément est dans une liste
element_in_list() {
    local element="$1"
    shift
    local list=("$@")

    for item in "${list[@]}"; do
        if [ "$element" == "$item" ]; then
            return 0
        fi
    done

    return 1
}

# Fonction pour parcourir un répertoire et ses sous-répertoires
parcourir_repertoire() {
    local rep="$1"

    # Parcourir les fichiers et répertoires du répertoire actuel
    for chemin in "$rep"/*; do
        # Vérifier si le chemin existe (éviter les erreurs si le répertoire est vide)
        [ -e "$chemin" ] || continue

        # Récupérer le nom de base du chemin
        nom_base="$(basename "$chemin")"

        # Si c'est un répertoire et qu'il est dans la liste des exclusions, passer au suivant
        if [ -d "$chemin" ] && element_in_list "$nom_base" "${EXCLUDED_DIRS[@]}"; then
            continue
        fi

        # Si c'est un fichier et qu'il est dans la liste des exclusions, passer au suivant
        if [ -f "$chemin" ] && element_in_list "$nom_base" "${EXCLUDED_FILES[@]}"; then
            continue
        fi

        # Si c'est un fichier et que son extension est dans la liste des exclusions, passer au suivant
        extension="${nom_base##*.}"
        if [ -f "$chemin" ] && element_in_list "$extension" "${EXCLUDED_EXTENSIONS[@]}"; then
            continue
        fi

        if [ -d "$chemin" ]; then
            # Si c'est un répertoire, appeler récursivement la fonction
            parcourir_repertoire "$chemin"
        elif [ -f "$chemin" ]; then
            # Si c'est un fichier, afficher le chemin relatif et le contenu du fichier
            chemin_relatif="${chemin#$initial_rep/}"
            echo "Chemin relatif et nom du fichier : $chemin_relatif"
            echo "###$chemin_relatif"
            cat "$chemin"
            echo "###"
			echo "  "
        fi
    done
}

# Appeler la fonction avec le répertoire courant ou un répertoire spécifié en argument
repertoire="${1:-.}"
# Résoudre le chemin absolu du répertoire initial pour un calcul correct des chemins relatifs
initial_rep="$(realpath "$repertoire")"
parcourir_repertoire "$repertoire"
