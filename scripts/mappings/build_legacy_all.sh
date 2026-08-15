#!/usr/bin/env bash
# Rebuild legacy (1.8.9-1.12.2) jars from TODAY's source via LegacyFabric yarn,
# then run the obf-rewrite pipeline on them.
set -u
MAPDIR="/c/Users/songd/mappings"
CLIENT_DIR="F:/zcode/EverlastingnessClient/client-modern"
DEPLOY="C:/Users/songd/.everlastingness/client"
JAR="/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot/bin/jar.exe"
export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot"

cd "$CLIENT_DIR" || exit 1
# switch mappings to legacyfabric
sed -i 's|mappings "net.fabricmc:yarn:${property("yarn_mappings")}:v2"|mappings "net.legacyfabric:yarn:${property("yarn_mappings")}:v2"|' v1_20_x/build.gradle

for ver in "$@"; do
  echo "=== $ver (legacyfabric yarn) ==="
  sed -i -e "s|^minecraft_version=.*|minecraft_version=$ver|" -e "s|^yarn_mappings=.*|yarn_mappings=$ver+build.604|" gradle.properties
  ./gradlew :v1_20_x:remapJar --no-daemon -q >/dev/null 2>&1 || { echo "GRADLE FAIL $ver"; continue; }
  built="v1_20_x/build/libs/everlastingness-$ver-1.0.0.jar"
  [ -f "$built" ] || { echo "JAR MISSING $ver"; continue; }
  work=$(mktemp -d)
  (cd "$work" && "$JAR" xf "$CLIENT_DIR/$built")
  rm -f "$DEPLOY/everlastingness-$ver.jar"
  (cd "$work" && "$JAR" cfm "$DEPLOY/everlastingness-$ver.jar" "$MAPDIR/agent-manifest.mf" .)
  python "$MAPDIR/make_refmap_obf.py" "$(cygpath -w "$work")" "$MAPDIR" "$ver" || true
  python "$MAPDIR/obf_rewrite.py" "$MAPDIR/inter2obf-$ver.json" \
      "C:\Users\songd\.everlastingness\client\everlastingness-$ver.jar" \
      "C:\Users\songd\mappings\everlastingness-$ver-obf.jar" || { echo "REWRITE FAIL $ver"; continue; }
  cp "$MAPDIR/everlastingness-$ver-obf.jar" "$DEPLOY/everlastingness-$ver.jar"
  rm -rf "$work"
  echo "OK $ver -> $(stat -c%s "$DEPLOY/everlastingness-$ver.jar") bytes"
done

# restore modern mappings line
sed -i 's|mappings "net.legacyfabric:yarn:${property("yarn_mappings")}:v2"|mappings "net.fabricmc:yarn:${property("yarn_mappings")}:v2"|' v1_20_x/build.gradle
