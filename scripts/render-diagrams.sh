#!/usr/bin/env bash
# Renders PlantUML blocks embedded in docs/**/*.md to SVG files in a sibling
# assets/ directory, and stores a checksum of the PlantUML source so CI can
# detect stale SVGs without depending on byte-identical rendering across OSes.
#
# Usage:
#   scripts/render-diagrams.sh          # render SVGs + update checksums
#   scripts/render-diagrams.sh --check  # CI mode: validate syntax and fail if
#                                       # a committed SVG is out of date
#
# Requirements: java (17+), curl. Graphviz is NOT required (Smetana layout).
set -euo pipefail
cd "$(dirname "$0")/.."

# Keep in sync with the cache key in .github/workflows/ci.yml
PLANTUML_VERSION="1.2026.6"
CACHE_DIR="${PLANTUML_CACHE_DIR:-$HOME/.cache/plantuml}"
JAR="$CACHE_DIR/plantuml-$PLANTUML_VERSION.jar"
JAR_URL="https://github.com/plantuml/plantuml/releases/download/v$PLANTUML_VERSION/plantuml-$PLANTUML_VERSION.jar"

if [ ! -f "$JAR" ]; then
    echo "Downloading PlantUML $PLANTUML_VERSION to $CACHE_DIR ..."
    mkdir -p "$CACHE_DIR"
    curl -fsSL -o "$JAR" "$JAR_URL"
fi

render() { # $1 = markdown file, $2 = output dir
    java -jar "$JAR" -tsvg -charset UTF-8 -Playout=smetana -o "$2" "$1"
}

sources_hash() { # sha256 of all ```plantuml blocks in a markdown file
    # sub(/\r$/,"") normalizes CRLF checkouts (Windows, core.autocrlf) so the
    # hash is identical across platforms.
    awk '{ sub(/\r$/, "") } /^```plantuml$/{f=1;next} /^```$/{f=0} f' "$1" | sha256sum | cut -d' ' -f1
}

check_mode=false
[ "${1:-}" = "--check" ] && check_mode=true

status=0
shopt -s globstar nullglob
for f in docs/**/*.md; do
    grep -q '^```plantuml' "$f" || continue
    dir="$(dirname "$f")"
    hash_file="$dir/assets/$(basename "$f").plantuml.sha256"
    current_hash="$(sources_hash "$f")"

    if $check_mode; then
        tmp="$(mktemp -d)"
        if ! render "$f" "$tmp"; then
            echo "::error file=$f::Invalid PlantUML block"
            status=1
        fi
        rm -rf "$tmp"
        stored_hash="$(tr -d ' \r\n' < "$hash_file" 2>/dev/null || echo "missing")"
        if [ "$current_hash" != "$stored_hash" ]; then
            echo "::error file=$f::Stale SVG — run scripts/render-diagrams.sh and commit the assets/ changes"
            status=1
        fi
    else
        echo "Rendering $f -> $dir/assets/"
        mkdir -p "$dir/assets"
        render "$f" "$(pwd)/$dir/assets"
        echo "$current_hash" > "$hash_file"
    fi
done

if $check_mode && [ $status -eq 0 ]; then
    echo "All PlantUML diagrams are valid and their SVGs are up to date."
fi
exit $status
