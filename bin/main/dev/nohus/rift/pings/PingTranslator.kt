package dev.nohus.rift.pings

import org.koin.core.annotation.Single

@Single
class PingTranslator {
    private val keywordTranslations = mapOf(
        // 船型翻译索引
        "Flycatchers" to "轻拦队",
        "Flycatcher" to "Flycatcher(飞燕级)",
        "Kirin/Scalpel" to "Kirin/Scalpel(麒麟级/手术刀级)",
        "Boosters" to "Boosters(加成跳驱)",
        "Harpy" to "Harpy(女妖级)",
        "Torp Bombers" to "Torp Bombers(隐轰队)",
        "Purifier" to "Purifier(净化级)",
        "Hound" to "Hound(猎犬级)",
        "Astero" to "Astero(阿斯特罗级)",
        "Else" to "Else(其他)",
        "Ferox Navy Issue" to "Ferox Navy Issue(猛鲑级海军型)",
        "Basilisk" to "Basilisk(皇冠蜥级)",
        "Support" to "Support(电子战船/抓人船)",
        "Logi" to "Logi(后勤)"
        "Kikis" to "Kikis(奇奇莫拉级)"
        "Slasher" to "Slasher(伐木者级)"
        "Hyena" to "Hyena(土狼级)"
        "Keres" to "Keres(克勒斯级)"
        "Moa" to "Moa(巨鸟级)"
        "Osprey" to "Osprey(鱼鹰级)"  
        "RNI" to "RNI(乌鸦级海军型)"
        "Rokh" to "Rokh(鹏鲲级)"
        "Scythe" to "Scythe(镰刀级)"
        "ONI" to "ONI(鱼鹰级海军型)"
        "Hound" to "Hound(猎犬级)"
        "Manticore" to "Manticore(蝎尾怪级)"
        "Nemesis" to "Nemesis(纳美西斯级)"
        "ceptors" to "ceptors(截击)"
        "Cyclone Fleet Issues" to "Cyclone Fleet Issues(飓风级舰队型)"
        "CFI" to "CFI(飓风级舰队型)"
        "Huginn" to "Huginn(休津级)"
        "Lach" to "Lach(拉克希斯级)"
        "Svipul" to "Svipul(斯威普级)"
        "Deacon" to "Deacon(执事级)"
        "ENI" to "ENI(送葬者级海军型)"
        "Sleipnir" to "Sleipnir(斯雷普尼级)"
        "Drake" to "Drake(幼龙级)"


    )

    fun translate(text: String): String {
        var translatedText = text
        keywordTranslations.forEach { (en, zh) ->
            translatedText = translatedText.replace(en, zh)
        }
        return translatedText
    }
} 
