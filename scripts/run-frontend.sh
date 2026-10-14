#!/usr/bin/env bash
set -euo pipefail

dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$dir/frontend"

if [ ! -d node_modules ]; then
  echo "Installing frontend dependencies..."
  npm install
fi

npm run dev -- "$@"
