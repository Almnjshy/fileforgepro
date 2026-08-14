package com.fileforge.pro.core.common

/**
 * App-wide dispatcher contract. UI code injects this and never touches
 * kotlinx.coroutines.Dispatchers directly — easier to test and to swap
 * for a fake scheduler in unit tests (Master Spec §65 — Performance).
 */
interface AppDispatchers {
    val main: kotlinx.coroutines.CoroutineDispatcher
    val io: kotlinx.coroutines.CoroutineDispatcher
    val default: kotlinx.coroutines.CoroutineDispatcher
    val unconfined: kotlinx.coroutines.CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val main = kotlinx.coroutines.Dispatchers.Main
    override val io = kotlinx.coroutines.Dispatchers.IO
    override val default = kotlinx.coroutines.Dispatchers.Default
    override val unconfined = kotlinx.coroutines.Dispatchers.Unconfined
}
