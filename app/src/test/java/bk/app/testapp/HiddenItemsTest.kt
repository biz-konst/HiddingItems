package bk.app.testapp

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.rules.TestName

import org.junit.Rule

class HiddenItemsTest {

    @get:Rule
    var testName = TestName()

    private val hiddenImpl = HidingItemsImpl()
    private val list = arrayListOf<Boolean>()

    private fun <T> ArrayList<T>.fill(value: T, fromIndex: Int, toIndex: Int) {
        for (i in fromIndex until toIndex) set(i, value)
    }

    private fun HidingItemsImpl.Bucket.log(): String = StringBuilder()
        .apply {
            if (entries.isEmpty()) {
                appendLine("    entries empty")
            } else {
                entries.forEachIndexed { i, entry ->
                    appendLine("    entry $i: index=${entry.index} pos=${entry.position} hidden=${entry.hidden}")
                }
            }
        }
        .toString()

    private fun HidingItemsImpl.log(full: Boolean = false): String = StringBuilder()
        .apply {
            if (table.isEmpty()) {
                appendLine("table empty\n")
            } else {
                table.forEachIndexed { i, bucket ->
                    appendLine("bucket $i: index=${bucket.index} pos=${bucket.position} len=${bucket.entries.size}")
                    if (full) {
                        appendLine("${bucket.log()}")
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
    }

    private fun hideElements(fromIndex: Int, toIndex: Int = fromIndex, count: Int = 0) {
        if (count > 0) {
            hiddenImpl.hide(fromIndex, count)
            list.fill(false, fromIndex, fromIndex + count)
        } else {
            hiddenImpl.hide(fromIndex, toIndex - fromIndex + 1)
            list.fill(false, fromIndex, toIndex + 1)
        }
    }

    private fun showElements(fromIndex: Int, toIndex: Int = fromIndex, count: Int = 0) {
        if (count > 0) {
            hiddenImpl.show(fromIndex, count)
            list.fill(true, fromIndex, fromIndex + count)
        } else {
            hiddenImpl.show(fromIndex, toIndex - fromIndex + 1)
            list.fill(true, fromIndex, toIndex + 1)
        }
    }

    private fun insertElements(
        fromIndex: Int, toIndex: Int = fromIndex, count: Int = 0, visible: Boolean = true
    ) {
        if (count > 0) {
            hiddenImpl.insert(fromIndex, count, visible)
            repeat(count) { list.add(fromIndex, visible) }
        } else {
            insertElements(fromIndex, count = toIndex - fromIndex + 1, visible = visible)
        }
    }

    private fun removeElements(
        fromIndex: Int, toIndex: Int = fromIndex, count: Int = 0
    ) {
        if (count > 0) {
            hiddenImpl.remove(fromIndex, count)
            repeat(count) { list.removeAt(fromIndex) }
        } else {
            removeElements(fromIndex, count = toIndex - fromIndex + 1)
        }
    }

    private fun moveElements(
        fromIndex: Int, toIndex: Int = fromIndex
    ) {
        if (fromIndex == toIndex) {
            return
        }
        hiddenImpl.move(fromIndex, toIndex)
        list.add(toIndex, list.removeAt(fromIndex))
    }

    @Before
    fun initList() {
        repeat(15) { list.add(true) }
    }

    //@After
    fun log() {
        println(testName.methodName)
        print(hiddenImpl.log(true))
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
        for (i in 1..14) {
            for (k in 3 downTo 0) {
                list.clear()
                repeat(15) { list.add(true) }
                hiddenImpl.table.clear()
                hideElements(1)
                hideElements(4, count = 2)
                hideElements(9, count = 3)

                println("i=$i, to=$k")
                moveElements(i, k)

                log()
                checkHidden()
            }
        }
    }
    //endregion

}