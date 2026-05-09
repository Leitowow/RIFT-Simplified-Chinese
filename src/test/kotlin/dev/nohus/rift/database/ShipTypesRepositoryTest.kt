package dev.nohus.rift.database

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.utils.HasNonAsciiWindowsUsernameUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class ShipTypesRepositoryTest : FreeSpec({

    val database = StaticDatabase(SqliteInitializer(HasNonAsciiWindowsUsernameUseCase(OperatingSystem.Linux, AppDirectories(LinuxDirectories()))))
    val namesRepository: NamesRepository = mockk()
    val typesRepository = TypesRepository(database, namesRepository)
    val target = ShipTypesRepository(
        staticDatabase = database,
        typesRepository = typesRepository,
    )

    listOf(
        "Buzzard" to "Buzzard",
        "buzzard" to "Buzzard",
        "buZZaRD" to "Buzzard",
        "Caldari Shuttle" to "Caldari Shuttle",
        "caldari shuttle" to "Caldari Shuttle",
        "kiki" to "Kikimora",
        "execuror" to "Exequror",
        "Exequror Navy" to "Exequror Navy Issue",
        "execuror navy" to "Exequror Navy Issue",
        "Exequror Navy Issue" to "Exequror Navy Issue",
        "Augoror Navy Issue" to "Augoror Navy Issue",
        "navy drake" to "Drake Navy Issue",
        "navy comet" to "Federation Navy Comet",
        "fleet cyclone" to "Cyclone Fleet Issue",
        "Auguror Navy Issue" to null,
        "cyclone fleet" to null,
        "navy osprey navy" to null,
        "invalid" to null,
        "" to null,
        "Buzzar" to null,
        " Buzzard" to null,
        "Buzzard " to null,
    ).forEach { (input, expectedName) ->
        val expected = expectedName?.let { typesRepository.getType(expectedName) }
        "for input \"$input\", getFuzzyShip() returns \"$expected\"" {
            val actual = target.getFuzzyShip(input)
            actual shouldBe expected
        }
    }
})
