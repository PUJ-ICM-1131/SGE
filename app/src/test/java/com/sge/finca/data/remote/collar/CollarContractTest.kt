package com.sge.finca.data.remote.collar

import com.sge.finca.data.repository.RemoteAnimalLocationSource
import com.sge.finca.data.repository.RemoteCollarRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que la app y el repo sge-collar-api hablen el mismo JSON.
 * Los cuerpos de ejemplo son respuestas reales del servidor.
 */
class CollarContractTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `decodifica las posiciones y las indexa por animal`() {
        val body = """
            {"generatedAt":1795364716969,"positions":[
              {"collarId":"c1","serial":"SGE-0001","animalId":"a1","lat":4.6280957,"lng":-74.0651394,
               "speedMps":0.11,"headingDeg":91.7,"batteryPercent":80,"timestamp":1795364716969,"campoNuevo":1},
              {"collarId":"c2","serial":"SGE-0002","animalId":null,"lat":4.6,"lng":-74.0,
               "speedMps":1.2,"headingDeg":10.0,"batteryPercent":90,"timestamp":1795364716969}
            ]}
        """.trimIndent()

        val locations = json.decodeFromString<CollarPositionsResponse>(body).toLocationsByAnimalId()

        assertEquals(setOf("a1"), locations.keys) // el collar sin animal se ignora
        val lola = locations.getValue("a1")
        assertEquals(4.6280957, lola.latitude, 0.0)
        assertEquals(-74.0651394, lola.longitude, 0.0)
        assertEquals(0.11f, lola.speedMetersPerSecond, 0f)
        assertEquals(1795364716969L, lola.timestamp)
    }

    @Test
    fun `decodifica un collar del CRUD`() {
        val body = """
            {"id":"beb02a6d-0b27-423c-a63d-a710e82a6d0b","serial":"SGE-0001","farmId":"f1","animalId":"a1",
             "animalName":null,"anchor":{"lat":4.628,"lng":-74.065},"radiusMeters":40,"status":"ACTIVE",
             "createdAt":"2026-11-22T16:25:16.955Z","updatedAt":"2026-11-22T16:25:16.955Z"}
        """.trimIndent()

        val collar = json.decodeFromString<CollarDto>(body)

        assertEquals("SGE-0001", collar.serial)
        assertEquals(40.0, collar.radiusMeters, 0.0)
        assertEquals("ACTIVE", collar.status)
    }

    @Test
    fun `PATCH solo envia los campos que cambian`() {
        val encoded = json.encodeToString(UpdateCollarRequest(status = "INACTIVE"))
        assertEquals("""{"status":"INACTIVE"}""", encoded)
    }

    @Test
    fun `una velocidad negativa se trata como quieto`() {
        val dto = CollarPositionDto(collarId = "c", serial = "S", lat = 0.0, lng = 0.0, speedMps = -1f, timestamp = 0)
        assertEquals(0f, dto.toDomain("a").speedMetersPerSecond, 0f)
    }

    @Test
    fun `el serial del collar es estable y cumple las reglas del proveedor`() {
        val id = "3f2b8c1e-9a4d-4e7b-b1c2-0d9e8f7a6b5c"
        val serial = RemoteCollarRepository.serialFor(id)

        assertEquals("SGE-3F2B8C1E9A4D4E7BB1C20D9E8F7A6B5C", serial)
        assertEquals(serial, RemoteCollarRepository.serialFor(id)) // mismo animal, mismo serial
        assertTrue(serial.length <= 40)
        assertTrue(serial.matches(Regex("^[A-Za-z0-9-]+$")))
    }

    @Test
    fun `la espera entre reintentos crece y tiene tope`() {
        val waits = (1..7).map { RemoteAnimalLocationSource.backoffMillis(it) }
        assertEquals(listOf(3_000L, 6_000L, 12_000L, 24_000L, 30_000L, 30_000L, 30_000L), waits)
        assertTrue(waits.zipWithNext().all { (a, b) -> b >= a })
    }
}
