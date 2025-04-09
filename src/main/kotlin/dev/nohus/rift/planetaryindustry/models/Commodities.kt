package dev.nohus.rift.planetaryindustry.models

object Commodities {

    private val p0Names = listOf(
        "Aqueous Liquids", "Autotrophs", "Base Metals", "Carbon Compounds",
        "Complex Organisms", "Felsic Magma", "Heavy Metals", "Ionic Solutions", "Microorganisms", "Noble Gas",
        "Noble Metals", "Non-CS Crystals", "Planktic Colonies", "Reactive Gas", "Suspended Plasma",
    )
    private val p1Names = listOf(
        "Water", "Industrial Fibers", "Reactive Metals", "Biofuels", "Proteins",
        "Silicon", "Toxic Metals", "Electrolytes", "Bacteria", "Oxygen", "Precious Metals", "Chiral Structures", "Biomass",
        "Oxidizing Compound", "Plasmoids",
    )
    private val p2Names = listOf(
        "Biocells", "Construction Blocks", "Consumer Electronics", "Coolant",
        "Enriched Uranium", "Fertilizer", "Genetically Enhanced Livestock", "Livestock", "Mechanical Parts",
        "Microfiber Shielding", "Miniature Electronics", "Nanites", "Oxides", "Polyaramids", "Polytextiles",
        "Rocket Fuel", "Silicate Glass", "Superconductors", "Supertensile Plastics", "Synthetic Oil", "Test Cultures",
        "Transmitter", "Viral Agent", "Water-Cooled CPU",
    )
    private val p3Names = listOf(
        "Biotech Research Reports", "Camera Drones", "Condensates",
        "Cryoprotectant Solution", "Data Chips", "Gel-Matrix Biopaste", "Guidance Systems", "Hazmat Detection Systems",
        "Hermetic Membranes", "High-Tech Transmitters", "Industrial Explosives", "Neocoms", "Nuclear Reactors",
        "Planetary Vehicles", "Robotics", "Smartfab Units", "Supercomputers", "Synthetic Synapses",
        "Transcranial Microcontrollers", "Ukomi Superconductors", "Vaccines",
    )
    private val p4Names = listOf(
        "Broadcast Node",
        "Integrity Response Drones",
        "Nano-Factory",
        "Organic Mortar Applicators",
        "Recursive Computing Module",
        "Self-Harmonizing Power Core",
        "Sterile Conduits",
        "Wetware Mainframe",
    )

    fun getTierName(commodity: String): String {
        return when (commodity) {
            in p0Names -> "原材料"
            in p1Names -> "一级"
            in p2Names -> "二级"
            in p3Names -> "三级"
            in p4Names -> "四级"
            else -> "未知"
        }
    }
}
