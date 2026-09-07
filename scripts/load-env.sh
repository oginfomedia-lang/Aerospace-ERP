#!/usr/bin/env bash
# Loads secrets from a local, gitignored .env file (repo root) into the current
# shell's environment, then optionally launches whatever command follows.
#
# Usage:
#   source scripts/load-env.sh                    # load vars into this shell only
#   scripts/load-env.sh mvn spring-boot:run        # load vars, then run a command
#
# This is the same KEY=VALUE loading model a systemd EnvironmentFile= or a
# Docker env_file: would use later — .env stays the one source of truth
# regardless of which of those you eventually wrap this in.

set -a
ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE — copy .env.example to .env and fill in real values first." >&2
  return 1 2>/dev/null || exit 1
fi
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if [ "$#" -gt 0 ]; then
  exec "$@"
fi
