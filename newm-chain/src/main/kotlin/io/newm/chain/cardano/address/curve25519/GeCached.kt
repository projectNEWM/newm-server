package io.newm.chain.cardano.address.curve25519

class GeCached(
    internal val yPlusX: Fe,
    internal val yMinusX: Fe,
    internal val z: Fe,
    internal val t2d: Fe
)
