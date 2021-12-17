package bk.app.testapp

import androidx.recyclerview.widget.ListUpdateCallback
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.rules.TestName

import org.junit.Rule

class HiddenItemsTest {

    @get:Rule
    var testName = TestName()

    private data class NotifyEvent(
        val eventType: EventType,
        val val1: Int = -1,
        val val2: Int = 0,
    ) {
        enum class EventType { Insert, Remove, Move }
    }

    private val notifyList = arrayListOf<NotifyEvent>()

    private val hiddenImpl = HidingItemsImpl().apply {
        setListener(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                notifyList.add(
                    NotifyEvent(
                        eventType = NotifyEvent.EventType.Insert,
                        val1 = position,
                        val2 = count
                    )
                )
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyList.add(
                    NotifyEvent(
                        eventType = NotifyEvent.EventType.Remove,
                        val1 = position,
                        val2 = count
                    )
                )
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyList.add(
                    NotifyEvent(
                        eventType = NotifyEvent.EventType.Move,
                        val1 = fromPosition,
                        val2 = toPosition
                    )
                )
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
            }
        })
    }

    private val listNotifyList = arrayListOf<NotifyEvent>()
    private val list = arrayListOf<Boolean>()

    private fun ArrayList<Boolean>.localPos(index: Int): Int {
        var local = 0
        for (i in 0 until index) if (get(i)) local++
        return local
    }

    private fun <T> ArrayList<T>.fill(value: T, fromIndex: Int, toIndex: Int) {
        for (i in fromIndex until toIndex) set(i, value)
    }

    private fun HidingItemsImpl.Bucket.log(): String = StringBuilder()
        .apply {
            if (entries.isEmpty()) {
                appendLine("\tentries empty")
            } else {
                entries.forEachIndexed { i, entry ->
                    appendLine("\tentry $i: index=${entry.index} pos=${entry.position} hidden=${entry.hidden}")
                }
            }
        }
        .toString()

    private fun HidingItemsImpl.log(full: Boolean = false): String = StringBuilder()
        .apply {
            if (table.isEmpty()) {
                appendLine("table empty")
            } else {
                table.forEachIndexed { i, bucket ->
                    appendLine("bucket $i: index=${bucket.index} pos=${bucket.position} len=${bucket.entries.size}")
                    if (full) {
                        append(bucket.log())
                    }
                }
            }
        }
        .toString()

    private fun fromHidden(hidden: HidingItemsImpl, size: Int): Array<Boolean> {
        return Array(size) { !hidden.isHidden(it) }
    }

    private fun positions(data: List<Boolean>): Array<Int> {
        val result = mutableListOf<Int>()
        data.forEachIndexed { index, b -> if (b) result.add(index) }
        return result.toTypedArray()
    }

    private fun positionsFromHidden(hidden: HidingItemsImpl, size: Int): Array<Int> {
        val result = mutableListOf<Int>()
        repeat(hidden.positionByIndex(size)) { i -> result.add(hidden.indexByPosition(i)) }
        return result.toTypedArray()
    }

    private fun checkHidden() {
        assertArrayEquals(list.toArray(), fromHidden(hiddenImpl, list.size))
        assertArrayEquals(positions(list), positionsFromHidden(hiddenImpl, list.size))
        assertArrayEquals(listNotifyList.toArray(), notifyList.toArray())
    }

    private fun hideElements(fromIndex: Int, count: Int = 1) {
        hiddenImpl.hide(fromIndex, count)
        val hidden = list.localPos(fromIndex + count) - list.localPos(fromIndex)
        if (hidden > 0) {
            listNotifyList.add(
                NotifyEvent(
                    eventType = NotifyEvent.EventType.Remove,
                    val1 = list.localPos(fromIndex),
                    val2 = hidden
                )
            )
        }
        list.fill(false, fromIndex, fromIndex + count)
    }

    private fun showElements(fromIndex: Int, count: Int = 1) {
        hiddenImpl.show(fromIndex, count)
        var local = 0
        var hidden = 0
        for (i in 0 until fromIndex + count) {
            if (list[i]) {
                if (hidden > 0) {
                    listNotifyList.add(
                        NotifyEvent(
                            eventType = NotifyEvent.EventType.Insert,
                            val1 = local,
                            val2 = hidden
                        )
                    )
                    local += hidden
                    hidden = 0
                }
                local++
            } else if (i >= fromIndex) {
                hidden++
            }
        }
        if (hidden > 0) {
            listNotifyList.add(
                NotifyEvent(
                    eventType = NotifyEvent.EventType.Insert,
                    val1 = local,
                    val2 = hidden
                )
            )
        }
        list.fill(true, fromIndex, fromIndex + count)
    }

    private fun insertElements(fromIndex: Int, count: Int = 1, visible: Boolean = true) {
        hiddenImpl.insert(fromIndex, count, visible)
        if (visible) {
            listNotifyList.add(
                NotifyEvent(
                    eventType = NotifyEvent.EventType.Insert,
                    val1 = list.localPos(fromIndex),
                    val2 = count
                )
            )
        }
        repeat(count) { list.add(fromIndex, visible) }
    }

    private fun removeElements(fromIndex: Int, count: Int = 1) {
        hiddenImpl.remove(fromIndex, count)
        val hidden = list.localPos(fromIndex + count) - list.localPos(fromIndex)
        if (hidden > 0) {
            listNotifyList.add(
                NotifyEvent(
                    eventType = NotifyEvent.EventType.Remove,
                    val1 = list.localPos(fromIndex),
                    val2 = hidden,
                )
            )
        }
        repeat(count) { list.removeAt(fromIndex) }
    }

    private fun moveElements(
        fromIndex: Int, toIndex: Int = fromIndex
    ) {
        if (fromIndex == toIndex) {
            return
        }
        hiddenImpl.move(fromIndex, toIndex)
        if (list[fromIndex] && list.localPos(fromIndex) != list.localPos(toIndex)) {
            listNotifyList.add(
                NotifyEvent(
                    eventType = NotifyEvent.EventType.Move,
                    val1 = list.localPos(fromIndex),
                    val2 = list.localPos(toIndex),
                )
            )
        }
        list.add(toIndex, list.removeAt(fromIndex))
    }

    private fun notifyLog() = StringBuilder().apply {
        notifyList.forEach {
            append("${it.eventType.name}: ")
            appendLine(
                when (it.eventType) {
                    NotifyEvent.EventType.Insert -> "to=${it.val1}, cnt=${it.val2}"
                    NotifyEvent.EventType.Remove -> "from=${it.val1}, cnt=${it.val2}"
                    NotifyEvent.EventType.Move -> "from=${it.val1}, to=${it.val2}"
                }
            )
        }
    }.toString()

    @Before
    fun initList() {
        repeat(15) { list.add(true) }
    }

    //@After
    fun log() {
        println(testName.methodName)
        print(hiddenImpl.log(true))
        println(notifyLog())
    }

    //region hide
    @Test
    fun `hide in loop (0 to list-lastIndex) from 1 to list-size elements and return valid table`() {
        for (i in 0..14) {
            for (k in 1..15 - i) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, k=$k")
                hideElements(i, count = k)

                log()
                checkHidden()
            }
        }
    }
    //endregion

    //region show
    @Test
    fun `show in loop (0 to list-lastIndex) from 1 to list-size elements and return valid table`() {
        for (i in 0..14) {
            for (k in 1..15 - i) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, k=$k")
                showElements(i, count = k)

                log()
                checkHidden()
            }
        }
    }
    //endregion

    //region insert
    @Test
    fun `insert in loop (0 to list-lastIndex) from 1 to 2 show elements and return valid table`() {
        for (i in 0..14) {
            for (k in 1..2) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, k=$k")
                insertElements(i, count = k)

                log()
                checkHidden()
            }
        }
    }

    @Test
    fun `insert in loop (0 to list-lastIndex) from 1 to 2 hide elements and return valid table`() {
        for (i in 0..14) {
            for (k in 1..2) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, k=$k")
                insertElements(i, count = k, visible = false)

                log()
                checkHidden()
            }
        }
    }
    //endregion

    //region remove
    @Test
    fun `remove in loop (0 to list-lastIndex) from 1 to list-size elements and return valid table`() {
        for (i in 0..14) {
            for (k in 1..15 - i) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, k=$k")
                removeElements(i, count = k)

                log()
                checkHidden()
            }
        }
    }
    //endregion

    //region move
    @Test
    fun `move in loop (0 to list-lastIndex) 1 element to new position (list-size down to 0) and return valid table`() {
        for (i in 0..14) {
            for (k in 14 downTo 0) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)
                notifyList.clear()
                listNotifyList.clear()

                println("i=$i, to=$k")
                moveElements(i, k)

                log()
                checkHidden()
            }
        }
    }
    //endregion

}