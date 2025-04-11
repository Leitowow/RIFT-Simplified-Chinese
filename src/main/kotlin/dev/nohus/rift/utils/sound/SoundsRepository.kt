package dev.nohus.rift.utils.sound

import org.koin.core.annotation.Single

@Single
class SoundsRepository {

    private val soundResources = listOf(
        Sound(id = 1, resource = "1j.wav", name = "1跳外有敌对"),
        Sound(id = 2, resource = "2j.wav", name = "2跳外有敌对"),
        Sound(id = 3, resource = "3j.wav", name = "3跳外有敌对"),
        Sound(id = 4, resource = "4j.wav", name = "4跳外有敌对"),
        Sound(id = 5, resource = "5j.wav", name = "5跳外有敌对"),
        Sound(id = 6, resource = "local-alarm.wav", name = "本地有敌对"),
        Sound(id = 7, resource = "bubbled.wav", name = "泡泡"),
        Sound(id = 8, resource = "gate-camp.wav", name = "堵门"),
        Sound(id = 9, resource = "wormhole.wav", name = "虫洞"),
        Sound(id = 10, resource = "PAP.wav", name = "舰队集结Ping"),
        Sound(id = 11, resource = "specific-character.wav", name = "指定角色提醒"),
        Sound(id = 12, resource = "specific-ship.wav", name = "指定船型提醒"),
        Sound(id = 13, resource = "specific-system.wav", name = "指定星系提醒"),
        Sound(id = 14, resource = "Engage.wav", name = "进入战斗"),
        Sound(id = 15, resource = "Disengage.wav", name = "离开战斗"),
        Sound(id = 16, resource = "tackled.wav", name = "被反跳"),
        Sound(id = 17, resource = "decloacking.wav", name = "隐身解除"),
        Sound(id = 18, resource = "chat-ingame.wav", name = "游戏内私聊"),
        Sound(id = 19, resource = "chat-jabber.wav", name = "jabber有新的消息"),
        Sound(id = 20, resource = "PI.wav", name = "行星工业提醒"),
    )

    fun getSounds(): List<Sound> {
        return soundResources
    }
}
