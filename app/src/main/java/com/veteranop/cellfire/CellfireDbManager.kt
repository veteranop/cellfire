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

enum class DbMatchLevel {
    /** conf == 100 — registered device confirmed this cell (alpha/plmn). Blue. */
    EXACT,
    /** conf 75–99 — strong crowd or FCC confirmation. Green. */
    HIGH_CONF,
    /** conf 40–74 — OCID-seeded or PCI-only unambiguous match. Yellow. */
    MED_CONF,
    /** conf 1–39 — pci_range guess or very sparse data. Red. */
    LOW_CONF,
    /** No record for this PCI in any loaded tile. Grey. */
    NONE
}

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
    val samples: Int,
    val conf: Int = 0,      // 0–100 confidence score computed server-side
    val source: String = "", // source tag that earned this record its score
    val arfcn: Int = 0      // EARFCN/NR-ARFCN — enables TAC resolution for neighbor cells
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

    // pci -> set of carrier names seen across all loaded tiles (for ambiguity detection)
    private val pciCarrierSets = HashMap<Int, MutableSet<String>>()

    // pci -> best DbCell by sample count (for PCI-only lookups)
    private val pciCells = HashMap<Int, DbCell>()

    // (pci, arfcn) -> best DbCell with a confirmed real TAC — lets neighbor cells
    // resolve to the full record when TAC=0 but EARFCN is known.
    // Same PCI + same EARFCN in the same tile = same physical cell, TAC=0 is a
    // phone-side measurement limitation, not genuine ambiguity.
    private val pciEarfcnMap = HashMap<Long, DbCell>()

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
        pciCarrierSets.clear()
        pciCells.clear()
        pciEarfcnMap.clear()
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

    /**
     * PCI-only lookup for neighbor cells where TAC is unavailable.
     * Returns the best DbCell only when exactly ONE carrier holds this PCI in the loaded tiles.
     * Returns null if ambiguous (multiple carriers share this PCI) or not found.
     */
    fun lookupByPciOnly(pci: Int): DbCell? {
        val carriers = pciCarrierSets[pci] ?: return null
        return if (carriers.size == 1) pciCells[pci] else null
    }

    /**
     * EARFCN-based lookup for neighbor cells (TAC=0) that have a known EARFCN.
     *
     * Same PCI + same EARFCN in the same geographic tile = same physical cell.
     * TAC=0 is a phone-side measurement limit, not real ambiguity. If another
     * device has seen this cell as a serving cell, its confirmed (pci, tac, carrier)
     * record will be in pciEarfcnMap and we can return it here with full confidence.
     *
     * Returns null if arfcn <= 0 or no confirmed real-TAC record is indexed.
     */
    fun lookupByPciEarfcn(pci: Int, arfcn: Int): DbCell? {
        if (arfcn <= 0) return null
        val result = pciEarfcnMap[packPciArfcnKey(pci, arfcn)]
        Log.d(TAG, if (result != null) "DB EARFCN HIT pci=$pci arfcn=$arfcn → ${result.carrier} tac=${result.tac}"
                   else                "DB EARFCN MISS pci=$pci arfcn=$arfcn")
        return result
    }

    /**
     * Returns the confidence level of a DB match for display purposes.
     *
     *  EXACT     — PCI + TAC both matched (registered device confirmed)
     *  PCI_ONLY  — PCI matched, TAC unavailable, single unambiguous carrier in tile
     *  AMBIGUOUS — PCI matched but multiple carriers share it in the loaded tiles
     *  NONE      — no record for this PCI in any loaded tile
     */
    fun lookupMatchLevel(pci: Int, tac: Int): DbMatchLevel {
        val exact = lookup[packKey(pci, tac)] ?: tacLookup[tac]
        if (exact != null) {
            return confToLevel(exact.conf)
        }
        // No TAC match — try PCI-only
        val carriers = pciCarrierSets[pci] ?: return DbMatchLevel.NONE
        return if (carriers.size == 1) {
            // Unambiguous PCI: use best cell's conf score but cap at MED_CONF
            // (no TAC confirmation, so can't be HIGH_CONF or EXACT)
            // conf=0 (unscored legacy) still shows MED_CONF so user sees it's in DB
            val bestConf = pciCells[pci]?.conf ?: 0
            if (bestConf >= 40 || bestConf == 0) DbMatchLevel.MED_CONF else DbMatchLevel.LOW_CONF
        } else {
            // Multiple carriers share this PCI — ambiguous
            DbMatchLevel.MED_CONF
        }
    }

    private fun confToLevel(conf: Int): DbMatchLevel = when {
        conf == 100  -> DbMatchLevel.EXACT
        conf >= 75   -> DbMatchLevel.HIGH_CONF
        conf >= 40   -> DbMatchLevel.MED_CONF
        else         -> DbMatchLevel.LOW_CONF  // conf 1–39 OR 0 (unscored legacy tile) — in DB but low trust
    }

    // ── private ──────────────────────────────────────────────────────────────

    private fun rebuildLookup() {
        lookup.clear()
        tacLookup.clear()
        pciCarrierSets.clear()
        pciCells.clear()
        pciEarfcnMap.clear()
        for (cachedTile in tileCache.values) {
            mergeCells(cachedTile.cells)
        }
    }

    private fun mergeCells(cells: List<DbCell>) {
        for (cell in cells) {
            // 0xFFFF (65535) is a modem sentinel meaning "TAC unavailable" — treat as tac=0
            val tac = if (cell.tac == 0xFFFF) 0 else cell.tac
            val normalizedCell = if (tac != cell.tac) cell.copy(tac = tac) else cell

            val key = packKey(normalizedCell.pci, normalizedCell.tac)
            val existing = lookup[key]
            if (existing == null || normalizedCell.samples > existing.samples) {
                lookup[key] = normalizedCell
            }
            if (normalizedCell.pci <= 0 && normalizedCell.tac > 0) {
                val existingTac = tacLookup[normalizedCell.tac]
                if (existingTac == null || normalizedCell.samples > existingTac.samples) {
                    tacLookup[normalizedCell.tac] = normalizedCell
                }
            }
            // Track per-PCI carrier sets for ambiguity detection
            if (normalizedCell.pci > 0 && normalizedCell.carrier.isNotBlank() && normalizedCell.carrier != "Unknown") {
                pciCarrierSets.getOrPut(normalizedCell.pci) { mutableSetOf() }.add(normalizedCell.carrier)
                val best = pciCells[normalizedCell.pci]
                if (best == null || normalizedCell.samples > best.samples) {
                    pciCells[normalizedCell.pci] = normalizedCell
                }
            }
            // Index real-TAC records by (pci, arfcn) so neighbor lookups can resolve
            // TAC=0 observations when the EARFCN is known. Only index records that have
            // a confirmed real TAC and a known EARFCN — TAC=0 entries are never anchors.
            if (normalizedCell.pci > 0 && normalizedCell.tac > 0 && normalizedCell.arfcn > 0 &&
                normalizedCell.carrier.isNotBlank() && normalizedCell.carrier != "Unknown") {
                val earfcnKey = packPciArfcnKey(normalizedCell.pci, normalizedCell.arfcn)
                val bestEarfcn = pciEarfcnMap[earfcnKey]
                if (bestEarfcn == null || normalizedCell.conf > bestEarfcn.conf ||
                    (normalizedCell.conf == bestEarfcn.conf && normalizedCell.samples > bestEarfcn.samples)) {
                    pciEarfcnMap[earfcnKey] = normalizedCell
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

    // EARFCN values can exceed Int range for NR (up to ~3.3M), so keep full bits.
    // pci is 0–503 (LTE) / 0–1007 (NR) — fits comfortably in the high 20 bits.
    private fun packPciArfcnKey(pci: Int, arfcn: Int): Long = (pci.toLong() shl 32) or (arfcn.toLong() and 0xFFFFFFFFL)
}
