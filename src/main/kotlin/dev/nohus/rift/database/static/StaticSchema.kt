package dev.nohus.rift.database.static

import org.jetbrains.exposed.sql.Table

object SolarSystems : Table() {
    val solarSystemId = integer("solarSystemId")
    val solarSystemName = varchar("solarSystemName", 100)
    val regionId = integer("regionId")
    val constellationId = integer("constellationId")
    val sunTypeId = integer("sunTypeID")
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val x2d = double("x2d").nullable()
    val y2d = double("y2d").nullable()
    val security = double("security")
    val hasJoveObservatory = bool("hasJoveObservatory")
    val asteroidBeltCount = integer("asteroidBeltCount")
    val iceFieldCount = integer("iceFieldCount")
    override val primaryKey = PrimaryKey(solarSystemId)
}

object Regions : Table() {
    val regionId = integer("regionId")
    val regionName = varchar("regionName", 100)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val x2d = double("x2d").nullable()
    val y2d = double("y2d").nullable()
    override val primaryKey = PrimaryKey(regionId)
}

object Constellations : Table() {
    val constellationId = integer("constellationId")
    val constellationName = varchar("constellationName", 100)
    val regionId = integer("regionId")
    val x = double("x")
    val y = double("y")
    val z = double("z")
    override val primaryKey = PrimaryKey(constellationId)
}

object MapLayouts : Table() {
    val layoutId = integer("layoutId")
    val name = varchar("name", 100)
    val regionId = integer("regionId")
}

object MapLayout : Table() {
    val layoutId = integer("layoutId")
    val solarSystemId = integer("solarSystemId")
    val x = integer("x")
    val y = integer("y")
}

object RegionMapLayout : Table() {
    val regionId = integer("regionId")
    val x = integer("x")
    val y = integer("y")
}

object Ships : Table() {
    val typeId = integer("typeId")
    val name = varchar("name", 100)
    val shipClass = varchar("class", 100)
    override val primaryKey = PrimaryKey(typeId)
}

object Types : Table() {
    val typeId = integer("typeId")
    val groupId = integer("groupId")
    val categoryId = integer("categoryId")
    val typeName = varchar("typeName", 150)
    val volume = float("volume")
    val radius = float("radius").nullable()
    val repackagedVolume = float("repackagedVolume").nullable()
    val iconId = integer("iconID").nullable()
    override val primaryKey = PrimaryKey(typeId)
}

object TypeGroups : Table() {
    val groupId = integer("groupId")
    val groupName = varchar("groupName", 100)
    override val primaryKey = PrimaryKey(groupId)
}

object TypeCategories : Table() {
    val categoryId = integer("categoryId")
    val categoryName = varchar("categoryName", 100)
    override val primaryKey = PrimaryKey(categoryId)
}

object StarGates : Table() {
    val fromSystemId = integer("fromSystemId")
    val toSystemId = integer("toSystemId")
    val starGateTypeId = integer("starGateTypeId")
    val locationId = long("locationId")
}

object Stations : Table() {
    val id = integer("id")
    val typeId = integer("typeId")
    val systemId = integer("systemId")
    val name = varchar("name", 100)
    val corporationId = integer("corporationId")
    val hasLoyaltyPointsStore = bool("hasLoyaltyPointsStore")
    override val primaryKey = PrimaryKey(id)
}

object Planets : Table() {
    val id = integer("id")
    val typeId = integer("typeId")
    val systemId = integer("systemId")
    val name = varchar("name", 100)
    val radius = float("radius")
    override val primaryKey = PrimaryKey(id)
}

object PlanetaryIndustrySchematics : Table() {
    val id = integer("id")
    val cycleTime = long("cycleTime")
    override val primaryKey = PrimaryKey(id)
}

object PlanetaryIndustrySchematicsTypes : Table() {
    val id = integer("id")
    val typeId = integer("typeId")
    val quantity = integer("quantity")
    val isInput = bool("isInput")
}

object Celestials : Table() {
    val id = integer("id")
    val typeId = integer("typeId")
    val solarSystemId = integer("solarSystemId")
    val orbitId = integer("orbitId").nullable()
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val radius = double("radius").nullable()
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}

object TypeDogmas : Table() {
    val typeId = integer("typeId")
    val entityOverviewShipGroupId = integer("entityOverviewShipGroupId").nullable()
}

object Backdrops : Table() {
    val name = varchar("name", 100)
    val bytes = binary("values")
}
