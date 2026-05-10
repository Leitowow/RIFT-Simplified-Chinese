package dev.nohus.rift.planetaryindustry

import dev.nohus.rift.planetaryindustry.CopyType.Excel
import dev.nohus.rift.planetaryindustry.CopyType.ExcelWithAddin
import dev.nohus.rift.planetaryindustry.CopyType.GoogleSheets
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.planetaryindustry.models.Colony
import dev.nohus.rift.planetaryindustry.models.ColonyStatus
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Extracting
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Idle
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NeedsAttention
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.NotSetup
import dev.nohus.rift.planetaryindustry.models.ColonyStatus.Producing
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.PinStatus
import dev.nohus.rift.planetaryindustry.simulation.ExtractionSimulation.Companion.getProgramOutputPrediction
import dev.nohus.rift.repositories.TypesRepository.Type
import java.time.Duration
import kotlin.math.roundToInt

object SpreadsheetFormatter {

    fun format(type: CopyType, items: List<ColonyItem>): String {
        return when (type) {
            GoogleSheets -> formatForGoogleSheets(items)
            Excel -> formatForExcel(items)
            ExcelWithAddin -> formatForExcelWithAddin(items)
        }
    }

    private fun formatForGoogleSheets(items: List<ColonyItem>): String {
        val types = getAllTypes(items)
        val extractedTypes = getAllExtractedTypes(items)
        val rows = items.joinToString("\n") { item ->
            val colony = item.colony
            val contents = getColonyContents(colony)
            val finalProducts = colony.overview.finalProducts.joinToString(",") { it.name }
            val status = colony.status.getDisplayName()
            val totalUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity + colony.overview.otherUsedCapacity)
            val finalProductsUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity)
            val otherUsedCapacity = String.format("%.02f", colony.overview.otherUsedCapacity)
            val expiryTimestamp = getGoogleSheetsExpiryTimestamp(item)
            val expiryReasons = getExpiryReason(item.ffwdColony.status)
            val averagesPerHourExtracted = getAveragesPerHourExtracted(colony)
            listOf(
                item.characterName,
                colony.characterId,
                colony.planet.name,
                colony.type.name,
                colony.system.name,
                colony.system.id,
                status,
                colony.status.isWorking,
                finalProducts,
                colony.overview.capacity,
                totalUsedCapacity,
                finalProductsUsedCapacity,
                otherUsedCapacity,
                expiryTimestamp,
                expiryReasons,
                *extractedTypes.map { averagesPerHourExtracted[it] ?: 0 }.toTypedArray(),
                *types.map { contents[it] ?: 0 }.toTypedArray(),
            ).joinToString(separator = "\t")
        }
        val headers = listOf(
            "角色名",
            "角色ID",
            "行星名",
            "行星类型",
            "星系名",
            "星系ID",
            "状态",
            "是否工作中",
            "最终产品",
            "产品容量",
            "已用容量",
            "已用容量（产品）",
            "已用容量（其他）",
            "到期时间",
            "到期原因",
            *extractedTypes.map { "每小时平均（${it.name}）" }.toTypedArray(),
            *types.map { "库存（${it.name}）" }.toTypedArray(),
        ).joinToString("\t")
        return "$headers\n$rows"
    }

    private fun formatForExcel(items: List<ColonyItem>): String {
        val types = getAllTypes(items)
        val extractedTypes = getAllExtractedTypes(items)
        val rows = items.joinToString("\n") { item ->
            val colony = item.colony
            val contents = getColonyContents(colony)
            val finalProducts = colony.overview.finalProducts.joinToString(",") { it.name }
            val status = colony.status.getDisplayName()
            val totalUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity + colony.overview.otherUsedCapacity)
            val finalProductsUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity)
            val otherUsedCapacity = String.format("%.02f", colony.overview.otherUsedCapacity)
            val expiryTimestamp = getExcelExpiryTimestamp(item)
            val expiryReasons = getExpiryReason(item.ffwdColony.status)
            val averagesPerHourExtracted = getAveragesPerHourExtracted(colony)
            listOf(
                item.characterName,
                colony.characterId,
                colony.planet.name,
                colony.type.name,
                colony.system.name,
                colony.system.id,
                status,
                colony.status.isWorking,
                finalProducts,
                colony.overview.capacity,
                totalUsedCapacity,
                finalProductsUsedCapacity,
                otherUsedCapacity,
                expiryTimestamp,
                expiryReasons,
                *extractedTypes.map { averagesPerHourExtracted[it] ?: 0 }.toTypedArray(),
                *types.map { contents[it] ?: 0 }.toTypedArray(),
            ).joinToString(separator = "\t")
        }
        val headers = listOf(
            "角色名",
            "角色ID",
            "行星名",
            "行星类型",
            "星系名",
            "星系ID",
            "状态",
            "是否工作中",
            "最终产品",
            "产品容量",
            "已用容量",
            "已用容量（产品）",
            "已用容量（其他）",
            "到期时间（日期）",
            "到期原因",
            *extractedTypes.map { "每小时平均（${it.name}）" }.toTypedArray(),
            *types.map { "库存（${it.name}）" }.toTypedArray(),
        ).joinToString("\t")
        return "$headers\n$rows"
    }

    private fun formatForExcelWithAddin(items: List<ColonyItem>): String {
        val types = getAllTypes(items)
        val extractedTypes = getAllExtractedTypes(items)
        val maxFinalProducts = items.maxOf { it.colony.overview.finalProducts.size }
        val rows = items.joinToString("\n") { item ->
            val colony = item.colony
            val contents = getColonyContents(colony)
            val finalProducts = colony.overview.finalProducts.map {
                "=EVEONLINE.TYPE(${it.id})"
            }.toTypedArray()
            val status = colony.status.getDisplayName()
            val totalUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity + colony.overview.otherUsedCapacity)
            val finalProductsUsedCapacity = String.format("%.02f", colony.overview.finalProductsUsedCapacity)
            val otherUsedCapacity = String.format("%.02f", colony.overview.otherUsedCapacity)
            val expiryTimestamp = getExcelExpiryTimestamp(item)
            val expiryReasons = getExpiryReason(item.ffwdColony.status)
            val averagesPerHourExtracted = getAveragesPerHourExtracted(colony)
            listOf(
                "=EVEONLINE.CHARACTER(${colony.characterId})",
                colony.planet.name,
                "=EVEONLINE.TYPE(${colony.planet.type.typeId})",
                "=EVEONLINE.SOLARSYSTEM(${colony.system.id})",
                status,
                colony.status.isWorking,
                *finalProducts,
                *List(maxFinalProducts - finalProducts.size) { "" }.toTypedArray(),
                colony.overview.capacity,
                totalUsedCapacity,
                finalProductsUsedCapacity,
                otherUsedCapacity,
                expiryTimestamp,
                expiryReasons,
                *extractedTypes.map { averagesPerHourExtracted[it] ?: 0 }.toTypedArray(),
                *types.map { contents[it] ?: 0 }.toTypedArray(),
            ).joinToString(separator = "\t")
        }
        val headers = listOf(
            "角色",
            "行星名",
            "行星类型",
            "星系",
            "状态",
            "是否工作中",
            *List(maxFinalProducts) { "最终产品" }.toTypedArray(),
            "产品容量",
            "已用容量",
            "已用容量（产品）",
            "已用容量（其他）",
            "到期时间（日期）",
            "到期原因",
            *extractedTypes.map { "每小时平均（${it.name}）" }.toTypedArray(),
            *types.map { "库存（${it.name}）" }.toTypedArray(),
        ).joinToString("\t")
        return "$headers\n$rows"
    }

    private fun getAllTypes(items: List<ColonyItem>): List<Type> {
        return items.flatMap { item ->
            item.colony.pins.flatMap { it.contents.keys }
        }.distinct().sortedBy { it.name }
    }

    private fun getColonyContents(colony: Colony): Map<Type, Long> {
        return colony.pins
            .flatMap { it.contents.entries }
            .groupingBy({ it.key })
            .fold(0L) { accumulator, element -> accumulator + element.value }
    }

    private fun getAllExtractedTypes(items: List<ColonyItem>): List<Type> {
        return items.flatMap { item ->
            item.colony.pins.filterIsInstance<Pin.Extractor>().mapNotNull { it.productType }
        }.distinct().sortedBy { it.name }
    }

    private fun getAveragesPerHourExtracted(colony: Colony): Map<Type, Int> {
        return colony.pins.filterIsInstance<Pin.Extractor>().mapNotNull { extractor ->
            extractor.productType?.let { it to (extractor.getAveragePerHour() ?: 0) }
        }.groupBy { it.first }.mapValues { entry -> entry.value.sumOf { it.second } }
    }

    private fun ColonyStatus.getDisplayName() = when (this) {
        is Extracting -> "开采中"
        is Producing -> "生产中"
        is NotSetup -> "未设置"
        is NeedsAttention -> "需要注意"
        is Idle -> "闲置"
    }

    private fun PinStatus.getDisplayName() = when (this) {
        PinStatus.Extracting -> "开采中"
        PinStatus.ExtractorExpired -> "采集器已过期"
        PinStatus.ExtractorInactive -> "采集器未激活"
        PinStatus.FactoryIdle -> "工厂闲置"
        PinStatus.InputNotRouted -> "输入未建立路线"
        PinStatus.NotSetup -> "未设置"
        PinStatus.OutputNotRouted -> "输出未建立路线"
        PinStatus.Producing -> "生产中"
        PinStatus.Static -> "静态"
        PinStatus.StorageFull -> "仓储已满"
    }

    private fun getGoogleSheetsExpiryTimestamp(item: ColonyItem): String {
        val expiryTimestamp = item.ffwdColony.currentSimTime.toEpochMilli() / 1000
        return "=EPOCHTODATE($expiryTimestamp)"
    }

    private fun getExcelExpiryTimestamp(item: ColonyItem): String {
        val expiryTimestamp = item.ffwdColony.currentSimTime.toEpochMilli() / 1000
        return "=($expiryTimestamp/86400)+25569"
    }

    private fun getExpiryReason(status: ColonyStatus): String {
        return if (status is Idle) {
            "闲置"
        } else {
            status.pins.map { it.status.getDisplayName() }.distinct().joinToString(",")
        }
    }

    private fun Pin.Extractor.getAveragePerHour(): Int? {
        if (isActive) {
            if (installTime != null && expiryTime != null && baseValue != null && cycleTime != null) {
                val totalProgramDuration = Duration.between(installTime, expiryTime)
                val totalCycles = (totalProgramDuration.toSeconds() / cycleTime.toSeconds()).toInt()
                val prediction = getProgramOutputPrediction(baseValue, cycleTime, totalCycles)
                val totalMined = prediction.sum()
                val averagePerHour = (totalMined / totalProgramDuration.toHours().toFloat()).roundToInt()
                return averagePerHour
            }
        }
        return null
    }
}
