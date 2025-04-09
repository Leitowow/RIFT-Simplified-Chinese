package dev.nohus.rift.pings

import org.koin.core.annotation.Single

@Single
class PingTranslator {
    private val keywordTranslations = mapOf(
        // 船型翻译索引
        "Flycatchers" to "轻拦队",
        "Flycatcher" to "飞燕级",
        "Kirin/Scalpel" to "麒麟级/手术刀级",
        "Boosters" to "加成跳驱",
        "Harpy" to "女妖级",
        "Torp Bombers" to "隐轰队",
        "Purifier" to "净化级",
        "Hound" to "猎犬级",
        "Astero" to "小白",
        "Else" to "其他",
        "Ferox Navy Issue" to "猛鲑级海军型(FNI)",
        "Basilisk" to "皇冠蜥级",
        "Support" to "电子战船/抓人船",

    )

    fun translate(text: String): String {
        var translatedText = text
        keywordTranslations.forEach { (en, zh) ->
            translatedText = translatedText.replace(en, zh)
        }
        return translatedText
    }
} 
