#!/usr/bin/env bash
# Rebuild every modern-era jar through the obf-rewrite pipeline.
# Usage: build_modern_all.sh <ver> ...
set -u
MAPDIR="/c/Users/songd/mappings"
CLIENT_DIR="F:/zcode/EverlastingnessClient/client-modern"
DEPLOY="C:/Users/songd/.everlastingness/client"
JAR="/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot/bin/jar.exe"
export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot"

# Yarn mapping coordinates per version (same table as the earlier build scripts)
declare -A YARN=(
 [1.16.5]="1.16.5+build.10" [1.17.1]="1.17.1+build.65" [1.18.1]="1.18.1+build.22"
 [1.18.2]="1.18.2+build.4" [1.19]="1.19+build.4" [1.19.2]="1.19.2+build.28"
 [1.19.3]="1.19.3+build.5" [1.19.4]="1.19.4+build.2" [1.20]="1.20+build.1"
 [1.20.1]="1.20.1+build.10" [1.20.2]="1.20.2+build.4" [1.20.3]="1.20.3+build.1"
 [1.20.4]="1.20.4+build.3" [1.20.5]="1.20.5+build.1" [1.20.6]="1.20.6+build.3"
 [1.21]="1.21+build.9" [1.21.1]="1.21.1+build.3" [1.21.3]="1.21.3+build.2"
 [1.21.4]="1.21.4+build.8" [1.21.5]="1.21.5+build.1" [1.21.6]="1.21.6+build.1"
 [1.21.7]="1.21.7+build.1" [1.21.8]="1.21.8+build.1" [1.21.9]="1.21.9+build.1"
 [1.21.10]="1.21.10+build.1" [1.21.11]="1.21.11+build.1"
)

for ver in "$@"; do
  y="${YARN[$ver]}"
  echo "=== $ver (yarn $y) ==="
  cd "$CLIENT_DIR" || exit 1
  sed -i -e "s|^minecraft_version=.*|minecraft_version=$ver|" -e "s|^yarn_mappings=.*|yarn_mappings=$y|" gradle.properties
  ./gradlew :v1_20_x:remapJar --no-daemon -q >/dev/null 2>&1 || { echo "GRADLE FAIL $ver"; continue; }
  built="v1_20_x/build/libs/everlastingness-1.20.1-1.0.0.jar"
  [ -f "$built" ] || built="v1_20_x/build/libs/everlastingness-$ver-1.0.0.jar"
  [ -f "$built" ] || { echo "JAR MISSING $ver"; continue; }
  work=$(mktemp -d)
  (cd "$work" && "$JAR" xf "$CLIENT_DIR/$built")
  rm -f "$DEPLOY/everlastingness-$ver.jar"
  (cd "$work" && "$JAR" cfm "$DEPLOY/everlastingness-$ver.jar" "$MAPDIR/agent-manifest.mf" .)
  # refmap-obf for this version
  python "$MAPDIR/make_refmap_obf.py" "$work" "$MAPDIR" "$ver" || true
  python "$MAPDIR/obf_rewrite.py" "$MAPDIR/inter2obf-$ver.json" \
      "C:\\Users\\songd\\.everlastingness\\client\\everlastingness-$ver.jar" \
      "C:\\Users\\songd\\mappings\\everlastingness-$ver-obf.jar" || { echo "REWRITE FAIL $ver"; continue; }
  cp "$MAPDIR/everlastingness-$ver-obf.jar" "$DEPLOY/everlastingness-$ver.jar"
  rm -rf "$work"
  echo "OK $ver -> $(stat -c%s "$DEPLOY/everlastingness-$ver.jar") bytes"
done
