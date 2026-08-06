#!/usr/bin/env bash
# Everlastingness — staging helper for real-MC end-to-end verification.
#
# Copies the built 1.7.10 client jar and the Mixin/LaunchWrapper runtime jars
# into the launcher's client-resources directory, so the launcher can assemble
# the injected classpath exactly as ClientProfiles expects.
#
# Run AFTER building (./gradlew :v1_7_10:reobfJar). Produces a manifest of
# what was staged so you can confirm before launching.
#
# Usage:
#   bash scripts/stage-client-1.7.10.sh
#
# Env:
#   EVERLASTINGNESS_HOME  (optional) overrides ~/.everlastingness

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOME_DIR="${EVERLASTINGNESS_HOME:-$HOME/.everlastingness}"
CLIENT_DIR="$HOME_DIR/client"
mkdir -p "$CLIENT_DIR"

# 1) The reobfuscated client jar built by RFG. The launcher's profile names it
#    "everlastingness-1.7.10.jar" — copy under that exact name.
SRC_JAR="$REPO_ROOT/client/v1_7_10/build/libs/v1_7_10-1.0.0-SNAPSHOT.jar"
if [[ ! -f "$SRC_JAR" ]]; then
  echo "ERROR: client jar not found at $SRC_JAR" >&2
  echo "       Run 'cd client && ./gradlew :v1_7_10:reobfJar' first." >&2
  exit 1
fi
cp "$SRC_JAR" "$CLIENT_DIR/everlastingness-1.7.10.jar"

# 2) The Mixin + LaunchWrapper runtime jars the launcher places on the injected
#    classpath (see ClientProfiles Legacy profile). These are declared by name
#    in the launcher; they must be present for injection to bootstrap.
declare -a RUNTIME_JARS=(
  "mixin-0.8.7.jar|https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/0.8.7/mixin-0.8.7.jar"
  "launchwrapper-1.12.jar|https://repo.spongepowered.org/repository/maven-public/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar"
)

staged=()
for entry in "${RUNTIME_JARS[@]}"; do
  name="${entry%%|*}"; url="${entry##*|}"
  dest="$CLIENT_DIR/$name"
  if [[ -f "$dest" ]]; then
    staged+=("ok  (existing)  $name")
    continue
  fi
  echo "Downloading $name ..."
  if curl -fsSL -o "$dest" "$url"; then
    staged+=("ok  (downloaded) $name")
  else
    staged+=("FAIL (download)  $name  <-  $url")
  fi
done

echo
echo "=== staged to $CLIENT_DIR ==="
ls -la "$CLIENT_DIR"
echo
echo "=== runtime jar status ==="
printf '%s\n' "${staged[@]}"
echo
echo "Next: open the launcher, select 1.7.10, ensure 'inject' is checked, and Launch."
echo "Watch the launcher's 'Game log' panel for the six [Everlastingness] checkpoints"
echo "listed in docs/e2e-verification-steps.md (Step 5)."
