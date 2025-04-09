package dev.nohus.rift.pings

import org.koin.core.annotation.Single

@Single
class PingTranslator {
    private val keywordTranslations = mapOf(
        // 船型翻译索引
        "Flycatchers" to "飞猎者",
        "Boosters" to "加成跳驱",
        "Harpy" to "女妖级",
        "Torp Bombers" to "隐轰队",
        "Purifiers" to "净化级",
        "Hounds" to "猎犬级",
        "Astero" to "小白",
        "Else" to "其他",
        

    )

    fun translate(text: String): String {
        var translatedText = text
        keywordTranslations.forEach { (en, zh) ->
            translatedText = translatedText.replace(en, zh)
        }
        return translatedText
    }
} 