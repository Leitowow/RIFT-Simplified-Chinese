package dev.nohus.rift.repositories

import dev.nohus.rift.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class WordsRepository(
    typesRepository: TypesRepository,
) {

    private val words = getDictionaryWords() + getSlangWords()
    private val typeNames = typesRepository.getAllTypeNames().toSet()

    private fun getDictionaryWords(): Set<String> {
        return runBlocking {
            String(Res.readBytes("files/words")).lines().toSet()
        }
    }

    private fun getSlangWords(): Set<String> {
        return setOf("pls", "lol", "blops", "ansi")
    }

    fun isWord(text: String) = text.lowercase() in words

    fun isTypeName(text: String) = text in typeNames
}
