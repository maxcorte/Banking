#!/usr/bin/env bash
set -euo pipefail

# Le script vit à la racine du projet (à côté du docker-compose).
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Charge .env pour récupérer DB_USER / DB_NAME.
set -a
[ -f .env ] && . ./.env
set +a
DBU="${DB_USER:-banking}"
DBN="${DB_NAME:-banking}"

BACKUP_DIR="$PROJECT_DIR/backups"
mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$BACKUP_DIR/banking-$STAMP.sql.gz"

# Dump compressé de la base, depuis le conteneur.
docker exec banking-db pg_dump -U "$DBU" "$DBN" | gzip > "$OUT"

# Rotation : ne garder que les 7 sauvegardes les plus récentes.
ls -1t "$BACKUP_DIR"/banking-*.sql.gz 2>/dev/null | tail -n +8 | xargs -r rm --

echo "$(date '+%F %T')  Sauvegarde OK : $(basename "$OUT")"
