package place.imphi.mods.focus.utils

/**
 * Simplest random implementation I could ~~copy from the internet~~ conceive
 * I could consider using the world seed for generating this thing, but not rn
 */
fun simpleRandom(seed: Long): Long {
    return (seed * 1103515245L + 12345L) and 0x7fffffffL
}