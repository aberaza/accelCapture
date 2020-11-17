package com.aberaza.accelcapture

/**
 * RingBuffer uses a fixed length array to implement a queue, where,
 * - [tail] Items are added to the tail
 * - [head] Items are removed from the head
 * - [capacity] Keeps track of how many items are currently in the queue
 */
class RingBuffer<T>(val maxSize: Int = 10) {
    val array = mutableListOf<T?>().apply {
        for (index in 0 until maxSize) {
            add(null)
        }
    }

    // Head - remove from the head (read index)
    var head = 0

    // Tail - add to the tail (write index)
    var tail = 0

    // How many items are currently in the queue
    var capacity = 0

    fun clear() {
        head = 0
        tail = 0
        capacity = 0
    }

    fun enqueue(item: T): RingBuffer<T> {
        // Check if there's space before attempting to add the item


        array[tail] = item
        // Loop around to the start of the array if there's a need for it
        tail = (tail + 1) % maxSize

        if (capacity == maxSize) {
            head = (head + 1) % maxSize
        } else {
            capacity++
        }
        return this
    }

    fun dequeue(): T? {
        // Check if queue is empty before attempting to remove the item
        if (capacity == 0) throw Exception(
            "Queue is empty, can't dequeue()")

        val result = array[head]
        // Loop around to the start of the array if there's a need for it
        head = (head + 1) % maxSize
        capacity--

        return result
    }

    fun peek(): T? = array[head]

    /**
     * - Ordinarily, T > H ([isNormal]).
     * - However, when the queue loops over, then T < H ([isFlipped]).
     */
    fun isNormal(): Boolean {
        return tail > head
    }

    fun isFlipped(): Boolean {
        return tail < head
    }

    fun isFull(): Boolean {
        return capacity == maxSize
    }

    override fun toString(): String = StringBuilder().apply {
        //this.append(contents().joinToString(", ", "{", "}"))
        this.append(" [capacity=$capacity, H=$head, T=$tail] A=${averageSum()}")
    }.toString()

    fun contents(): MutableList<T?> {
        return mutableListOf<T?>().apply {
            var itemCount = capacity
            var readIndex = head
            while (itemCount > 0) {
                add(array[readIndex])
                readIndex = (readIndex + 1) % maxSize
                itemCount--
            }
        }
    }

    fun averageSum(): Float {
        var returnValue = 0.0f

        if(capacity >=  maxSize) {
            var itemCount = capacity
            var readIndex = head
            var sumValue= 0.0f

            while (itemCount > 0) {
                sumValue += array[readIndex] as Float
                readIndex = (readIndex + 1) % maxSize
                itemCount--
            }
            returnValue = sumValue/capacity
        }
        return returnValue
    }
}