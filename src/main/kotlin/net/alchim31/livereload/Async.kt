package net.alchim31.livereload

import java.util.*
import kotlin.coroutines.experimental.*

fun launch(block: suspend () -> Unit) = launch(block, EmptyCoroutineContext)
fun launch(block: suspend () -> Unit, context: CoroutineContext) =
  block.startCoroutine(StandaloneCoroutine(context))

private class StandaloneCoroutine(override val context: CoroutineContext): Continuation<Unit> {
  override fun resume(value: Unit) {}
  override fun resumeWithException(exception: Throwable) {}
}

private object SleepTimer{
  val timer = Timer("net.alchim31.livereload.SleepTimer")
}

suspend fun sleep(delayMilliseconds: Long):Unit = suspendCoroutine {
  SleepTimer.timer.schedule(object : TimerTask() {
    override fun run() {
      this.cancel()
      it.resume(Unit)
    }
  }, delayMilliseconds)
}
