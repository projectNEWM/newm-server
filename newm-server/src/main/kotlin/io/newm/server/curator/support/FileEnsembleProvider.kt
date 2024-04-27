package io.newm.server.curator.support

import org.apache.curator.ensemble.EnsembleProvider
import java.io.File

class FileEnsembleProvider(val filePath: String) : EnsembleProvider {
    override fun close() {}

    override fun start() {}

    override fun getConnectionString(): String = File(filePath).readText().trim()

    override fun setConnectionString(value: String) {}

    override fun updateServerListEnabled(): Boolean = false
}
