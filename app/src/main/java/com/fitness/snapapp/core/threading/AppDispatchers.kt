package com.fitness.snapapp.core.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Centralised coroutine dispatchers.
 * 'inference' gets its own single-thread executor so QAIRT calls are serialised.
 */
object AppDispatchers {
    /** Dedicated single thread for SNPE execute() — must be serialised */
    val inference: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "qairt-worker").also { it.priority = Thread.MAX_PRIORITY }
        }.asCoroutineDispatcher()

    val ui:       CoroutineDispatcher = Dispatchers.Main
    val database: CoroutineDispatcher = Dispatchers.IO
    val audio:    CoroutineDispatcher = Dispatchers.Default
    val compute:  CoroutineDispatcher = Dispatchers.Default  // exercise logic, posture
}
