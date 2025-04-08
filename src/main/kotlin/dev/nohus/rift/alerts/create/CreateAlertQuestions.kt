package dev.nohus.rift.alerts.create

import dev.nohus.rift.alerts.create.FormQuestion.CombatTargetQuestion
import dev.nohus.rift.alerts.create.FormQuestion.FreeformTextQuestion
import dev.nohus.rift.alerts.create.FormQuestion.IntelChannelQuestion
import dev.nohus.rift.alerts.create.FormQuestion.JumpsRangeQuestion
import dev.nohus.rift.alerts.create.FormQuestion.MultipleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.OwnedCharacterQuestion
import dev.nohus.rift.alerts.create.FormQuestion.PlanetaryIndustryColoniesQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SingleChoiceQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SoundQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SpecificCharactersQuestion
import dev.nohus.rift.alerts.create.FormQuestion.SystemQuestion
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.repositories.ShipTypesRepository

@Suppress("PropertyName")
class CreateAlertQuestions(
    shipTypesRepository: ShipTypesRepository,
    configurationPackRepository: ConfigurationPackRepository,
) {
    private var id = 0

    // Alert trigger
    val ALERT_TRIGGER_INTEL_REPORTED = FormChoiceItem(id = id++, text = "预警提示")
    val ALERT_TRIGGER_GAME_ACTION = FormChoiceItem(id = id++, text = "游戏内发生事件")
    val ALERT_TRIGGER_PLANETARY_INDUSTRY = FormChoiceItem(id = id++, text = "行星工业需要关注")
    val ALERT_TRIGGER_CHAT_MESSAGE = FormChoiceItem(id = id++, text = "收到聊天消息")
    val ALERT_TRIGGER_JABBER_PING = FormChoiceItem(id = id++, text = "收到Jabber通知")
    val ALERT_TRIGGER_JABBER_MESSAGE = FormChoiceItem(id = id++, text = "收到Jabber消息")
    val ALERT_TRIGGER_NO_MESSAGE = FormChoiceItem(id = id++, text = "未收到相关信息")
    val ALERT_TRIGGER_QUESTION = SingleChoiceQuestion(
        title = "在以下情况触发警报:",
        items = buildList {
            add(ALERT_TRIGGER_INTEL_REPORTED)
            add(ALERT_TRIGGER_GAME_ACTION)
            add(ALERT_TRIGGER_PLANETARY_INDUSTRY)
            add(ALERT_TRIGGER_CHAT_MESSAGE)
            if (configurationPackRepository.isJabberEnabled()) {
                add(ALERT_TRIGGER_JABBER_PING)
                add(ALERT_TRIGGER_JABBER_MESSAGE)
            }
            add(ALERT_TRIGGER_NO_MESSAGE)
        },
    )

    // Intel report type
    val INTEL_REPORT_TYPE_ANY_CHARACTER = FormChoiceItem(id = id++, text = "角色")
    val INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS = FormChoiceItem(id = id++, text = "特定角色")
    val INTEL_REPORT_TYPE_ANY_SHIP = FormChoiceItem(id = id++, text = "舰船")
    val INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES = FormChoiceItem(id = id++, text = "特定舰船类型")
    val INTEL_REPORT_TYPE_WORMHOLE = FormChoiceItem(id = id++, text = "虫洞")
    val INTEL_REPORT_TYPE_GATE_CAMP = FormChoiceItem(id = id++, text = "堵门")
    val INTEL_REPORT_TYPE_BUBBLES = FormChoiceItem(id = id++, text = "泡泡")
    val INTEL_REPORT_TYPE_QUESTION = MultipleChoiceQuestion(
        title = "如果报告包含以下内容:",
        items = listOf(
            INTEL_REPORT_TYPE_ANY_CHARACTER,
            INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS,
            INTEL_REPORT_TYPE_ANY_SHIP,
            INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES,
            INTEL_REPORT_TYPE_WORMHOLE,
            INTEL_REPORT_TYPE_GATE_CAMP,
            INTEL_REPORT_TYPE_BUBBLES,
        ),
    )

    // Intel report type, specific characters
    val INTEL_REPORT_TYPE_SPECIFIC_CHARACTERS_QUESTION = SpecificCharactersQuestion(
        title = "且报告的角色包括以下任意一个:",
        allowEmpty = false,
    )

    // Intel report type, specific ship classes
    val INTEL_REPORT_TYPE_SPECIFIC_SHIP_CLASSES_QUESTION = MultipleChoiceQuestion(
        title = "且报告的舰船包括以下任意一种:",
        items = shipTypesRepository.getShipClasses()
            .sorted()
            .mapIndexed { index, shipClass -> FormChoiceItem(index, shipClass) },
    )

    // Intel report location
    val INTEL_REPORT_LOCATION_SYSTEM = FormChoiceItem(id = id++, text = "指定星系")
    val INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER =
        FormChoiceItem(id = id++, text = "任意在线角色的位置")
    val INTEL_REPORT_LOCATION_OWNED_CHARACTER =
        FormChoiceItem(id = id++, text = "指定在线角色的位置")
    val INTEL_REPORT_LOCATION_QUESTION = SingleChoiceQuestion(
        title = "且报告位置在:",
        items = listOf(
            INTEL_REPORT_LOCATION_SYSTEM,
            INTEL_REPORT_LOCATION_ANY_OWNED_CHARACTER,
            INTEL_REPORT_LOCATION_OWNED_CHARACTER,
        ),
    )

    // Intel report location, system
    val INTEL_REPORT_LOCATION_SYSTEM_QUESTION = SystemQuestion(
        title = "星系名称:",
        allowEmpty = false,
    )

    // Intel report location, specific character
    val INTEL_REPORT_LOCATION_OWNED_CHARACTER_QUESTION = OwnedCharacterQuestion(
        title = "使用角色:",
    )

    // Intel report location, jumps range
    val INTEL_REPORT_LOCATION_JUMPS_RANGE_QUESTION = JumpsRangeQuestion(
        title = "距离:",
    )

    // Game action type
    val GAME_ACTION_TYPE_IN_COMBAT = FormChoiceItem(
        id = id++,
        text = "进入战斗",
        description = "包含你攻击别人和被攻击",
    )
    val GAME_ACTION_TYPE_UNDER_ATTACK = FormChoiceItem(
        id = id++,
        text = "你有危险",
        description = "包含被反跳和被毁电",
    )
    val GAME_ACTION_TYPE_ATTACKING = FormChoiceItem(
        id = id++,
        text = "你正在攻击",
    )
    val GAME_ACTION_TYPE_BEING_WARP_SCRAMBLED = FormChoiceItem(
        id = id++,
        text = "你被反跳",
    )
    val GAME_ACTION_TYPE_DECLOAKED = FormChoiceItem(
        id = id++,
        text = "你被破隐",
    )
    val GAME_ACTION_TYPE_COMBAT_STOPPED = FormChoiceItem(
        id = id++,
        text = "你离开了战斗",
        description = "包含你攻击别人和被攻击",
    )
    val GAME_ACTION_TYPE_QUESTION = MultipleChoiceQuestion(
        title = "当下列任意情况发生:",
        items = listOf(
            GAME_ACTION_TYPE_IN_COMBAT,
            GAME_ACTION_TYPE_UNDER_ATTACK,
            GAME_ACTION_TYPE_ATTACKING,
            GAME_ACTION_TYPE_BEING_WARP_SCRAMBLED,
            GAME_ACTION_TYPE_DECLOAKED,
            GAME_ACTION_TYPE_COMBAT_STOPPED,
        ),
    )

    // Game action type, combat target
    val GAME_ACTION_TYPE_COMBAT_TARGET_QUESTION = CombatTargetQuestion(
        title = "目标的名字包含:",
        placeholder = "Dark Blood",
        allowEmpty = true,
    )

    // Game action type, decloak exceptions
    val GAME_ACTION_TYPE_DECLOAKED_EXCEPTIONS_QUESTION = FreeformTextQuestion(
        title = "忽略被以下物体破隐:",
        placeholder = "逗号分隔的关键词列表，例如：星门",
        allowEmpty = true,
    )

    // Game action type, combat stopped, duration
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_10_SECONDS = FormChoiceItem(id = id++, text = "10秒")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_20_SECONDS = FormChoiceItem(id = id++, text = "20秒")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_30_SECONDS = FormChoiceItem(id = id++, text = "30秒")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_1_MINUTE = FormChoiceItem(id = id++, text = "1分钟")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_2_MINUTES = FormChoiceItem(id = id++, text = "2分钟")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_5_MINUTES = FormChoiceItem(id = id++, text = "5分钟")
    val GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_QUESTION = SingleChoiceQuestion(
        title = "未进入战斗时间:",
        items = listOf(
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_10_SECONDS,
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_20_SECONDS,
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_30_SECONDS,
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_1_MINUTE,
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_2_MINUTES,
            GAME_ACTION_TYPE_COMBAT_STOPPED_DURATION_5_MINUTES,
        ),
    )

    // Planetary Industry event type
    val PLANETARY_INDUSTRY_EVENT_TYPE_NOT_SETUP = FormChoiceItem(
        id = id++,
        text = "殖民地未设置",
        description = "未创建路线，未选择蓝图等",
    )
    val PLANETARY_INDUSTRY_EVENT_TYPE_EXTRACTOR_INACTIVE = FormChoiceItem(
        id = id++,
        text = "提取器已过期",
        description = "提取程序已完成",
    )
    val PLANETARY_INDUSTRY_EVENT_TYPE_STORAGE_FULL = FormChoiceItem(
        id = id++,
        text = "存储已满",
        description = "存储或发射台接收输出已满",
    )
    val PLANETARY_INDUSTRY_EVENT_TYPE_IDLE = FormChoiceItem(
        id = id++,
        text = "殖民地空闲",
        description = "生产已停止（例如因材料不足）",
    )
    val PLANETARY_INDUSTRY_EVENT_TYPE_QUESTION = MultipleChoiceQuestion(
        title = "当下列任意情况发生:",
        items = listOf(
            PLANETARY_INDUSTRY_EVENT_TYPE_EXTRACTOR_INACTIVE,
            PLANETARY_INDUSTRY_EVENT_TYPE_STORAGE_FULL,
            PLANETARY_INDUSTRY_EVENT_TYPE_IDLE,
            PLANETARY_INDUSTRY_EVENT_TYPE_NOT_SETUP,
        ),
    )

    // Planetary Industry colony filter
    val PLANETARY_INDUSTRY_COLONIES_QUESTION = PlanetaryIndustryColoniesQuestion(
        title = "在这些殖民地:",
    )

    // Planetary Industry alert before
    val PLANETARY_INDUSTRY_ALERT_BEFORE_NONE = FormChoiceItem(id = id++, text = "发生时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_5_MINUTES = FormChoiceItem(id = id++, text = "提前5分钟")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_15_MINUTES = FormChoiceItem(id = id++, text = "提前15分钟")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_30_MINUTES = FormChoiceItem(id = id++, text = "提前30分钟")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_1_HOUR = FormChoiceItem(id = id++, text = "提前1小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_2_HOURS = FormChoiceItem(id = id++, text = "提前2小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_4_HOURS = FormChoiceItem(id = id++, text = "提前4小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_8_HOURS = FormChoiceItem(id = id++, text = "提前8小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_12_HOURS = FormChoiceItem(id = id++, text = "提前12小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_24_HOURS = FormChoiceItem(id = id++, text = "提前24小时")
    val PLANETARY_INDUSTRY_ALERT_BEFORE_QUESTION = SingleChoiceQuestion(
        title = "触发时间:",
        items = listOf(
            PLANETARY_INDUSTRY_ALERT_BEFORE_NONE,
            PLANETARY_INDUSTRY_ALERT_BEFORE_5_MINUTES,
            PLANETARY_INDUSTRY_ALERT_BEFORE_15_MINUTES,
            PLANETARY_INDUSTRY_ALERT_BEFORE_30_MINUTES,
            PLANETARY_INDUSTRY_ALERT_BEFORE_1_HOUR,
            PLANETARY_INDUSTRY_ALERT_BEFORE_2_HOURS,
            PLANETARY_INDUSTRY_ALERT_BEFORE_4_HOURS,
            PLANETARY_INDUSTRY_ALERT_BEFORE_8_HOURS,
            PLANETARY_INDUSTRY_ALERT_BEFORE_12_HOURS,
            PLANETARY_INDUSTRY_ALERT_BEFORE_24_HOURS,
        ),
    )

    // Chat message, channel type
    val CHAT_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "任意频道")
    val CHAT_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "指定频道")
    val CHAT_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "在:",
        items = listOf(
            CHAT_MESSAGE_CHANNEL_ANY,
            CHAT_MESSAGE_CHANNEL_SPECIFIC,
        ),
    )

    // Chat message, specific channel
    val CHAT_MESSAGE_SPECIFIC_CHANNEL_QUESTION = FreeformTextQuestion(
        title = "频道名称:",
        placeholder = "频道名称",
        allowEmpty = false,
    )

    // Chat message, sender
    val CHAT_MESSAGE_SENDER_QUESTION = FreeformTextQuestion(
        title = "发送者是:",
        placeholder = "角色名称。留空表示任意。",
        allowEmpty = true,
    )

    // Chat message, message contains
    val CHAT_MESSAGE_MESSAGE_CONTAINING_QUESTION = FreeformTextQuestion(
        title = "消息包含:",
        placeholder = "消息内容。留空表示任意。",
        allowEmpty = true,
    )

    // Jabber ping, ping type
    val JABBER_PING_TYPE_FLEET = FormChoiceItem(id = id++, text = "舰队通知")
    val JABBER_PING_TYPE_MESSAGE = FormChoiceItem(id = id++, text = "消息通知")
    val JABBER_PING_TYPE_QUESTION = SingleChoiceQuestion(
        title = "且它是:",
        items = listOf(
            JABBER_PING_TYPE_FLEET,
            JABBER_PING_TYPE_MESSAGE,
        ),
    )

    // Jabber ping, fleet ping, fleet commander
    val JABBER_PING_FLEET_COMMANDER_QUESTION = SpecificCharactersQuestion(
        title = "且舰队指挥官是以下任意一个:",
        allowEmpty = true,
    )

    // Jabber ping, fleet ping, formup system
    val JABBER_PING_FLEET_FORMUP_SYSTEM_QUESTION = SystemQuestion(
        title = "且舰队集结星系是:",
        allowEmpty = true,
    )

    // Jabber ping, fleet ping, PAP type
    val JABBER_PING_FLEET_PAP_TYPE_STRATEGIC = FormChoiceItem(id = id++, text = "战略")
    val JABBER_PING_FLEET_PAP_TYPE_PEACETIME = FormChoiceItem(id = id++, text = "和平时期")
    val JABBER_PING_FLEET_PAP_TYPE_ANY = FormChoiceItem(id = id++, text = "任意")
    val JABBER_PING_FLEET_PAP_TYPE_QUESTION = SingleChoiceQuestion(
        title = "且PAP类型是:",
        items = listOf(
            JABBER_PING_FLEET_PAP_TYPE_STRATEGIC,
            JABBER_PING_FLEET_PAP_TYPE_PEACETIME,
            JABBER_PING_FLEET_PAP_TYPE_ANY,
        ),
    )

    // Jabber ping, fleet ping, doctrine
    val JABBER_PING_FLEET_DOCTRINE_QUESTION = FreeformTextQuestion(
        title = "且配置包含:",
        placeholder = "留空表示任意。",
        allowEmpty = true,
    )

    // Jabber ping, target
    val JABBER_PING_TARGET_QUESTION = FreeformTextQuestion(
        title = "且目标是:",
        placeholder = "例如 \"蜂巢\"。留空表示任意。",
        allowEmpty = true,
    )

    // Jabber message, channel type
    val JABBER_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "任意聊天")
    val JABBER_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "指定聊天")
    val JABBER_MESSAGE_CHANNEL_DIRECT_MESSAGE = FormChoiceItem(id = id++, text = "私聊消息")
    val JABBER_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "在:",
        items = listOf(
            JABBER_MESSAGE_CHANNEL_ANY,
            JABBER_MESSAGE_CHANNEL_SPECIFIC,
            JABBER_MESSAGE_CHANNEL_DIRECT_MESSAGE,
        ),
    )

    // Jabber message, specific channel
    val JABBER_MESSAGE_SPECIFIC_CHANNEL_QUESTION = FreeformTextQuestion(
        title = "聊天名称:",
        placeholder = "聊天名称（用户或房间）",
        allowEmpty = false,
    )

    // Jabber message, sender
    val JABBER_MESSAGE_SENDER_QUESTION = FreeformTextQuestion(
        title = "发送者是:",
        placeholder = "用户名。留空表示任意。",
        allowEmpty = true,
    )

    // Jabber message, message contains
    val JABBER_MESSAGE_MESSAGE_CONTAINING_QUESTION = FreeformTextQuestion(
        title = "消息包含:",
        placeholder = "消息内容。留空表示任意。",
        allowEmpty = true,
    )

    // No message channel type
    val NO_MESSAGE_CHANNEL_ALL = FormChoiceItem(id = id++, text = "所有监控频道")
    val NO_MESSAGE_CHANNEL_ANY = FormChoiceItem(id = id++, text = "任意监控频道")
    val NO_MESSAGE_CHANNEL_SPECIFIC = FormChoiceItem(id = id++, text = "指定频道")
    val NO_MESSAGE_CHANNEL_TYPE_QUESTION = SingleChoiceQuestion(
        title = "在:",
        items = listOf(
            NO_MESSAGE_CHANNEL_ALL,
            NO_MESSAGE_CHANNEL_ANY,
            NO_MESSAGE_CHANNEL_SPECIFIC,
        ),
    )

    // No message channel type, specific channel
    val NO_MESSAGE_CHANNEL_SPECIFIC_QUESTION = IntelChannelQuestion(
        title = "频道名称:",
    )

    // No message duration
    val NO_MESSAGE_DURATION_2_MINUTES = FormChoiceItem(id = id++, text = "2分钟")
    val NO_MESSAGE_DURATION_5_MINUTES = FormChoiceItem(id = id++, text = "5分钟")
    val NO_MESSAGE_DURATION_10_MINUTES = FormChoiceItem(id = id++, text = "10分钟")
    val NO_MESSAGE_DURATION_20_MINUTES = FormChoiceItem(id = id++, text = "20分钟")
    val NO_MESSAGE_DURATION_30_MINUTES = FormChoiceItem(id = id++, text = "30分钟")
    val NO_MESSAGE_DURATION_QUESTION = SingleChoiceQuestion(
        title = "持续时间至少:",
        items = listOf(
            NO_MESSAGE_DURATION_2_MINUTES,
            NO_MESSAGE_DURATION_5_MINUTES,
            NO_MESSAGE_DURATION_10_MINUTES,
            NO_MESSAGE_DURATION_20_MINUTES,
            NO_MESSAGE_DURATION_30_MINUTES,
        ),
    )

    // Alert action
    val ALERT_ACTION_RIFT_NOTIFICATION = FormChoiceItem(id = id++, text = "发送RIFT通知", description = "详细通知弹窗")
    val ALERT_ACTION_SYSTEM_NOTIFICATION = FormChoiceItem(id = id++, text = "发送系统通知", description = "简单文本通知")
    val ALERT_ACTION_PUSH_NOTIFICATION = FormChoiceItem(id = id++, text = "发送移动推送通知", description = "在RIFT设置中配置")
    val ALERT_ACTION_PLAY_SOUND = FormChoiceItem(id = id++, text = "播放声音")
    val ALERT_ACTION_SHOW_COLONIES = FormChoiceItem(id = id++, text = "显示殖民地", description = "打开行星工业窗口")
    val ALERT_ACTION_SHOW_PING = FormChoiceItem(id = id++, text = "显示通知", description = "打开通知窗口")
    val ALERT_ACTION_QUESTION = MultipleChoiceQuestion(
        title = "当此警报触发时:",
        items = listOf(
            ALERT_ACTION_RIFT_NOTIFICATION,
            ALERT_ACTION_SYSTEM_NOTIFICATION,
            ALERT_ACTION_PUSH_NOTIFICATION,
            ALERT_ACTION_PLAY_SOUND,
        ),
    )

    // Alert action (Planetary Industry ping version)
    val ALERT_ACTION_PLANETARY_INDUSTRY_QUESTION = MultipleChoiceQuestion(
        title = "当此警报触发时:",
        items = listOf(
            ALERT_ACTION_SHOW_COLONIES,
            ALERT_ACTION_RIFT_NOTIFICATION,
            ALERT_ACTION_SYSTEM_NOTIFICATION,
            ALERT_ACTION_PUSH_NOTIFICATION,
            ALERT_ACTION_PLAY_SOUND,
        ),
    )

    // Alert action (Jabber ping version)
    val ALERT_ACTION_JABBER_PING_QUESTION = MultipleChoiceQuestion(
        title = "当此警报触发时:",
        items = listOf(
            ALERT_ACTION_SHOW_PING,
            ALERT_ACTION_PUSH_NOTIFICATION,
            ALERT_ACTION_PLAY_SOUND,
        ),
    )

    // Alert action, sound
    val ALERT_ACTION_SOUND_QUESTION = SoundQuestion(
        title = "使用声音:",
    )

    // Alert cooldown
    val ALERT_COOLDOWN_NONE = FormChoiceItem(id = id++, text = "每次触发")
    val ALERT_COOLDOWN_30_SECONDS = FormChoiceItem(id = id++, text = "30秒")
    val ALERT_COOLDOWN_1_MINUTE = FormChoiceItem(id = id++, text = "1分钟")
    val ALERT_COOLDOWN_2_MINUTES = FormChoiceItem(id = id++, text = "2分钟")
    val ALERT_COOLDOWN_5_MINUTES = FormChoiceItem(id = id++, text = "5分钟")
    val ALERT_COOLDOWN_10_MINUTES = FormChoiceItem(id = id++, text = "10分钟")
    val ALERT_COOLDOWN_QUESTION = SingleChoiceQuestion(
        title = "冷却时间:",
        items = listOf(
            ALERT_COOLDOWN_NONE,
            ALERT_COOLDOWN_30_SECONDS,
            ALERT_COOLDOWN_1_MINUTE,
            ALERT_COOLDOWN_2_MINUTES,
            ALERT_COOLDOWN_5_MINUTES,
            ALERT_COOLDOWN_10_MINUTES,
        ),
    )
}
