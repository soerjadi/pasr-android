package id.oemah.nusantara.feature.orders

import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.StatusChange
import id.oemah.nusantara.testing.testOrder
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderTimelineTest {
    private val labels = listOf("Pesanan dibuat", "Diproses penjual", "Siap diambil", "Selesai")

    private fun states(status: OrderStatus) = OrderTimeline.steps(testOrder(status = status)).map { it.state }

    @Test fun created_has_the_first_step_current() {
        assertEquals(listOf(StepState.CURRENT, StepState.PENDING, StepState.PENDING, StepState.PENDING), states(OrderStatus.CREATED))
    }

    @Test fun seller_processing_marks_the_first_done() {
        assertEquals(listOf(StepState.DONE, StepState.CURRENT, StepState.PENDING, StepState.PENDING), states(OrderStatus.SELLER_PROCESSING))
    }

    @Test fun ready_for_pickup_marks_two_done() {
        assertEquals(listOf(StepState.DONE, StepState.DONE, StepState.CURRENT, StepState.PENDING), states(OrderStatus.READY_FOR_PICKUP))
    }

    @Test fun completed_has_every_step_done() {
        assertEquals(List(4) { StepState.DONE }, states(OrderStatus.COMPLETED))
    }

    @Test fun the_four_steps_use_the_spec_labels_in_order() {
        assertEquals(labels, OrderTimeline.steps(testOrder(status = OrderStatus.COMPLETED)).map { it.label })
    }

    @Test fun a_cancellation_after_processing_keeps_two_done_steps_then_a_cancelled_step_and_nothing_pending() {
        val order = testOrder(
            status = OrderStatus.CANCELLED,
            history = listOf(
                StatusChange(OrderStatus.CREATED, "2026-10-15T03:02:00Z"),
                StatusChange(OrderStatus.SELLER_PROCESSING, "2026-10-15T04:02:00Z"),
                StatusChange(OrderStatus.CANCELLED, "2026-10-15T05:00:00Z"),
            ),
        )

        val steps = OrderTimeline.steps(order)

        assertEquals(listOf("Pesanan dibuat", "Diproses penjual", "Dibatalkan"), steps.map { it.label })
        assertEquals(listOf(StepState.DONE, StepState.DONE, StepState.CANCELLED), steps.map { it.state })
        assertEquals(Instant.parse("2026-10-15T05:00:00Z"), steps.last().at)
    }

    @Test fun a_cancellation_right_after_creation_has_one_done_step() {
        val order = testOrder(
            status = OrderStatus.CANCELLED,
            history = listOf(StatusChange(OrderStatus.CREATED, "2026-10-15T03:02:00Z"), StatusChange(OrderStatus.CANCELLED, "2026-10-15T03:10:00Z")),
        )

        assertEquals(listOf(StepState.DONE, StepState.CANCELLED), OrderTimeline.steps(order).map { it.state })
    }

    @Test fun reached_steps_carry_their_dates_and_pending_steps_have_none() {
        val order = testOrder(
            status = OrderStatus.SELLER_PROCESSING,
            history = listOf(
                StatusChange(OrderStatus.CREATED, "2026-10-15T03:02:00Z"),
                StatusChange(OrderStatus.SELLER_PROCESSING, "2026-10-15T04:02:00Z"),
            ),
        )

        val steps = OrderTimeline.steps(order)

        assertEquals(Instant.parse("2026-10-15T03:02:00Z"), steps[0].at)
        assertEquals(Instant.parse("2026-10-15T04:02:00Z"), steps[1].at)
        assertNull(steps[2].at)
        assertNull(steps[3].at)
    }

    @Test fun a_missing_or_garbled_history_entry_gives_no_date_but_never_crashes() {
        val order = testOrder(status = OrderStatus.READY_FOR_PICKUP, history = listOf(StatusChange(OrderStatus.CREATED, "bukan tanggal")))

        val steps = OrderTimeline.steps(order)

        assertNull(steps[0].at)
        assertNull(steps[1].at)
        assertEquals(StepState.CURRENT, steps[2].state)
    }
}
