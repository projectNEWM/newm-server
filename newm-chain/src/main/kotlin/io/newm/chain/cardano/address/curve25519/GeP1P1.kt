package io.newm.chain.cardano.address.curve25519

data class GeP1P1(internal val x: Fe, internal val y: Fe, internal val z: Fe, internal val t: Fe) {
    fun toFull(): Ge {
        val x = this.x * this.t
        val y = this.y * this.z
        val z = this.z * this.t
        val t = this.x * this.y
        return Ge(x, y, z, t)
    }

    fun toPartial(): GePartial {
        val x = this.x * this.t
        val y = this.y * this.z
        val z = this.z * this.t
        return GePartial(x, y, z)
    }
}
