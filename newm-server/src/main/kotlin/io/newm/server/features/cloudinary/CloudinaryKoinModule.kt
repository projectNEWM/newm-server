package io.newm.server.features.cloudinary

import com.cloudinary.Cloudinary
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.staticmembers.StaticMembers
import io.newm.shared.ktx.getConfigString
import org.koin.dsl.module

val cloudinaryKoinModule =
    module {
        single { Cloudinary(get<ApplicationEnvironment>().getConfigString(StaticMembers.getCloudinaryUrl())) }
    }
