package dev.nohus.rift.database.local

import dev.nohus.rift.repositories.character.CharacterAffiliationRepository
import dev.nohus.rift.repositories.character.CharactersRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.koin.core.annotation.Single
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class CleanupLocalDatabaseUseCase(
    private val localDatabase: LocalDatabase,
) {

    suspend operator fun invoke() {
        val currentTime = Instant.now().toEpochMilli()
        val deletedAffiliations = localDatabase.transaction {
            CharacterAffiliations.deleteWhere {
                CharacterAffiliations.checkTimestamp less currentTime - CharacterAffiliationRepository.expiryDuration
            }
        }
        val deletedCharacters = localDatabase.transaction {
            Characters2.deleteWhere {
                Characters2.checkTimestamp less currentTime - CharactersRepository.expiryDuration
            }
        }
        if (deletedAffiliations > 0 || deletedCharacters > 0) {
            logger.info { "Deleted expired rows from database: $deletedAffiliations affiliations and $deletedCharacters characters" }
        }
    }
}
