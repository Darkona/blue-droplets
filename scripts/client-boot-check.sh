#!/usr/bin/env bash
# Boots the real client headless (Xvfb + Mesa software GL) into a copy of a superflat test world and reports whether it
# joined, ran the commands and left the log without errors. No screenshot: the check is the log.
# Adapted from Green Feathers' scripts/client-boot-check.sh for 1.18.2. Minecraft 1.18.2 can't open a singleplayer world
# from the command line (no --quickPlaySingleplayer), so a dedicated server of its own (run/bootserver, a free port from
# 25699) hosts the world and the client connects to it on startup (--server/--port).
#
# Usage: [GRADLE_ARGS="-PwithCompat"] [COMMANDS='droplets_of_thirst set @s 7 2;effect give @s droplets_of_thirst:quenchness 30']
#        scripts/client-boot-check.sh [TIMEOUT_SECONDS]
#   GRADLE_ARGS  extra Gradle flags (see build.gradle); COMMANDS typed into chat after joining, ';'-separated.
# Generates its own superflat world (run/bootserver/bootworld) the first time. Every run: noon, clear weather, no mob
# spawns. Log lines listed in scripts/boot-check-known.txt are not counted as errors.
set -u
DIR="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${1:-300}"
cd "$DIR" || exit 2
RUN=run
SRV="$RUN/bootserver"
# Every process this run starts carries this tag in its environment, and cleanup kills only those: other checks,
# other projects' clients and the shared Gradle daemon are never touched.
export BOOTCHECK_TAG="$(basename "$DIR")-bootcheck-$$-$(date +%s)"
own_processes() {
    for d in /proc/[0-9]*; do
        p=${d#/proc/}
        [ "$p" = "$$" ] && continue
        grep -qzx "BOOTCHECK_TAG=$BOOTCHECK_TAG" "$d/environ" 2>/dev/null || continue
        tr '\0' ' ' < "$d/cmdline" 2>/dev/null | grep -q GradleDaemon && continue
        echo "$p"
    done
}
# A free port and a free X display, so this check can run next to other projects' checks.
PORT=25699
while ss -ltnH "sport = :$PORT" 2>/dev/null | grep -q .; do PORT=$((PORT + 1)); done
DISP=97
while [ -e "/tmp/.X$DISP-lock" ] || [ -e "/tmp/.X11-unix/X$DISP" ]; do DISP=$((DISP + 1)); done

mkdir -p build "$SRV"
# Gradle only builds and writes the launch scripts; the server and the client then run outside it, so both can run at
# once without two Gradle builds on the same project.
PREP_LOG="build/client-boot-check-prepare.log"
./gradlew classes apiClasses createBootCheckLaunchScript createBootCheckServerLaunchScript --no-configuration-cache \
    -PbootCheckPort=$PORT ${GRADLE_ARGS:-} > "$PREP_LOG" 2>&1 \
    || { echo "BOOTCHECK: build failed (see $PREP_LOG)"; exit 2; }
SERVER_SH="build/moddev/runBootCheckServer.sh"; CLIENT_SH="build/moddev/runBootCheck.sh"

echo "eula=true" > "$SRV/eula.txt"
write_props() {
    cat > "$SRV/server.properties" <<PROPS_EOF
level-name=$1
level-type=minecraft\:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
spawn-monsters=false
spawn-npcs=false
difficulty=peaceful
gamemode=survival
force-gamemode=true
spawn-protection=0
online-mode=false
enforce-secure-profile=false
server-port=$PORT
PROPS_EOF
}
# Waits until the server log says Done; fails if the server exits first.
wait_done() {
    for _ in $(seq 1 "$TIMEOUT"); do
        grep -qE 'Done \(' "$1" && return 0
        kill -0 "$2" 2>/dev/null || return 1
        sleep 1
    done
    return 1
}
# A superflat world of its own (no structures, no caves to spawn in): generated once by the boot check server.
BOOTWORLD="$SRV/bootworld"
if [ ! -f "$BOOTWORLD/level.dat" ]; then
    echo "Generating a superflat world for the boot check..."
    write_props bootworld
    GEN_LOG="build/client-boot-check-world.log"
    bash "$SERVER_SH" > "$GEN_LOG" 2>&1 &
    GEN=$!
    wait_done "$GEN_LOG" $GEN
    kill $(own_processes) 2>/dev/null
    wait $GEN 2>/dev/null
    [ -f "$BOOTWORLD/level.dat" ] || { echo "BOOTCHECK: could not generate the superflat world (see $GEN_LOG)"; exit 2; }
fi
rm -rf "$SRV/bdboot"; cp -r "$BOOTWORLD" "$SRV/bdboot"
rm -f "$SRV/bdboot/session.lock"
# A fresh player, placed at the world spawn in survival, or the HUD under test (hearts, food, thirst) is hidden.
rm -rf "$SRV/bdboot/playerdata"
rm -rf "$SRV/bdboot/serverconfig"
write_props bdboot
# The client plays as BDBoot (build.gradle), an operator so the setup commands run. Offline UUID, as the server computes it.
python3 - "$SRV/ops.json" <<'PY'
import hashlib, json, sys, uuid
name = "BDBoot"
u = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + name).encode()).digest(), version=3)
json.dump([{"uuid": str(u), "name": name, "level": 4, "bypassesPlayerLimit": False}], open(sys.argv[1], "w"))
PY

OPTS="$RUN/options.txt"; touch "$OPTS"
for cat in master music record weather block hostile neutral player ambient voice; do
    sed -i "/^soundCategory_$cat:/d" "$OPTS"; echo "soundCategory_$cat:0.0" >> "$OPTS"
done
# A fresh game dir opens the first-launch accessibility screen, which waits for a click.
sed -i "/^onboardAccessibility:/d" "$OPTS"; echo "onboardAccessibility:false" >> "$OPTS"
# No tutorial toast waiting for input.
sed -i "/^tutorialStep:/d" "$OPTS"; echo "tutorialStep:none" >> "$OPTS"
# No multiplayer warning screen.
sed -i "/^skipMultiplayerWarning:/d" "$OPTS"; echo "skipMultiplayerWarning:true" >> "$OPTS"

LOG="build/client-boot-check.log"; : > "$LOG"
SERVER_LOG="build/client-boot-check-server.log"; : > "$SERVER_LOG"
GAME_LOG="$RUN/logs/latest.log"; rm -f "$GAME_LOG"

bash "$SERVER_SH" > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
if ! wait_done "$SERVER_LOG" $SERVER_PID; then
    kill $(own_processes) 2>/dev/null; sleep 3; kill -9 $(own_processes) 2>/dev/null
    echo "BOOTCHECK: FAIL (the server didn't start, see $SERVER_LOG)"
    grep -E 'Exception|Error' "$SERVER_LOG" | head -5
    exit 1
fi

export LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3 MESA_GLSL_VERSION_OVERRIDE=330
XAUTH="$DIR/build/$BOOTCHECK_TAG.xauth"
xvfb-run -n $DISP -f "$XAUTH" -s "-screen 0 1920x1080x24" bash "$CLIENT_SH" > "$LOG" 2>&1 &
PID=$!
logs() { cat "$LOG" "$GAME_LOG" "$SERVER_LOG" 2>/dev/null; }

FAIL_RE='InvalidInjectionException|Mixin apply failed|Preparing crash report|Exception in thread "Render thread"|Failed to load .*droplets_of_thirst|FileNotFoundException: .*droplets_of_thirst'
# The server let the client in (the client doesn't log chat on 1.18.2).
OK_RE='BDBoot joined the game'
verdict="TIMEOUT after ${TIMEOUT}s"
for _ in $(seq 1 "$TIMEOUT"); do
    if logs | grep -qE "$FAIL_RE"; then verdict="FAIL"; break; fi
    if logs | grep -qE "$OK_RE"; then
        sleep 20
        X="env DISPLAY=:$DISP XAUTHORITY=$XAUTH xdotool"
        SETUP='gamerule sendCommandFeedback false;difficulty peaceful;time set noon;weather clear;gamerule doDaylightCycle false;gamerule doWeatherCycle false;gamerule doMobSpawning false'
        IFS=';' read -ra CMDS <<< "$SETUP;${COMMANDS:-}"
        for cmd in "${CMDS[@]}"; do
            [ -z "$cmd" ] && continue
            $X key t; sleep 1; $X type --delay 20 "/$cmd"; $X key Return; sleep 1
        done
        # Let the HUD render for a while with the commands applied before judging the log.
        sleep 15
        logs | grep -qE "$FAIL_RE" && verdict="FAIL" || verdict="PASS"
        break
    fi
    kill -0 $PID 2>/dev/null || { verdict="EXITED EARLY"; break; }
    sleep 1
done

kill $(own_processes) 2>/dev/null; sleep 3; kill -9 $(own_processes) 2>/dev/null
rm -f "$XAUTH"

# Suspicious lines: ERROR level, an exception or error class starting a line, uncaught exceptions; minus known ones.
KNOWN="$DIR/scripts/boot-check-known.txt"
problems=$(logs | grep -E '/ERROR\]|^[A-Za-z_$][A-Za-z0-9_$.]*(Exception|Error)(:|$)|Exception in thread' \
    | { if [ -f "$KNOWN" ]; then grep -vEf <(grep -vE '^[[:space:]]*(#|$)' "$KNOWN"); else cat; fi; } | sort -u)
[ "$verdict" = PASS ] && [ -n "$problems" ] && verdict="FAIL (errors in the log)"
echo "BOOTCHECK: $verdict"
[ -n "$problems" ] && echo "$problems" | head -20
logs | grep -E "$OK_RE" | head -2
exit $([ "$verdict" = PASS ] && echo 0 || echo 1)
