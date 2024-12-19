#!/bin/bash

# Fonction pour vérifier si un élément est dans une liste
element_in_list() {
    local element="$1"
    shift
    local list=("$@")

    for item in "${list[@]}"; do
        if [[ "$element" == "$item" ]]; then
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
        # Vérifier si le chemin existe (pour éviter les erreurs si le répertoire est vide)
        [ -e "$chemin" ] || continue

        # Récupérer le nom de base du chemin
        nom_base="$(basename "$chemin")"

        # Si c'est un répertoire et qu'il est dans la liste des exclusions, passer au suivant
        if [[ -d "$chemin" ]] && element_in_list "$nom_base" "${EXCLUDED_DIRS[@]}"; then
            continue
        fi

        # Si c'est un fichier et qu'il est dans la liste des exclusions, passer au suivant
        if [[ -f "$chemin" ]] && element_in_list "$nom_base" "${EXCLUDED_FILES[@]}"; then
            continue
        fi

        # Si c'est un fichier et que son extension est dans la liste des exclusions, passer au suivant
        extension="${nom_base##*.}"
        if [[ -f "$chemin" ]] && element_in_list "$extension" "${EXCLUDED_EXTENSIONS[@]}"; then
            continue
        fi

        if [[ -d "$chemin" ]]; then
            # Si c'est un répertoire, appeler récursivement la fonction
            parcourir_repertoire "$chemin"
        elif [[ -f "$chemin" ]]; then
            # Si c'est un fichier, afficher le chemin relatif et le contenu du fichier
            chemin_relatif="${chemin#$initial_rep/}"
            echo "Chemin relatif et nom du fichier : $chemin_relatif" | tee -a "$OUTPUT_FILE"
            echo "###$chemin_relatif" | tee -a "$OUTPUT_FILE"
            cat "$chemin" | tee -a "$OUTPUT_FILE"
            echo "" | tee -a "$OUTPUT_FILE"
            echo "###" | tee -a "$OUTPUT_FILE"
            echo "  " | tee -a "$OUTPUT_FILE"
        fi
    done
}

# Afficher une barre de progression
afficher_progression() {
    local total_files=$(find "$repertoire" -type f | wc -l)
    local current_file=0

    parcourir_repertoire_progress() {
        local rep="$1"

        for chemin in "$rep"/*; do
            [ -e "$chemin" ] || continue

            nom_base="$(basename "$chemin")"
            if [[ -d "$chemin" ]] && element_in_list "$nom_base" "${EXCLUDED_DIRS[@]}"; then
                continue
            fi

            if [[ -f "$chemin" ]] && element_in_list "$nom_base" "${EXCLUDED_FILES[@]}"; then
                continue
            fi

            extension="${nom_base##*.}"
            if [[ -f "$chemin" ]] && element_in_list "$extension" "${EXCLUDED_EXTENSIONS[@]}"; then
                continue
            fi

            if [[ -d "$chemin" ]]; then
                parcourir_repertoire_progress "$chemin"
            elif [[ -f "$chemin" ]]; then
                current_file=$((current_file + 1))
                echo -ne "Progression : $current_file / $total_files fichiers\r"
                chemin_relatif="${chemin#$initial_rep/}"
                echo "Chemin relatif et nom du fichier : $chemin_relatif" | tee -a "$OUTPUT_FILE"
                echo "###$chemin_relatif" | tee -a "$OUTPUT_FILE"
                cat "$chemin" | tee -a "$OUTPUT_FILE"
				echo "" | tee -a "$OUTPUT_FILE"
                echo "###" | tee -a "$OUTPUT_FILE"
                echo "  " | tee -a "$OUTPUT_FILE"
            fi
        done
    }

    parcourir_repertoire_progress "$repertoire"
}

# Demander à l'utilisateur de spécifier le répertoire à parcourir
echo -n "Veuillez entrer le chemin du répertoire à parcourir : "
read -r repertoire
initial_rep="$(realpath "$repertoire")"

# Demander à l'utilisateur de spécifier l'emplacement du fichier de sortie
echo -n "Veuillez entrer le chemin du fichier de sortie (par défaut : output.txt) : "
read -r output_file_path
OUTPUT_FILE="${output_file_path:-$(pwd)/resultat.txt}"

# Demander à l'utilisateur de spécifier les extensions de fichiers à inclure
echo -n "Veuillez entrer les extensions de fichiers à inclure (séparées par des espaces, laissez vide pour tout inclure) : "
read -r -a INCLUDED_EXTENSIONS

# Demander à l'utilisateur de spécifier les extensions de fichiers à exclure
echo -n "Veuillez entrer les extensions de fichiers à exclure (séparées par des espaces, laissez vide pour aucune exclusion) : "
read -r -a EXCLUDED_EXTENSIONS

# Demander à l'utilisateur de spécifier les répertoires à exclure
echo -n "Veuillez entrer les sous-répertoires à exclure (séparés par des espaces, laissez vide pour aucun) : "
read -r -a EXCLUDED_DIRS

# Vider le fichier de sortie avant de commencer
> "$OUTPUT_FILE"

# Appeler la fonction d'affichage de la progression
afficher_progression

echo -e "\nTraitement terminé. Les résultats sont disponibles dans le fichier : $OUTPUT_FILE"
