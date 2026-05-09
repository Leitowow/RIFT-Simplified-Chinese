package dev.nohus.rift.startupwarning

import dev.nohus.rift.network.ntp.GetNtpTimeUseCase
import org.koin.core.annotation.Single
import java.time.Duration

@Single
class HasIncorrectSystemTimeUseCase(
    private val getNtpTimeUseCase: GetNtpTimeUseCase,
) {

    sealed interface SystemTimeStatus {
        object Ok : SystemTimeStatus
        data class Incorrect(val offset: Duration) : SystemTimeStatus
    }

    private val maximumOffset = Duration.ofMinutes(1)

    suspend operator fun invoke(): SystemTimeStatus? {
        val result = getNtpTimeUseCase()
        return result?.let {
            if (it.clockOffset.abs() <= maximumOffset) {
                SystemTimeStatus.Ok
            } else {
                SystemTimeStatus.Incorrect(it.clockOffset)
            }
        }
    }
}
