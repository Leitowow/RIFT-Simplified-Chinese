package dev.nohus.rift.debug

import ch.qos.logback.classic.spi.ILoggingEvent
import dev.nohus.rift.ViewModel
import dev.nohus.rift.about.GetVersionUseCase
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.logging.LoggingRepository
import dev.nohus.rift.network.interceptors.EsiRateLimitInterceptor
import dev.nohus.rift.network.interceptors.EsiRateLimitInterceptor.BucketKey
import dev.nohus.rift.network.requests.CacheStatistics
import dev.nohus.rift.network.requests.RequestStatisticsInterceptor
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.ZoneId
import dev.nohus.rift.network.requests.CacheStatistics.BucketKey as CacheBucketKey

@Factory
class DebugViewModel(
    private val settings: Settings,
    getVersionUseCase: GetVersionUseCase,
    private val jabberClient: JabberClient,
    operatingSystem: OperatingSystem,
    private val requestStatisticsInterceptor: RequestStatisticsInterceptor,
    private val esiRateLimitInterceptor: EsiRateLimitInterceptor,
    private val cacheStatistics: CacheStatistics,
) : ViewModel() {

    data class UiState(
        val tab: DebugTab = DebugTab.Logs,
        // Logs
        val events: List<ILoggingEvent> = emptyList(),
        val displayTimezone: ZoneId,
        val version: String,
        val vmVersion: String,
        val operatingSystem: OperatingSystem,
        val isJabberConnected: Boolean,
        // Network
        val buckets: List<RequestStatisticsInterceptor.Bucket> = emptyList(),
        // Rate limits
        val rateLimitBuckets: Map<BucketKey, EsiRateLimitInterceptor.Bucket> = emptyMap(),
        val spentTokens: Map<BucketKey, List<EsiRateLimitInterceptor.SpentTokens>> = emptyMap(),
        // Cache
        val cacheRequests: Map<CacheBucketKey, Int> = emptyMap(),
    )

    enum class DebugTab {
        Logs,
        Network,
        RateLimits,
        Cache,
    }

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
            version = getVersionUseCase(),
            vmVersion = getVmVersion(),
            operatingSystem = operatingSystem,
            isJabberConnected = jabberClient.state.value.isConnected,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            LoggingRepository.state.collect { logs ->
                _state.update { it.copy(events = logs) }
            }
        }
        viewModelScope.launch {
            requestStatisticsInterceptor.buckets.collectLatest { buckets ->
                _state.update { it.copy(buckets = buckets.toList()) }
            }
        }
        viewModelScope.launch {
            while (true) {
                val (buckets, spendTokens) = esiRateLimitInterceptor.getBuckets()
                _state.update { it.copy(rateLimitBuckets = buckets, spentTokens = spendTokens) }
                delay(333)
            }
        }
        viewModelScope.launch {
            cacheStatistics.requests.collectLatest { requests ->
                _state.update { it.copy(cacheRequests = requests) }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        displayTimezone = settings.displayTimeZone,
                    )
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update {
                    it.copy(
                        isJabberConnected = jabberClient.state.value.isConnected,
                    )
                }
            }
        }
    }

    fun onTabClick(tab: DebugTab) {
        _state.update { it.copy(tab = tab) }
    }

    private fun getVmVersion(): String {
        return "${System.getProperty("java.vm.vendor")} ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}"
    }
}
