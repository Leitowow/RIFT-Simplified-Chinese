package dev.nohus.rift.di

import com.sun.jna.Native
import dev.nohus.rift.logging.analytics.Analytics
import dev.nohus.rift.network.interceptors.CacheOverrideInterceptor
import dev.nohus.rift.network.interceptors.EsiAppHeadersInterceptor
import dev.nohus.rift.network.interceptors.EsiAuthorizationInterceptor
import dev.nohus.rift.network.interceptors.EsiCompatibilityInterceptor
import dev.nohus.rift.network.interceptors.EsiErrorLimitInterceptor
import dev.nohus.rift.network.interceptors.EsiRateLimitInterceptor
import dev.nohus.rift.network.interceptors.LoggingInterceptor
import dev.nohus.rift.network.interceptors.RequestIdInterceptor
import dev.nohus.rift.network.interceptors.UserAgentInterceptor
import dev.nohus.rift.network.requests.CacheStatisticsLocalInterceptor
import dev.nohus.rift.network.requests.CacheStatisticsNetworkInterceptor
import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.OriginatorRateLimitInterceptor
import dev.nohus.rift.network.requests.RequestExecutor
import dev.nohus.rift.network.requests.RequestExecutorImpl
import dev.nohus.rift.network.requests.RequestStatisticsInterceptor
import dev.nohus.rift.notifications.system.LinuxSendNotificationUseCase
import dev.nohus.rift.notifications.system.MacSendNotificationUseCase
import dev.nohus.rift.notifications.system.SendNotificationUseCase
import dev.nohus.rift.notifications.system.WindowsSendNotificationUseCase
import dev.nohus.rift.utils.GetOperatingSystemUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.Linux
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.OperatingSystem.Windows
import dev.nohus.rift.utils.activewindow.GetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.LinuxGetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.MacGetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.WindowsGetActiveWindowUseCase
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.openwindows.GetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.LinuxGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.MacGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.WindowsGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.windows.User32
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import dev.nohus.rift.utils.osdirectories.MacDirectories
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import dev.nohus.rift.utils.osdirectories.WindowsDirectories
import io.kamel.core.config.Core
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.httpUrlFetcher
import io.kamel.core.config.takeFrom
import io.kamel.image.config.animatedImageDecoder
import io.kamel.image.config.imageBitmapDecoder
import io.kamel.image.config.resourcesFetcher
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.utils.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

@Module
@ComponentScan("dev.nohus.rift")
class KoinModule

val platformModule = module {
    single<OperatingSystem> {
        get<GetOperatingSystemUseCase>()()
    }
    single<OperatingSystemDirectories> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxDirectories()
            Windows -> WindowsDirectories()
            MacOs -> MacDirectories()
        }
    }
    single<GetOpenEveClientsUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxGetOpenEveClientsUseCase(get(), get())
            Windows -> WindowsGetOpenEveClientsUseCase(get())
            MacOs -> MacGetOpenEveClientsUseCase()
        }
    }
    single<GetActiveWindowUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxGetActiveWindowUseCase(get())
            Windows -> WindowsGetActiveWindowUseCase(get())
            MacOs -> MacGetActiveWindowUseCase()
        }
    }
    single<SendNotificationUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxSendNotificationUseCase(get(), get())
            Windows -> WindowsSendNotificationUseCase()
            MacOs -> MacSendNotificationUseCase(get())
        }
    }
}

val factoryModule = module {
    single<OkHttpClient>(qualifier = named("api")) {
        val directory = get<AppDirectories>().getAppCacheDirectory().resolve("http-cache")
        val size = 50L * 1024 * 1024 // 50MB
        OkHttpClient.Builder()
            .cache(Cache(directory.toFile(), size))
            .addInterceptor(get<UserAgentInterceptor>())
            .addNetworkInterceptor(get<RequestStatisticsInterceptor>())
            .addNetworkInterceptor(get<LoggingInterceptor>())
            .build()
    }
    single<OkHttpClient>(qualifier = named("esi")) {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 64
        }
        val directory = get<AppDirectories>().getAppCacheDirectory().resolve("esi-cache")
        val size = 100L * 1024 * 1024 // 100MB
        OkHttpClient.Builder()
            .cache(Cache(directory.toFile(), size))
            .dispatcher(dispatcher)
            .addInterceptor(get<RequestIdInterceptor>())
            .addInterceptor(get<CacheStatisticsLocalInterceptor>())
            .addInterceptor(get<EsiCompatibilityInterceptor>())
            .addNetworkInterceptor(get<EsiAuthorizationInterceptor>())
            .addNetworkInterceptor(get<CacheStatisticsNetworkInterceptor>())
            .addNetworkInterceptor(get<EsiErrorLimitInterceptor>())
            .addNetworkInterceptor(get<EsiRateLimitInterceptor>())
            .addNetworkInterceptor(get<OriginatorRateLimitInterceptor>())
            .addNetworkInterceptor(get<CacheOverrideInterceptor>())
            .addNetworkInterceptor(get<UserAgentInterceptor>())
            .addNetworkInterceptor(get<EsiAppHeadersInterceptor>())
            .addNetworkInterceptor(get<RequestStatisticsInterceptor>())
            .addNetworkInterceptor(get<LoggingInterceptor>())
            .build()
    }
    single<OkHttpClient>(qualifier = named("zkillr2z2")) {
        val directory = get<AppDirectories>().getAppCacheDirectory().resolve("zkill-cache")
        val size = 50L * 1024 * 1024 // 50MB
        OkHttpClient.Builder()
            .cache(Cache(directory.toFile(), size))
            .addInterceptor(get<UserAgentInterceptor>())
            .addNetworkInterceptor(get<RequestStatisticsInterceptor>())
            .addNetworkInterceptor(get<LoggingInterceptor>())
            .build()
    }
    single<Json>(qualifier = named("network")) {
        Json {
            ignoreUnknownKeys = true
        }
    }
    single<Json>(qualifier = named("settings")) {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
    factory<RequestExecutor> { RequestExecutorImpl(get(named("network"))) }
    single<User32> { Native.load("user32", User32::class.java) }
    single<Analytics> { Analytics() }
    single<KamelConfig> { getKamelConfig(get(), get()) }
}

private fun getKamelConfig(
    userAgentInterceptor: UserAgentInterceptor,
    requestStatisticsInterceptor: RequestStatisticsInterceptor,
): KamelConfig {
    val logStatisticsPlugin = createClientPlugin("LogStatistics") {
        onResponse {
            requestStatisticsInterceptor.addExternalRequest(Originator.UiImage, Endpoint.ImageServiceAsset, it.status.isSuccess())
        }
    }
    return KamelConfig {
        takeFrom(KamelConfig.Core)
        httpUrlFetcher {
            httpCache(100 * 1024 * 1024)
            defaultRequest {
                header(HttpHeaders.UserAgent, userAgentInterceptor.userAgentValue)
                header(HttpHeaders.CacheControl, CacheControl.MAX_AGE)
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, httpResponse ->
                    !httpResponse.status.isSuccess()
                }
            }
            install(logStatisticsPlugin)
        }
        resourcesFetcher()
        imageBitmapDecoder()
        animatedImageDecoder()
        imageBitmapCacheSize = 1000
    }
}
