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
            "Character name",
            "Character ID",
            "Planet name",
            "Planet type",
            "System name",
            "System ID",
            "Status",
            "Is working",
            "Final products",
            "Product capacity",
            "Used capacity",
            "Used capacity (products)",
            "Used capacity (other)",
            "Expires at",
            "Expiry reason",
            *extractedTypes.map { "Avg. per hour (${it.name})" }.toTypedArray(),
            *types.map { "Stored (${it.name})" }.toTypedArray(),
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
            "Character name",
            "Character ID",
            "Planet name",
            "Planet type",
            "System name",
            "System ID",
            "Status",
            "Is working",
            "Final products",
            "Product capacity",
            "Used capacity",
            "Used capacity (products)",
            "Used capacity (other)",
            "Expires at (Date)",
            "Expiry reason",
            *extractedTypes.map { "Avg. per hour (${it.name})" }.toTypedArray(),
            *types.map { "Stored (${it.name})" }.toTypedArray(),
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
            "Character",
            "Planet name",
            "Planet type",
            "System",
            "Status",
            "Is working",
            *List(maxFinalProducts) { "Final product" }.toTypedArray(),
            "Product capacity",
            "Used capacity",
            "Used capacity (products)",
            "Used capacity (other)",
            "Expires at (Date)",
            "Expiry reason",
            *extractedTypes.map { "Avg. per hour (${it.name})" }.toTypedArray(),
            *types.map { "Stored (${it.name})" }.toTypedArray(),
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
        is Extracting -> "Extracting"
        is Producing -> "Producing"
        is NotSetup -> "Not setup"
        is NeedsAttention -> "Needs attention"
        is Idle -> "Idle"
    }

    private fun PinStatus.getDisplayName() = when (this) {
        PinStatus.Extracting -> "Extracting"
        PinStatus.ExtractorExpired -> "Extractor expired"
        PinStatus.ExtractorInactive -> "Extractor inactive"
        PinStatus.FactoryIdle -> "Factory idle"
        PinStatus.InputNotRouted -> "Input not routed"
        PinStatus.NotSetup -> "Not setup"
        PinStatus.OutputNotRouted -> "Output not routed"
        PinStatus.Producing -> "Producing"
        PinStatus.Static -> "Static"
        PinStatus.StorageFull -> "Storage full"
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
            "Idle"
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
