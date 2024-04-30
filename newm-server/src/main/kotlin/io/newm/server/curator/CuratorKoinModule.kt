package io.newm.server.curator

import io.ktor.server.application.Application
import io.ktor.server.application.pluginRegistry
import io.newm.server.curator.support.CuratorPlugin
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.koin.dsl.module

val curatorKoinModule =
    module {
        single<CuratorFramework> {
            get<Application>().pluginRegistry[CuratorPlugin.key].client
        }

        factory { LeaderLatch(get(), it.get<String>()) }
        factory { InterProcessMutex(get(), it.get<String>()) }
    }
