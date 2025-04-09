package dev.nohus.rift.assets

object LocationFlags {

    private val flags = mapOf(
        "AssetSafety" to null, // Known from location
        "AutoFit" to null, // Inside a container
        "BoosterBay" to "增效剂舱",
        "Cargo" to "货柜舱",
        "CorporationGoalDeliveries" to "交付物",
        "CorpseBay" to "尸体舱",
        "Deliveries" to "交付物",
        "DroneBay" to "无人机舱",
        "FighterBay" to "铁骑舰载机舱",
        "FighterTube0" to "铁骑舰载机发射管 0",
        "FighterTube1" to "铁骑舰载机发射管 1",
        "FighterTube2" to "铁骑舰载机发射管 2",
        "FighterTube3" to "铁骑舰载机发射管 3",
        "FighterTube4" to "铁骑舰载机发射管 4",
        "FleetHangar" to "舰队机库",
        "FrigateEscapeBay" to "护卫舰逃生舱",
        "Hangar" to null, // Inside a hangar
        "HangarAll" to null,
        "HiSlot0" to "高槽 0",
        "HiSlot1" to "高槽 1",
        "HiSlot2" to "高槽 2",
        "HiSlot3" to "高槽 3",
        "HiSlot4" to "高槽 4",
        "HiSlot5" to "高槽 5",
        "HiSlot6" to "高槽 6",
        "HiSlot7" to "高槽 7",
        "HiddenModifiers" to "隐藏加成",
        "Implant" to "植入体",
        "InfrastructureHangar" to "基础设施机库",
        "LoSlot0" to "低槽 0",
        "LoSlot1" to "低槽 1",
        "LoSlot2" to "低槽 2",
        "LoSlot3" to "低槽 3",
        "LoSlot4" to "低槽 4",
        "LoSlot5" to "低槽 5",
        "LoSlot6" to "低槽 6",
        "LoSlot7" to "低槽 7",
        "Locked" to "已锁定",
        "MedSlot0" to "中槽 0",
        "MedSlot1" to "中槽 1",
        "MedSlot2" to "中槽 2",
        "MedSlot3" to "中槽 3",
        "MedSlot4" to "中槽 4",
        "MedSlot5" to "中槽 5",
        "MedSlot6" to "中槽 6",
        "MedSlot7" to "中槽 7",
        "MobileDepotHold" to "移动式仓库",
        "MoonMaterialBay" to "卫星材料舱",
        "QuafeBay" to "酷非舱",
        "RigSlot0" to "改装件槽 0",
        "RigSlot1" to "改装件槽 1",
        "RigSlot2" to "改装件槽 2",
        "RigSlot3" to "改装件槽 3",
        "RigSlot4" to "改装件槽 4",
        "RigSlot5" to "改装件槽 5",
        "RigSlot6" to "改装件槽 6",
        "RigSlot7" to "改装件槽 7",
        "ShipHangar" to "舰船机库",
        "Skill" to "技能",
        "SpecializedAmmoHold" to "弹药舱",
        "SpecializedAsteroidHold" to "小行星舱",
        "SpecializedCommandCenterHold" to "指挥中心舱",
        "SpecializedFuelBay" to "燃料舱",
        "SpecializedGasHold" to "气云舱",
        "SpecializedIceHold" to "冰矿舱",
        "SpecializedIndustrialShipHold" to "工业舰舱",
        "SpecializedLargeShipHold" to "大型舰船舱",
        "SpecializedMaterialBay" to "材料舱",
        "SpecializedMediumShipHold" to "中型舰船舱",
        "SpecializedMineralHold" to "矿物舱",
        "SpecializedOreHold" to "矿石舱",
        "SpecializedPlanetaryCommoditiesHold" to "行星产物舱",
        "SpecializedSalvageHold" to "打捞舱",
        "SpecializedShipHold" to "舰船舱",
        "SpecializedSmallShipHold" to "小型舰船舱",
        "StructureDeedBay" to "建筑蓝图舱",
        "SubSystemBay" to "子系统舱",
        "SubSystemSlot0" to "子系统 0",
        "SubSystemSlot1" to "子系统 1",
        "SubSystemSlot2" to "子系统 2",
        "SubSystemSlot3" to "子系统 3",
        "SubSystemSlot4" to "子系统 4",
        "SubSystemSlot5" to "子系统 5",
        "SubSystemSlot6" to "子系统 6",
        "SubSystemSlot7" to "子系统 7",
        "Unlocked" to null, // Inside an unlocked container
        "Wardrobe" to "衣柜",
    )

    fun getName(flag: String): String? {
        return flags[flag]
    }
}
