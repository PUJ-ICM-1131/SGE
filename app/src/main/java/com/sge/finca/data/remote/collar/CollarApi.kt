package com.sge.finca.data.remote.collar

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API del proveedor externo de collares GPS (repo sge-collar-api).
 * La clave (X-Api-Key) la agrega el interceptor de [com.sge.finca.data.injection.NetworkModule].
 * Los errores HTTP llegan como retrofit2.HttpException con un cuerpo [CollarErrorDto].
 */
interface CollarApi {

    // --- Posición en tiempo real ---

    /** Posiciones de los collares activos de esos animales. [animalIds] separados por coma. */
    @GET("v1/positions")
    suspend fun getPositions(@Query("animalIds") animalIds: String): CollarPositionsResponse

    @GET("v1/collars/{id}/position")
    suspend fun getPosition(@Path("id") collarId: String): CollarPositionDto

    // --- CRUD de collares ---

    @GET("v1/collars")
    suspend fun listCollars(
        @Query("farmId") farmId: String? = null,
        @Query("animalIds") animalIds: String? = null,
    ): CollarListResponse

    @GET("v1/collars/{id}")
    suspend fun getCollar(@Path("id") collarId: String): CollarDto

    @POST("v1/collars")
    suspend fun createCollar(@Body request: CreateCollarRequest): CollarDto

    @PATCH("v1/collars/{id}")
    suspend fun updateCollar(@Path("id") collarId: String, @Body request: UpdateCollarRequest): CollarDto

    @DELETE("v1/collars/{id}")
    suspend fun deleteCollar(@Path("id") collarId: String)

    companion object {
        /** Tope de ids por consulta, para no generar URLs gigantes. */
        const val MAX_ANIMALS_PER_REQUEST = 100
    }
}
