# Nightmare changes

- Replaced the nightmare scream with three pitch layers from the existing natural Nurse shriek and Wailing Head scream. Mono, softened edges, restrained distortion, about -17 LUFS with safe peak headroom.
- Hanging torso appears after 4-7 seconds of quiet instead of 40-90 seconds.
- The player must actually face the torso with an unobstructed view; no elapsed-time shortcut starts the scare.
- Two seconds after confirmed sight, the sky turns red immediately and the camera quickly turns toward a very fast crawling Beast.
- The dream Beast uses the existing model and crawl animation. Its normal AI, damage, gravity, collisions and sounds are disabled. Reaching the player wakes them without an attack.
- Removed the old rumble, thunder and heartbeat escalation. The chase starts with the new scream.
- Wake, death, leaving the dimension and disconnect clear the camera lock and sky effect. Previous first/third-person perspective is restored.

The replacement classes and audio overlay are based on the 1.22.34 release. Compile the two Java source files against that release and the Forge 1.20.1 runtime classpath, preserving SRG method names. Assemble the class output and asset overlay with `assemble.py`. Client and server need the same build because Strung effect mode 25 now starts the chase instead of rumble.

Checks: compilation, runtime member-link validation, scoped jar comparison and timing/math regression checks. Client visual timing, sky rendering, actual mouse lock, crawl appearance, and wake/death/disconnect transitions still require an in-game acceptance pass. No visual capture is claimed here.
