package com.lxdnz.nz.compassapplication

/**
 * Created by alex on 4/03/18.
 */
class LowPass{
    /*
     * Time smoothing constant for low-pass filter 0 ? ? ? 1 ; a smaller value
     * basically means more smoothing See:
     * http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    private val ALPHA = 0.2f

    /**
     * Filter the given input against the previous values and return a low-pass
     * filtered result.
     *
     * @param input
     * float array to smooth.
     * @param prev
     * float array representing the previous values.
     * @return float array smoothed with a low-pass filter.
     */
    fun filter(input: FloatArray?, prev: FloatArray?): FloatArray {
        if (input == null || prev == null) throw NullPointerException("input and prev float arrays must be non-NULL")
        if (input.size != prev.size) throw IllegalArgumentException("input and prev must be the same length")

        for (i in input.indices) {
            prev[i] = prev[i] + ALPHA * (input[i] - prev[i])
        }
        return prev
    }
}