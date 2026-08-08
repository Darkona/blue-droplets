#!/usr/bin/env bash
# Boots the real client headless (Xvfb + Mesa software GL) straight into a copy of a superflat test world and reports
# whether it joined, ran the commands and left the log without errors. No screenshot: the check is the log.
# Adapted from Green Feathers' scripts/client-boot-check.sh.
#
# Usage: [GRADLE_ARGS="-PwithCompat"] [COMMANDS='blue_droplets set @s 7 2;effect give @s blue_droplets:quenchness 30']
#        scripts/client-boot-check.sh [TIMEOUT_SECONDS]
#   GRADLE_ARGS  extra Gradle flags (see build.gradle); COMMANDS typed into chat after joining, ';'-separated.
# Generates its own superflat world (run/bootworld) the first time, with a dedicated server run. Every run: noon, clear
# weather, no mob spawns, survival. Log lines listed in scripts/boot-check-known.txt are not counted as errors.
set -u
DIR="$(cd "$(dirname "$0")/.." && pwd)"
TIMEOUT="${1:-300}"
cd "$DIR" || exit 2
RUN=run
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
cleanup() { kill $(own_processes) 2>/dev/null; sleep 3; kill -9 $(own_processes) 2>/dev/null; }

# A superflat world of its own (no structures, no caves to spawn in): generated once by scripts/server-boot-check.sh.
BOOTWORLD="$RUN/bootworld"
if [ ! -f "$BOOTWORLD/level.dat" ]; then
    echo "Generating a superflat world for the boot check..."
    "$DIR/scripts/server-boot-check.sh" 300 > build/client-boot-check-world.log 2>&1
    [ -f "$RUN/bootcheck-server/world/level.dat" ] || { echo "BOOTCHECK: could not generate the superflat world (see build/client-boot-check-world.log)"; exit 2; }
    rm -rf "$BOOTWORLD"; cp -r "$RUN/bootcheck-server/world" "$BOOTWORLD"
fi
rm -rf "$RUN/saves/bdboot"; mkdir -p "$RUN/saves"; cp -r "$BOOTWORLD" "$RUN/saves/bdboot"
rm -f "$RUN/session.lock" "$RUN/saves/bdboot/session.lock"
# A fresh player takes the world's game type (set to survival below) instead of whatever the last run left.
rm -rf "$RUN/saves/bdboot/playerdata"
# Pre-confirm the "Experimental Settings" screen, which waits for a click, turn cheats on, and play survival, in the copy only.
python3 - "$RUN/saves/bdboot/level.dat" <<'PY'
import gzip, sys
f = sys.argv[1]; b = bytearray(gzip.open(f).read())
for name in (b"allowCommands", b"confirmedExperimentalSettings"):
    tag = b"\x01" + len(name).to_bytes(2, "big") + name
    i = b.find(tag)
    if i >= 0: b[i + len(tag)] = 1
# Survival, or the HUD under test (hearts, food, thirst) is hidden.
tag = b"\x03" + len(b"GameType").to_bytes(2, "big") + b"GameType"
i = b.find(tag)
if i >= 0: b[i + len(tag):i + len(tag) + 4] = (0).to_bytes(4, "big")
gzip.open(f, "wb").write(bytes(b))
PY
OPTS="$RUN/options.txt"; touch "$OPTS"
for cat in master music record weather block hostile neutral player ambient voice; do
    sed -i "/^soundCategory_$cat:/d" "$OPTS"; echo "soundCategory_$cat:0.0" >> "$OPTS"
done
# A fresh game dir opens the first-launch accessibility screen, which waits for a click.
sed -i "/^onboardAccessibility:/d" "$OPTS"; echo "onboardAccessibility:false" >> "$OPTS"
# No tutorial toast waiting for input.
sed -i "/^tutorialStep:/d" "$OPTS"; echo "tutorialStep:none" >> "$OPTS"

LOG="build/client-boot-check.log"; mkdir -p build; : > "$LOG"
GAME_LOG="$RUN/logs/latest.log"; rm -f "$GAME_LOG"

# A free X display number, so parallel checks (this or other projects) never share a screen.
DISPLAY_NUM=""
for n in $(seq 90 130); do
    [ -e "/tmp/.X$n-lock" ] || [ -e "/tmp/.X11-unix/X$n" ] || { DISPLAY_NUM=$n; break; }
done
[ -n "$DISPLAY_NUM" ] || { echo "BOOTCHECK: no free X display between :90 and :130"; exit 2; }

export LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3 MESA_GLSL_VERSION_OVERRIDE=330
XAUTH="$DIR/build/$BOOTCHECK_TAG.xauth"
xvfb-run -n "$DISPLAY_NUM" -f "$XAUTH" -s "-screen 0 1920x1080x24" ./gradlew runBootCheck --no-configuration-cache ${GRADLE_ARGS:-} > "$LOG" 2>&1 &
PID=$!
logs() { cat "$LOG" "$GAME_LOG" 2>/dev/null; }
X="env DISPLAY=:$DISPLAY_NUM XAUTHORITY=$XAUTH xdotool"

FAIL_RE='InvalidInjectionException|Mixin apply failed|Preparing crash report|Exception in thread "Render thread"|Failed to load .*blue_droplets|FileNotFoundException: .*blue_droplets'
OK_RE='joined the game'
verdict="TIMEOUT after ${TIMEOUT}s"
for _ in $(seq 1 "$TIMEOUT"); do
    if logs | grep -qE "$FAIL_RE"; then verdict="FAIL"; break; fi
    if logs | grep -qE "$OK_RE"; then
        sleep 20
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

cleanup
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
