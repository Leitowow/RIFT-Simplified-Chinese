package dev.nohus.rift.planetaryindustry.models

import dev.nohus.rift.repositories.TypesRepository.Type

data class ColonyOverview(
    /**
     * Products of this colony that aren't also consumed by this colony
     */
    val finalProducts: Set<Type>,

    /**
     * Total capacity of these storages where final products end up
     */
    val capacity: Int,

    /**
     * Total capacity used by other commodities in storages where final products end up
     */
    val otherUsedCapacity: Double,

    /**
     * Total capacity used by final products in storages where final products end up
     */
    val finalProductsUsedCapacity: Double,
)

fun getColonyOverview(
    routes: List<Route>,
    pins: List<Pin>,
): ColonyOverview {
    val finalProducts = getFinalProducts(pins)
    val finalProductDestinationIds = routes
        .filter { it.type in finalProducts }
        .map { it.destinationPinId }
        .distinct()
    val finalProductStoragePins = pins.filter { it.id in finalProductDestinationIds }
    val totalCapacity = finalProductStoragePins.sumOf { it.getCapacity() ?: 0 }
    val totalOtherUsedCapacity = finalProductStoragePins.sumOf { pin ->
        pin.contents.entries
            .filter { it.key !in finalProducts }
            .sumOf { it.key.volume.toDouble() * it.value }
    }
    val totalFinalProductsUsedCapacity = finalProductStoragePins.sumOf { pin ->
        pin.contents.entries
            .filter { it.key in finalProducts }
            .sumOf { it.key.volume.toDouble() * it.value }
    }
    return ColonyOverview(
        finalProducts = finalProducts,
        capacity = totalCapacity,
        otherUsedCapacity = totalOtherUsedCapacity,
        finalProductsUsedCapacity = totalFinalProductsUsedCapacity,
    )
}

private fun getFinalProducts(pins: List<Pin>): Set<Type> {
    val producing = getProducing(pins) + getExtracting(pins)
    val consuming = getConsuming(pins)
    return producing - consuming
}

private fun getProducing(pins: List<Pin>): Set<Type> {
    return pins.filterIsInstance<Pin.Factory>()
        .mapNotNull { it.schematic }
        .map { it.outputType }
        .toSet()
}

private fun getExtracting(pins: List<Pin>): Set<Type> {
    return pins.filterIsInstance<Pin.Extractor>()
        .mapNotNull { it.productType }
        .toSet()
}

private fun getConsuming(pins: List<Pin>): Set<Type> {
    return pins.filterIsInstance<Pin.Factory>()
        .mapNotNull { it.schematic }
        .flatMap { it.inputs.map { it.key } }
        .toSet()
}
