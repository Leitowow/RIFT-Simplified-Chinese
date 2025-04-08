package dev.nohus.rift.database

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.HasNonAsciiWindowsUsernameUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SolarSystemsRepositoryTest : FreeSpec({

    val target = SolarSystemsRepository(
        staticDatabase = StaticDatabase(SqliteInitializer(HasNonAsciiWindowsUsernameUseCase(OperatingSystem.Linux, AppDirectories(LinuxDirectories())))),
    )

    listOf(
        Triple("Jita", emptyList(), "Jita"),
        Triple("Jita", listOf("Delve"), "Jita"),
        Triple("jita", emptyList(), "Jita"),
        Triple("1DQ1-A", emptyList(), "1DQ1-A"),
        Triple("1dq", emptyList(), "1DQ1-A"),
        Triple("1DQ", emptyList(), "1DQ1-A"),
        Triple("1dq1", emptyList(), "1DQ1-A"),
        Triple("1dQ1-a", emptyList(), "1DQ1-A"),
        Triple("invalid", emptyList(), null),
        Triple("", emptyList(), null),
        Triple("RP", emptyList(), null),
        Triple("rp", emptyList(), null),
        Triple("1dq1-", emptyList(), null),
        Triple("Jita ", emptyList(), null),
        Triple(" Jita", emptyList(), null),
        Triple("40-239", emptyList(), "4O-239"),
        Triple("O-3VW8", emptyList(), "0-3VW8"),
        Triple("4O-", listOf("Outer Passage"), "4O-ZRI"),
        Triple("4O-", listOf("Delve"), "4O-239"),
        Triple("4O-", listOf("Period Basis"), null),
        Triple("4O-", emptyList(), null),
        Triple("40-", listOf("Outer Passage"), "4O-ZRI"),
        Triple("40-", listOf("Delve"), "4O-239"),
        Triple("40-", listOf("Period Basis"), null),
        Triple("40-", emptyList(), null),
        Triple("1B", listOf("Delve"), "1B-VKF"),
        Triple("1B", listOf("Tenal"), "1BWK-S"),
        Triple("1B", listOf("Delve", "Tenal"), null),
        Triple("1B", listOf("Tenal", "Delve"), null),
        Triple("1B", emptyList(), null),
        Triple("1d", listOf("Delve"), null),
        Triple("1D", emptyList(), null),
        Triple("1d", listOf("Delve"), null),
        Triple("1D", listOf("Delve"), null),
        Triple("1d", listOf("Paragon Soul"), "1DDR-X"),
        Triple("1D", listOf("Paragon Soul"), "1DDR-X"),
        Triple("1D", listOf("Paragon Soul", "Tenal"), "1DDR-X"),
    ).forEach { (input, regionsHint, expected) ->
        "for input \"$input\" with region hint \"$regionsHint\" getSystem() returns \"$expected\"" {
            val actual = target.getSystemName(input, regionsHint)
            actual shouldBe expected
        }
    }

    listOf(
        Triple("U-Q", listOf(30000629), "U-QMOA"),
        Triple("U-Q", listOf(30001155), "U-QVWD"),
        Triple("U-Q", listOf(), null),
        Triple("U-Q", listOf(30000629, 30001155), null),
        Triple("EX6-AO", listOf(), "EX6-AO"),
        Triple("AND", listOf(), null),
    ).forEach { (input, systemHints, expected) ->
        "for input \"$input\" with system hints \"$systemHints\" getSystem() returns \"$expected\"" {
            val actual = target.getSystemName(input, emptyList(), systemHints)
            actual shouldBe expected
        }
    }
})
