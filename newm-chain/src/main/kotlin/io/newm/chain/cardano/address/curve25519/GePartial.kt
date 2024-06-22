package io.newm.chain.cardano.address.curve25519

data class GePartial(
    internal val x: Fe,
    internal val y: Fe,
    internal val z: Fe
) {
    private fun doubleP1P1(): GeP1P1 {
        val xx = x.square()
        val yy = y.square()
        val b = z.squareAndDouble()
        val a = x + y
        val aa = a.square()
        val y3 = yy + xx
        val z3 = yy - xx
        val x3 = aa - y3
        val t3 = b - z3

        return GeP1P1(x3, y3, z3, t3)
    }

    fun double(): GePartial = doubleP1P1().toPartial()

    fun doubleFull(): Ge = doubleP1P1().toFull()
}
