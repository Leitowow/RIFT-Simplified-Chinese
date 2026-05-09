package dev.nohus.rift.wallet

class TypeDetails(private val details: Map<Long, TypeDetail>) {

    operator fun get(id: Long?): TypeDetail? {
        if (id == null) return null
        return details[id]
    }

    operator fun get(id: Long): TypeDetail? {
        return details[id]
    }
}
