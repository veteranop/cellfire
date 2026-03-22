package com.veteranop.cellfire

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.util.Locale
import kotlin.math.*

@Serializable
data class DbCell(
    val pci: Int,
    val mnc: String,
    val carrier: String,
    val lat: Double,
    val lon: Double,
    val cellid: Long,
    val tac: Int,
    val range: Int,
    val samples: Int
)

object CellfireDbManager {

    private const val BASE_URL = "https://cellfire.io/files/cellfire-db/"
    private const val TILE_SIZE = 0.5
    private const val RADIUS_MILES = 80.0
    private const val MILES_PER_DEGREE_LAT = 69.0
    private const val TAG = "CellfireDb"
    private const val TILE_TTL_MS = 5 * 60 * 1000L  // Re-check server every 5 minutes

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    private data class CachedTile(
        val cells: List<DbCell>,
        val fetchedAt: Long,
        val lastModified: String? = null  // for If-Modified-Since conditional GETs
    )

    private sealed class TileResult {
        data class Downloaded(val cells: List<DbCell>, val lastModified: String?) : TileResult()
        object NotModified : TileResult()
        object NotFound : TileResult()
    }

    // tile filename -> cached tile + metadata
    private val tileCache = mutableMapOf<String, CachedTile>()

    // (pci, tac) -> best matching DbCell (highest sample count wins)
    private val lookup = HashMap<Long, DbCell>()

    // tac -> DbCell for records where pci is unknown (-1 or 0) in source data
    private val tacLookup = HashMap<Int, DbCell>()

    @Volatile private var lastCenterLat = Double.NaN
    @Volatile private var lastCenterLon = Double.NaN

    /**
     * Download/refresh tiles within RADIUS_MILES of the given location.
     * - New tiles are downloaded immediately.
     * - Cached tiles older than TILE_TTL_MS are re-checked via If-Modified-Since.
     * - 304 Not Modified = cheap refresh (no re-download).
     * - Skips entirely if center hasn't moved more than ~10 miles since last call
     *   AND all nearby tiles are still fresh.
     */
    suspend fun refreshTiles(lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        val movedFar = lastCenterLat.isNaN() ||
            abs(lat - lastCenterLat) >= 0.15 ||
            abs(lon - lastCenterLon) >= 0.15

        val now = System.currentTimeMillis()
        val tiles = tilesForRadius(lat, lon)
        val hasStaleTiles = tiles.any { filename ->
            val cached = tileCache[filename]
            cached == null || (now - cached.fetchedAt > TILE_TTL_MS && cached.cells.isNotEmpty())
        }

        if (!movedFar && !hasStaleTiles) return@withContext

        lastCenterLat = lat
        lastCenterLon = lon

        val needed = tiles.count { tileCache[it] == null }
        Log.d(TAG, "Tile refresh @ (${String.format(Locale.US, "%.3f", lat)}, ${String.format(Locale.US, "%.3f", lon)}): " +
                "${tiles.size} in radius, $needed new, ${tileCache.values.count { now - it.fetchedAt > TILE_TTL_MS }} stale")

        var loaded = 0
        var updated = 0
        for (filename in tiles) {
            val cached = tileCache[filename]
            val isStale = cached == null || (now - cached.fetchedAt > TILE_TTL_MS && cached.cells.isNotEmpty())
            if (!isStale) continue

            try {
                val result = downloadTile(filename, cached?.lastModified)
                when (result) {
                    is TileResult.Downloaded -> {
                        tileCache[filename] = CachedTile(result.cells, now, result.lastModified)
                        if (cached == null) loaded++ else updated++
                        Log.d(TAG, "  ↓ $filename (${result.cells.size} records)")
                    }
                    is TileResult.NotModified -> {
                        // Server says nothing changed — just refresh timestamp so we don't re-check for another TTL
                        tileCache[filename] = cached!!.copy(fetchedAt = now)
                        Log.d(TAG, "  ✓ $filename (not modified)")
                    }
                    is TileResult.NotFound -> {
                        // 404 — area not seeded yet; cache the miss so we don't hammer the server
                        tileCache[filename] = CachedTile(emptyList(), now)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "  ✗ $filename: ${e.message}")
            }
        }

        if (loaded > 0 || updated > 0) {
            rebuildLookup()
            val validPci = lookup.values.filter { it.pci > 0 }
            Log.d(TAG, "Tile refresh done: +$loaded new, $updated refreshed — ${lookup.size} total records (${validPci.size} with real PCI)")
            validPci.take(10).forEach { c ->
                Log.d(TAG, "  real-pci pci=${c.pci} tac=${c.tac} carrier=${c.carrier}")
            }
        }
    }

    /** Force a full re-download on the next refreshTiles call. */
    fun clearCache() {
        tileCache.clear()
        lookup.clear()
        tacLookup.clear()
        lastCenterLat = Double.NaN
        lastCenterLon = Double.NaN
    }

    /**
     * Look up carrier data by PCI + TAC. Returns null if not in the local cache.
     */
    fun lookupByPciTac(pci: Int, tac: Int): DbCell? {
        val result = lookup[packKey(pci, tac)] ?: tacLookup[tac]
        Log.d(TAG, if (result != null) "DB HIT pci=$pci tac=$tac → ${result.carrier}"
                   else                "DB MISS pci=$pci tac=$tac (${lookup.size} records loaded)")
        return result
    }

    // ── private ──────────────────────────────────────────────────────────────

    private fun rebuildLookup() {
        lookup.clear()
        tacLookup.clear()
        for (cachedTile in tileCache.values) {
            mergeCells(cachedTile.cells)
        }
    }

    private fun mergeCells(cells: List<DbCell>) {
        for (cell in cells) {
            val key = packKey(cell.pci, cell.tac)
            val existing = lookup[key]
            if (existing == null || cell.samples > existing.samples) {
                lookup[key] = cell
            }
            if (cell.pci <= 0) {
                val existingTac = tacLookup[cell.tac]
                if (existingTac == null || cell.samples > existingTac.samples) {
                    tacLookup[cell.tac] = cell
                }
            }
        }
    }

    private fun downloadTile(filename: String, ifModifiedSince: String? = null): TileResult {
        val builder = Request.Builder().url("$BASE_URL$filename")
        if (ifModifiedSince != null) {
            builder.header("If-Modified-Since", ifModifiedSince)
        }
        val response = client.newCall(builder.build()).execute()
        return try {
            when {
                response.code() == 304 -> TileResult.NotModified
                !response.isSuccessful -> TileResult.NotFound
                else -> {
                    val lastMod = response.header("Last-Modified")
                    val bytes = response.body()?.bytes() ?: return TileResult.NotFound
                    val jsonStr = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().readText()
                    val cells = json.decodeFromString<List<DbCell>>(jsonStr)
                    TileResult.Downloaded(cells, lastMod)
                }
            }
        } finally {
            response.close()
        }
    }

    private fun tilesForRadius(lat: Double, lon: Double): Set<String> {
        val radiusLatDeg = RADIUS_MILES / MILES_PER_DEGREE_LAT
        val radiusLonDeg = RADIUS_MILES / (MILES_PER_DEGREE_LAT * cos(Math.toRadians(lat)))

        val minLat = floor((lat - radiusLatDeg) / TILE_SIZE) * TILE_SIZE
        val maxLat = floor((lat + radiusLatDeg) / TILE_SIZE) * TILE_SIZE
        val minLon = floor((lon - radiusLonDeg) / TILE_SIZE) * TILE_SIZE
        val maxLon = floor((lon + radiusLonDeg) / TILE_SIZE) * TILE_SIZE

        val tiles = mutableSetOf<String>()
        var tLat = minLat
        while (tLat <= maxLat + 0.001) {
            var tLon = minLon
            while (tLon <= maxLon + 0.001) {
                tiles.add(tileFilename(tLat, tLon))
                tLon += TILE_SIZE
            }
            tLat += TILE_SIZE
        }
        return tiles
    }

    private fun tileFilename(lat: Double, lon: Double): String =
        "grid_${coordStr(lat)}_${coordStr(lon)}.json.gz"

    private fun coordStr(v: Double): String {
        val prefix = if (v >= 0.0) "p" else "n"
        return "$prefix${String.format(Locale.US, "%.1f", abs(v))}"
    }

    private fun packKey(pci: Int, tac: Int): Long = (pci.toLong() shl 32) or (tac.toLong() and 0xFFFFFFFFL)
}
