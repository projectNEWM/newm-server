package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.util.extractAssetMetadata
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.txbuilder.ktx.cborHexToPlutusData
import org.junit.jupiter.api.Test
import java.math.BigInteger

class Cip68DatumTest {

    @Test
    fun `test CIP68 datum`() {
        val createdUtxoSet = setOf(
            CreatedUtxo(
                address = "addr_test1xqyeshaz2w77449uepcap6p8pud9ju3rjckddxdv9qxf787m46qvthcky2x42khv4jyuen72ksjtqrapzw28jdy3vtdqnkrmz5",
                addressType = "00",
                stakeAddress = "stake_test17rd6aqx9mutz9r24ttk2ezwvel9tgf9sp7s389rexjgk9kssedugy",
                hash = "5916ad353cadca8cef2951bbdd47500325fb9d8d149237327f17edda8d283650",
                ix = 0L,
                lovelace = BigInteger("1706760"),
                datumHash = null,
                datum = "d8799fa3446e616d6548537061636542756445696d6167654b697066733a2f2f7465737445696d616765583061723a2f2f66355738525a6d4151696d757a5f7679744659396f66497a6439517047614449763255587272616854753401ff",
                scriptRef = null,
                nativeAssets = listOf(
                    NativeAsset(
                        policy = "169e3ad7038f23049f3ed7fff137b1b29bccaba4732035477c15b4c9",
                        name = "000643b00211c612bdc6ab89e8797f796fb3d83232b70139a484b25e022ab2",
                        amount = BigInteger.ONE
                    )
                ),
                cbor = null,
            )
        )

        val result = cip68UtxoOutputsTo721MetadataMap(createdUtxoSet)
        val ledgerAssetMetadata = result[0].first.extractAssetMetadata(result[0].second)
        assertThat(ledgerAssetMetadata).isNotEmpty()
    }

    private fun cip68UtxoOutputsTo721MetadataMap(createdUtxos: Set<CreatedUtxo>): List<Pair<MetadataMap, List<LedgerAsset>>> {
        return createdUtxos.filter { createdUtxo ->
            createdUtxo.nativeAssets.any { nativeAsset ->
                nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
            }
        }.mapNotNull { cip68CreatedUtxo ->
            cip68CreatedUtxo.datum?.let { datum ->
                val cip68PlutusData = datum.cborHexToPlutusData()
                if (cip68PlutusData.hasConstr() && cip68PlutusData.constr == 0) {
                    cip68CreatedUtxo.nativeAssets.filter { nativeAsset ->
                        nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
                    }.map { nativeAsset ->
                        val metadataMap = cip68PlutusData.toMetadataMap(nativeAsset.policy, nativeAsset.name)
                        metadataMap to cip68CreatedUtxo.nativeAssets.map { na ->
                            LedgerAsset(1, na.policy, na.name, BigInteger.ONE)
                        }
                    }
                } else {
                    null
                }
            }
        }.flatten()
    }

    companion object {
        private val CIP68_REFERENCE_TOKEN_REGEX = Regex("^000643b0.*$") // (100)TokenName
    }
}
