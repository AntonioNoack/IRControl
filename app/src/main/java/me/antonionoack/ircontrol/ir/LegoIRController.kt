package me.antonionoack.ircontrol.ir

import android.hardware.ConsumerIrManager

class LegoIRController(private val manager: ConsumerIrManager) : Thread() {

    private var isActive = false
    private var run = true

    fun requestStop() {
        run = false
    }

    override fun run() {
        isActive = true
        try {
            while (run) {
                // ... send the signals
                for (c in Channel.values()) {
                    c.RED.tick()
                    c.BLUE.tick()
                    if (c.RED.active > 0 || c.BLUE.active > 0) {
                        val s0 = 4 + c.id
                        val s1 = c.BLUE.speed
                        val s2 = c.RED.speed
                        val s3 = 15 xor s0 xor s1 xor s2
                        // send 01id, bbbb, aaaa llll
                        manager.transmit(
                            38000, intArrayOf(
                                TR, START,
                                TR, i(s0, 3),
                                TR, i(s0, 2),
                                TR, i(s0, 1),
                                TR, i(s0, 0),
                                TR, i(s1, 3),
                                TR, i(s1, 2),
                                TR, i(s1, 1),
                                TR, i(s1, 0),
                                TR, i(s2, 3),
                                TR, i(s2, 2),
                                TR, i(s2, 1),
                                TR, i(s2, 0),
                                TR, i(s3, 3),
                                TR, i(s3, 2),
                                TR, i(s3, 1),
                                TR, i(s3, 0),
                                TR, START
                            )
                        )
                    }
                }
                // one round is about 10ms
                // ... wait a little
                sleep(1)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        isActive = false
    }

    companion object {
        // in us
        private const val START = (26.3 * 39).toInt()
        private const val ZERO = (26.3 * 10).toInt()
        private const val ONE = (26.3 * 21).toInt()
        private const val TR = (26.3 * 6).toInt()
        private fun i(data: Int, pos: Int): Int {
            return if (data shr pos and 1 != 0) ONE else ZERO
        }
    }
}