package id.oemah.nusantara.network

import id.oemah.nusantara.api.AdsApi
import id.oemah.nusantara.api.AuthApi
import id.oemah.nusantara.api.CatalogApi
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.SocialApi
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

/** Every operation of the generated client, found by reflecting on its Retrofit annotations. */
object ApiOperations {
    class Operation(val name: String, val httpMethod: String, template: String, val bodyType: Type?) {
        /** Regex for the request path, `{param}` segments match any single path segment. */
        val pattern = Regex(
            "^" + template.split(Regex("[{][^}]+[}]")).joinToString("[^/]+") { Regex.escape(it) } + "$",
        )
    }

    private val apis = listOf(AuthApi::class.java, MeApi::class.java, CatalogApi::class.java, SocialApi::class.java, OrdersApi::class.java, AdsApi::class.java)

    val all: List<Operation> = apis.flatMap { api ->
        api.declaredMethods.filterNot { it.isSynthetic || '$' in it.name }.mapNotNull(::toOperation)
    }

    private fun toOperation(method: Method): Operation? {
        val (httpMethod, template) = method.getAnnotation(GET::class.java)?.let { "GET" to it.value }
            ?: method.getAnnotation(POST::class.java)?.let { "POST" to it.value }
            ?: method.getAnnotation(PUT::class.java)?.let { "PUT" to it.value }
            ?: method.getAnnotation(PATCH::class.java)?.let { "PATCH" to it.value }
            ?: method.getAnnotation(DELETE::class.java)?.let { "DELETE" to it.value }
            ?: return null
        return Operation(method.name, httpMethod, template, bodyTypeOf(method))
    }

    /** `suspend fun x(): Response<T>` is `x(…, Continuation<? super Response<T>>)` to reflection; null when T is Unit. */
    private fun bodyTypeOf(method: Method): Type? {
        val continuation = method.genericParameterTypes.last() as ParameterizedType
        val response = (continuation.actualTypeArguments[0] as WildcardType).lowerBounds[0] as ParameterizedType
        return response.actualTypeArguments[0].takeUnless { it == Unit::class.java }
    }
}
