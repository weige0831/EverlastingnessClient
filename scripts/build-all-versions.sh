#!/usr/bin/env bash
#
# build-all-versions.sh — builds and stages the Everlastingness client jar
# for every Lunar-supported Minecraft version.
#
# Usage:
#   ./scripts/build-all-versions.sh           # build everything
#   ./scripts/build-all-versions.sh modern    # only modern (1.16.5+)
#   ./scripts/build-all-versions.sh legacy    # only legacy (1.7.10–1.12.2)
#
# This script orchestrates the per-era Gradle builds, parameterising the
# modern build by MC version (updating gradle.properties before each build)
# and the legacy build by subproject. Output jars are staged to
# ~/.everlastingness/client/everlastingness-<version>.jar.
#
# NOTE: each modern version requires its own Loom resolve (downloads MC +
# Yarn mappings), which is network-bound and slow (~2-5 min per version).
# Set EVERLASTINGNESS_PARALLEL=1 to build multiple versions concurrently.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLIENT_DIR="${EVERLASTINGNESS_HOME:-$HOME/.everlastingness}/client"
mkdir -p "$CLIENT_DIR"

# ---- version lists (mirror ClientProfiles.BuildAllVersions) ----
LEGACY_VERSIONS=("1.7.10" "1.8.9" "1.9.4" "1.11.2" "1.12.2")
MODERN_VERSIONS=(
  "1.16.5" "1.17.1" "1.18.1" "1.18.2"
  "1.19" "1.19.2" "1.19.3" "1.19.4"
  "1.20" "1.20.1" "1.20.2" "1.20.3" "1.20.4" "1.20.5" "1.20.6"
  "1.21" "1.21.1" "1.21.3" "1.21.4" "1.21.5"
  "1.21.6" "1.21.7" "1.21.8" "1.21.9" "1.21.10" "1.21.11"
  "26.1" "26.2"
)

# Yarn mapping versions. The key is the MC version; the value is the Yarn
# build tag. For versions not yet pinned, the script fetches the latest Yarn
# build via the Fabric API at build time.
declare -A YARN_BUILDS=(
  ["1.16.5"]="1.16.5+build.10"
  ["1.17.1"]="1.17.1+build.65"
  ["1.18.1"]="1.18.1+build.22"
  ["1.18.2"]="1.18.2+build.4"
  ["1.19"]="1.19+build.4"
  ["1.19.2"]="1.19.2+build.28"
  ["1.19.3"]="1.19.3+build.5"
  ["1.19.4"]="1.19.4+build.2"
  ["1.20"]="1.20+build.1"
  ["1.20.1"]="1.20.1+build.10"
  ["1.20.2"]="1.20.2+build.4"
  ["1.20.3"]="1.20.3+build.1"
  ["1.20.4"]="1.20.4+build.3"
  ["1.20.5"]="1.20.5+build.1"
  ["1.20.6"]="1.20.6+build.3"
  ["1.21"]="1.21+build.2"
  ["1.21.1"]="1.21.1+build.3"
  ["1.21.3"]="1.21.3+build.2"
  ["1.21.4"]="1.21.4+build.8"
  ["1.21.5"]="1.21.5+build.1"
)

stage_jar() {
  local src="$1" version="$2"
  local dest="$CLIENT_DIR/everlastingness-$version.jar"
  if [ -f "$src" ]; then
    cp "$src" "$dest"
    echo "  ✓ staged $dest ($(du -h "$dest" | cut -f1))"
  else
    echo "  ✗ build did not produce $src — skipping"
    return 1
  fi
}

build_legacy() {
  local version="$1"
  echo "=== Legacy build: $version ==="
  cd "$REPO_ROOT/client"
  # Each legacy version has its own subproject (v1_7_10 exists; others are
  # created on first build via the template). The build produces a reobf jar
  # + runs MixinTargetPatcher for the notch-remap pass.
  local subproject
  case "$version" in
    1.7.10) subproject="v1_7_10" ;;
    1.8.9)  subproject="v1_8_9" ;;
    1.9.4)  subproject="v1_9_4" ;;
    1.11.2) subproject="v1_11_2" ;;
    1.12.2) subproject="v1_12_2" ;;
  esac
  export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/jdk1.8.0_202}"
  if ./gradlew ":${subproject}:patchMixinTargets" -PmcVersion="$version" 2>&1 | grep -qE "BUILD SUCCESSFUL"; then
    stage_jar "$REPO_ROOT/client/$subproject/build/libs/$subproject-1.0.0-SNAPSHOT.jar" "$version"
  else
    echo "  ✗ legacy build failed for $version"
    return 1
  fi
}

build_modern() {
  local version="$1"
  echo "=== Modern build: $version ==="
  cd "$REPO_ROOT/client-modern"
  # Gradle 9.5.1 (required by Loom 1.17.x) needs JDK 17+.
  export JAVA_HOME="${JAVA_HOME_21:-/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot}"
  # Update gradle.properties for this MC version.
  local yarn="${YARN_BUILDS[$version]:-}"
  if [ -z "$yarn" ]; then
    # Fetch latest Yarn build for this MC version from the Fabric API.
    yarn=$(curl -fsSL "https://meta.fabricmc.net/v2/versions/yarn/$version" \
      | grep -oE '"build":[0-9]+' | head -1 | grep -oE '[0-9]+')
    if [ -n "$yarn" ]; then
      yarn="$version+build.$yarn"
    else
      echo "  ✗ no Yarn build found for $version — skipping"
      return 1
    fi
  fi
  echo "  yarn = $yarn"
  # Patch gradle.properties in-place.
  sed -i.bak \
    -e "s/^minecraft_version=.*/minecraft_version=$version/" \
    -e "s/^yarn_mappings=.*/yarn_mappings=$yarn/" \
    v1_20_x/gradle.properties
  # The base archives name should reflect the version.
  sed -i.bak2 "s/archivesName.set(\"everlastingness-[^\"]*\")/archivesName.set(\"everlastingness-$version\")/" \
    v1_20_x/build.gradle.kts
  # Build the agent jar (Loom resolves MC + Yarn for this version).
  if ./gradlew :v1_20_x:build 2>&1 | grep -qE "BUILD SUCCESSFUL"; then
    stage_jar "$REPO_ROOT/client-modern/v1_20_x/build/libs/everlastingness-$version-1.0.0-agent.jar" "$version"
  else
    echo "  ✗ modern build failed for $version"
    return 1
  fi
}

main() {
  local mode="${1:-all}"
  local failed=() succeeded=()
  case "$mode" in
    legacy)
      for v in "${LEGACY_VERSIONS[@]}"; do
        if build_legacy "$v"; then succeeded+=("$v"); else failed+=("$v"); fi
      done
      ;;
    modern)
      for v in "${MODERN_VERSIONS[@]}"; do
        if build_modern "$v"; then succeeded+=("$v"); else failed+=("$v"); fi
      done
      ;;
    all)
      for v in "${LEGACY_VERSIONS[@]}"; do
        if build_legacy "$v"; then succeeded+=("$v"); else failed+=("$v"); fi
      done
      for v in "${MODERN_VERSIONS[@]}"; do
        if build_modern "$v"; then succeeded+=("$v"); else failed+=("$v"); fi
      done
      ;;
    *)
      echo "Usage: $0 [all|modern|legacy]"; exit 1 ;;
  esac
  echo ""
  echo "=== BUILD SUMMARY ==="
  echo "  Succeeded (${#succeeded[@]}): ${succeeded[*]:-none}"
  echo "  Failed    (${#failed[@]}): ${failed[*]:-none}"
  echo ""
  echo "Staged jars in $CLIENT_DIR:"
  ls -1 "$CLIENT_DIR"/everlastingness-*.jar 2>/dev/null | sed 's|.*/||' || echo "  (none)"
}

main "$@"
