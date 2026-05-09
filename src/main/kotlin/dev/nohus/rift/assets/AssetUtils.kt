package dev.nohus.rift.assets

import dev.nohus.rift.assets.AssetsViewModel.Asset

fun Asset.getTotalItems(): Int {
    return 1 + children.sumOf { it.getTotalItems() }
}

fun Asset.getTotalPrice(): Double {
    val price = price?.let { it * quantity } ?: 0.0
    val childrenPrice = children.sumOf { it.getTotalPrice() }
    return price + childrenPrice
}

fun Asset.getTotalVolume(): Double {
    val volume = type.repackagedVolume ?: type.volume
    val totalVolume = volume * quantity
    val childrenVolume = children.sumOf { it.getTotalVolume() }
    return totalVolume + childrenVolume
}
