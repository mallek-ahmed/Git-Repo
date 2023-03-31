#!/bin/bash

# Configuration des variables
API_URL="https://example.com/api/endpoint"
USERNAME="votre_nom_utilisateur"
PASSWORD="votre_mot_de_passe"
CONTENT_TYPE="application/json"

# Paramètres à passer dans l'appel
PARAM1="valeur1"
PARAM2="valeur2"

# Authentification
AUTH=$(echo -n "$USERNAME:$PASSWORD" | base64)

# Construction de la requête
REQ_DATA=$(cat <<-END
{
  "parametre1": "${PARAM1}",
  "parametre2": "${PARAM2}"
}
END
)

# Appel du Webservice REST et stockage du résultat dans une variable
RESPONSE=$(curl -s -X POST "${API_URL}" \
  -H "Authorization: Basic ${AUTH}" \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d "${REQ_DATA}")

# Exploitation du résultat de la requête
# Utilisation de 'grep' et 'sed' pour extraire des informations du JSON
RESULTAT=$(echo "${RESPONSE}" | grep -oP '"votre_champ":\s*"\K[^"]+')

# Affichage du résultat
echo "Le résultat de la requête est : ${RESULTAT}"
