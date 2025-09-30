package com.freehands.assistant.utils

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe object pool for float arrays to reduce memory allocations.
 *
 * @param arraySize The size of each array in the pool
 * @param maxPoolSize Maximum number of arrays to keep in the pool
 * @param clearOnRelease Whether to clear the array when returning it to the pool
 */
class FloatArrayPool(
    private val arraySize: Int,
    private val maxPoolSize: Int,
    private val clearOnRelease: Boolean = true
) {
    private val pool: BlockingQueue<FloatArray> = ArrayBlockingQueue(maxPoolSize)
    private val createdCount = AtomicInteger(0)
    private val activeCount = AtomicInteger(0)

    /**
     * Acquires an array from the pool or creates a new one if the pool is empty.
     */
    fun acquire(): FloatArray {
        val array = pool.poll()
        return if (array != null) {
            array
        } else {
            createdCount.incrementAndGet()
            FloatArray(arraySize)
        }.also {
            activeCount.incrementAndGet()
        }
    }

    /**
     * Returns an array to the pool for reuse.
     */
    fun release(array: FloatArray) {
        if (clearOnRelease) {
            array.fill(0f)
        }
        if (pool.size < maxPoolSize) {
            pool.offer(array)
        }
        activeCount.decrementAndGet()
    }

    /**
     * Gets the number of arrays created by this pool.
     */
    fun getCreatedCount(): Int = createdCount.get()

    /**
     * Gets the number of arrays currently in use.
     */
    fun getActiveCount(): Int = activeCount.get()

    /**
     * Gets the number of arrays available in the pool.
     */
    fun getAvailableCount(): Int = pool.size

    companion object {
        /**
         * Creates a new array pool with default settings.
         */
        @JvmStatic
        fun createDefaultPool(arraySize: Int): FloatArrayPool {
            return FloatArrayPool(
                arraySize = arraySize,
                maxPoolSize = Runtime.getRuntime().availableProcessors() * 2,
                clearOnRelease = true
            )
        }
    }
}
