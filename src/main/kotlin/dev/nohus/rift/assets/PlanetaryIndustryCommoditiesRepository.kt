package dev.nohus.rift.assets

import dev.nohus.rift.repositories.TypesRepository
import org.koin.core.annotation.Single

@Single
class PlanetaryIndustryCommoditiesRepository(
    private val typesRepository: TypesRepository,
) {

    private val p0Names = listOf(
        "水状液体", "自养生物", "基础金属", "碳化合物",
        "复杂有机物", "长英质岩浆", "重金属", "离子溶液", "微生物", "惰性气体",
        "贵金属", "非晶体", "浮游生物群", "活性气体", "悬浮等离子体",
    )
    private val p1Names = listOf(
        "水", "工业纤维", "活性金属", "生物燃料", "蛋白质",
        "硅", "有毒金属", "电解质", "细菌", "氧气", "贵金属", "手性结构", "生物质",
        "氧化化合物", "等离子体团",
    )
    private val p2Names = listOf(
        "生物电池", "建筑模块", "消费电子产品", "冷却剂",
        "浓缩铀", "肥料", "基因增强牲畜", "牲畜", "机械零件",
        "微纤维护盾", "微型电子产品", "纳米机器人", "氧化物", "聚芳酰胺", "聚酯纤维",
        "火箭燃料", "硅酸盐玻璃", "超导体", "超张力塑料", "合成油", "测试培养物",
        "发射器", "病毒体", "水冷CPU",
    )
    private val p3Names = listOf(
        "生物技术研究报告", "摄像无人机", "冷凝物",
        "低温保护溶液", "数据芯片", "凝胶基质生物浆", "制导系统", "危险物质检测系统",
        "密封膜", "高科技发射器", "工业炸药", "神经通讯器", "核反应堆",
        "行星载具", "机器人", "智能工厂单元", "超级计算机", "合成突触",
        "经颅微控制器", "乌科米超导体", "疫苗",
    )
    private val p4Names = listOf(
        "广播节点",
        "完整性响应无人机",
        "纳米工厂",
        "有机砂浆喷涂器",
        "递归计算模块",
        "自协调动力核心",
        "无菌导管",
        "湿件主机",
    )
    private val ids = (p0Names + p1Names + p2Names + p3Names + p4Names).mapNotNull { typesRepository.getTypeId(it) }

    fun isPlanetaryIndustryItems(items: List<Int>): Boolean {
        return items.distinct().all { it in ids }
    }
}
