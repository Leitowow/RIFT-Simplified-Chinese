package dev.nohus.rift.map.markers

sealed interface MapMarkersInputModel {
    data object New : MapMarkersInputModel
    data class AddToSystem(val systemId: Int) : MapMarkersInputModel
}
