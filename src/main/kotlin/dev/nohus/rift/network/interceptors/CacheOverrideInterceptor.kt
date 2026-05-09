package dev.nohus.rift.network.interceptors

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

@Single
class CacheOverrideInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance

        val response = chain.proceed(request)

        /**
         * Project details are cached for 1 year and instead use a cache buster parameter based on the project
         * listing last modified date.
         */
        if (endpoint is Endpoint.GetCorporationsIdProjectsIdContributors ||
            endpoint is Endpoint.GetCorporationsIdProjectsIdContribution ||
            endpoint is Endpoint.GetCorporationsIdProjectsId ||
            endpoint is Endpoint.GetFreelanceJobsId ||
            endpoint is Endpoint.GetCharactersIdFreelanceJobsIdParticipation ||
            endpoint is Endpoint.GetCorporationsIdFreelanceJobsIdParticipants

        ) {
            val hasCacheBuster = request.url.queryParameter("cb") != null
            if (hasCacheBuster) {
                return response.newBuilder()
                    .header("Cache-Control", "private, max-age=31536000")
                    .build()
            }
        }

        return response
    }
}
