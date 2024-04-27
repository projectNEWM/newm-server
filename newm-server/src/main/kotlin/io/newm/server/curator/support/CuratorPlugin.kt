package io.newm.server.curator.support

import io.ktor.server.application.Application
import io.ktor.server.application.Plugin
import io.ktor.util.AttributeKey
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory

class CuratorPlugin(val client: CuratorFramework) {
    init {
        client.start()
    }

    companion object : Plugin<Application, CuratorFrameworkFactory.Builder, CuratorPlugin> {
        override val key: AttributeKey<CuratorPlugin> = AttributeKey(CuratorPlugin::class.qualifiedName!!)

        override fun install(
            pipeline: Application,
            configure: CuratorFrameworkFactory.Builder.() -> Unit
        ): CuratorPlugin {
            val builder = CuratorFrameworkFactory.builder()
            configure(builder)
            return CuratorPlugin(builder.build())
        }
    }
}
