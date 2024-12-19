#!/bin/bash

# Fonction pour afficher la structure des répertoires
display_structure() {
    local dir_path=$1
    local indent=$2
    local prefix=$3

    # Liste les fichiers et répertoires dans le répertoire actuel
    for entry in "$dir_path"/*; do
        if [ -e "$entry" ]; then
            local name=$(basename "$entry")
            if [ -d "$entry" ]; then
                echo "${indent}${prefix}${name}/"
                display_structure "$entry" "${indent}│   " "├── "
            else
                echo "${indent}${prefix}${name}"
            fi
        fi
    done
}

# Point d'entrée du script
main() {
    local root_dir=$1

    if [ -z "$root_dir" ]; then
        echo "Usage: $0 <directory>"
        exit 1
    fi

    if [ ! -d "$root_dir" ]; then
        echo "Error: $root_dir is not a directory"
        exit 1
    fi

    echo "$(basename "$root_dir")/"
    display_structure "$root_dir" "" "├── "
}

main "$1"
