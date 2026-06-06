#!/usr/bin/env bash
# Boots a dedicated server, waits until it is up, stops it the way a console "stop" or Ctrl+C does (SIGTERM runs the
# server's shutdown hook, which saves and stops), and reports whether it started and stopped without errors.
# Uses its own folder (run/bootcheck-server) with a superflat world, and only kills processes it started itself.
#
# Usage: [GRADLE_ARGS="-PwithCompat"] scripts/server-boot-check.sh [TIMEOUT_SECONDS]
#   GRADLE_ARGS  extra Gradle flags (see build.gradle); KEEP_WORLD=1 keeps the world from the last run.
# Log lines listed in scripts/boot-check-known.txt are not counted as errors.
set -u
DIR="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${1:-300}"
cd "$DIR" || exit 2
GAME_DIR=run/bootcheck-server
# Every process this run starts carries this tag in its environment, and cleanup kills only those: other checks,
# other projects' servers and the shared Gradle daemon are never touched.
export BOOTCHECK_TAG="$(basename "$DIR")-server-bootcheck-$$-$(date +%s)"
own_processes() {
    for d in /proc/[0-9]*; do
        p=${d#/proc/}
        [ "$p" = "$$" ] && continue
        grep -qzx "BOOTCHECK_TAG=$BOOTCHECK_TAG" "$d/environ" 2>/dev/null || continue
        tr '\0' ' ' < "$d/cmdline" 2>/dev/null | grep -q GradleDaemon && continue
        echo "$p"
    done
}
# The game itself: a java process of ours that is not Gradle (the Gradle client is java too).
own_game() {
    for p in $(own_processes); do
        tr '\0' ' ' < "/proc/$p/cmdline" 2>/dev/null | grep -qE 'gradle-wrapper|GradleWrapperMain|GradleMain' && continue
        tr '\0' ' ' < "/proc/$p/cmdline" 2>/dev/null | grep -q java && echo "$p"
    done
}

mkdir -p "$GAME_DIR"
[ "${KEEP_WORLD:-0}" = 1 ] || rm -rf "$GAME_DIR/world"
echo "eula=true" > "$GAME_DIR/eula.txt"
# A random port, so two checks (or a server someone is playing on) never collide.
PORT=$((20000 + RANDOM % 20000))
cat > "$GAME_DIR/server.properties" <<PROPS_EOF
level-name=world
level-type=minecraft\:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
spawn-monsters=false
online-mode=false
server-port=$PORT
enable-rcon=false
enable-query=false
PROPS_EOF

LOG="build/server-boot-check.log"; mkdir -p build; : > "$LOG"
GAME_LOG="$GAME_DIR/logs/latest.log"; rm -f "$GAME_LOG"
./gradlew runServerBootCheck --no-configuration-cache ${GRADLE_ARGS:-} > "$LOG" 2>&1 < /dev/null &
PID=$!
logs() { cat "$LOG" "$GAME_LOG" 2>/dev/null; }

FATAL_RE='Preparing crash report|Failed to start the minecraft server|Mixin apply failed|InvalidInjectionException'
verdict="TIMEOUT after ${TIMEOUT}s"
started=0
for _ in $(seq 1 "$TIMEOUT"); do
    if logs | grep -qE "$FATAL_RE"; then verdict="FAIL"; break; fi
    if logs | grep -qE 'Done \([0-9.]+s\)!'; then started=1; break; fi
    kill -0 $PID 2>/dev/null || { verdict="EXITED EARLY"; break; }
    sleep 1
done

stopped=0
if [ $started = 1 ]; then
    sleep 5
    game=$(own_game)
    [ -n "$game" ] && kill -TERM $game 2>/dev/null
    for _ in $(seq 1 120); do
        [ -z "$(own_game)" ] && { stopped=1; break; }
        sleep 1
    done
fi
kill $(own_processes) 2>/dev/null; sleep 2; kill -9 $(own_processes) 2>/dev/null
wait $PID 2>/dev/null

# Suspicious lines: ERROR level, an exception or error class starting a line, uncaught exceptions; minus known ones.
KNOWN="$DIR/scripts/boot-check-known.txt"
suspicious() {
    logs | grep -E '/ERROR\]|^[A-Za-z_$][A-Za-z0-9_$.]*(Exception|Error)(:|$)|Exception in thread' \
        | { if [ -f "$KNOWN" ]; then grep -vEf <(grep -vE '^[[:space:]]*(#|$)' "$KNOWN"); else cat; fi; }
}
problems=$(suspicious | sort -u)
if [ $started = 1 ]; then
    if [ $stopped = 0 ]; then verdict="FAIL (did not stop within 120 s)"
    elif ! logs | grep -q 'Stopping server'; then verdict="FAIL (stopped without 'Stopping server')"
    elif [ -n "$problems" ]; then verdict="FAIL (errors in the log)"
    else verdict="PASS"; fi
fi
echo "SERVER BOOTCHECK: $verdict"
[ -n "$problems" ] && echo "$problems" | head -20
logs | grep -E 'Done \(' | head -1
echo "logs: $LOG, $GAME_LOG"
exit $([ "$verdict" = PASS ] && echo 0 || echo 1)
