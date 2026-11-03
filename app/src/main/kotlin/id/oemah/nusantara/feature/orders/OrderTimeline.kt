package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.api.model.StoreOrder
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

    fun steps(order: Order): List<TimelineStep> = steps(order.status, order.statusHistory)

    fun steps(order: StoreOrder): List<TimelineStep> = steps(order.status, order.statusHistory)

    fun steps(status: OrderStatus, statusHistory: List<StatusChange>): List<TimelineStep> {
        val at = statusHistory.mapNotNull { change -> parse(change.at)?.let { change.status to it } }.toMap()

        if (status == OrderStatus.CANCELLED) {
            val reached = statusHistory.mapNotNull { PATH.indexOf(it.status).takeIf { i -> i >= 0 } }.maxOrNull() ?: 0
            return PATH.take(reached + 1).map { TimelineStep(it.label(), StepState.DONE, at[it]) } +
                TimelineStep(OrderStatus.CANCELLED.label(), StepState.CANCELLED, at[OrderStatus.CANCELLED])
        }

        val current = PATH.indexOf(status)
        return PATH.mapIndexed { index, s ->
            val state = when {
                index < current -> StepState.DONE
                index == current -> if (s == OrderStatus.COMPLETED) StepState.DONE else StepState.CURRENT
                else -> StepState.PENDING
            }
            TimelineStep(s.label(), state, at[s])
        }
    }

    private fun parse(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()
}
