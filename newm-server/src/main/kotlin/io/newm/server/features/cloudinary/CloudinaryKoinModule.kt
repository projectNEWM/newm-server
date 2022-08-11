package io.projectnewm.server.features.cloudinary

import com.cloudinary.Cloudinary
import io.ktor.server.application.ApplicationEnvironment
import io.projectnewm.server.ext.getConfigString
import org.koin.dsl.module

val cloudinaryKoinModule = module {
    single { Cloudinary(get<ApplicationEnvironment>().getConfigString("cloudinary.url")) }
}
