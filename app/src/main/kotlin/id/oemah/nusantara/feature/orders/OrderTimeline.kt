package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.api.model.OrderStatus
import java.time.Instant

enum class StepState { DONE, CURRENT, PENDING, CANCELLED }

data class TimelineStep(val label: String, val state: StepState, val at: Instant?)

object OrderTimeline {
    private val PATH = listOf(
        OrderStatus.CREATED,
        OrderStatus.SELLER_PROCESSING,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.COMPLETED,
    )

    /**
     * The four-step path for a live order. A cancelled order shows the steps it actually reached
     * followed by a "Dibatalkan" step, never steps it can no longer take.
     */
    fun steps(order: Order): List<TimelineStep> {
        val at = order.statusHistory.mapNotNull { change -> parse(change.at)?.let { change.status to it } }.toMap()

        if (order.status == OrderStatus.CANCELLED) {
            val reached = order.statusHistory.mapNotNull { PATH.indexOf(it.status).takeIf { i -> i >= 0 } }.maxOrNull() ?: 0
            return PATH.take(reached + 1).map { TimelineStep(it.label(), StepState.DONE, at[it]) } +
                TimelineStep(OrderStatus.CANCELLED.label(), StepState.CANCELLED, at[OrderStatus.CANCELLED])
        }

        val current = PATH.indexOf(order.status)
        return PATH.mapIndexed { index, status ->
            val state = when {
                index < current -> StepState.DONE
                index == current -> if (status == OrderStatus.COMPLETED) StepState.DONE else StepState.CURRENT
                else -> StepState.PENDING
            }
            TimelineStep(status.label(), state, at[status])
        }
    }

    private fun parse(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()
}
