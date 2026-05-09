package dev.nohus.rift.network.ntp

import dev.nohus.rift.network.ntp.NtpClient.NtpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class GetNtpTimeUseCase {

    private val ntpServers = listOf(
        /* Primary: NIST Internet Time Service.
         * Reliable public service. This should work in the vast majority of cases. */
        "time.nist.gov",

        /* Secondary: Cloudflare Time Services.
         * Commercial public service. Anyone who can't reach NIST should be able to at least reach Cloudflare. */
        "time.cloudflare.com",

        /* Fallback: NTP Pool Project
         * Cluster of thousands of volunteer NTP servers pooled together.
         * Used as an absolute last resort to not abuse the resource. */
        "pool.ntp.org",
    )

    suspend operator fun invoke(): NtpResult? {
        return withContext(Dispatchers.IO) {
            val client = NtpClient()
            ntpServers.firstNotNullOfOrNull { server ->
                client.requestTime(server, NtpClient.STANDARD_NTP_PORT, 10_000)
            }
        }
    }
}
