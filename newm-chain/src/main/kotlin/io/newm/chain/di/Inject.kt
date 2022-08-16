package io.newm.server.di

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> inject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = GlobalContext.get().inject<T>(qualifier = qualifier, parameters = parameters)
