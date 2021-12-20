package bk.app.testapp

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ListUpdateCallback

const val TAG = "testapplog"

class MainActivity : AppCompatActivity() {

    private var raw = ""
    val mask = "1##--##2"
    private val allowed = "0123456789"
    private val maskToRaw = intArrayOf(0, -1, -1, -1, -2, -3, -3, -3)
    private val rawToMask = intArrayOf(1, 2, 5, 6, 7)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "%2\$s".format("1", "2"))
//        Log.d(TAG, "${PhoneNumberUtils.formatNumber("1234567890", "+7", "RU")}")
//        Log.d(TAG, "${PhoneNumberUtils.normalizeNumber("8123-456-78-90")}")
//        Log.d(TAG, "${PhoneNumberUtils.formatNumber("+71234567890", "+7", "RU")}")
//        Log.d(TAG, "${PhoneNumberUtils.normalizeNumber("+7(1234)56-78-90")}")
        findViewById<EditText>(R.id.editTextPersonName).apply {
            var skip = false
            addTextChangedListener(
                onTextChanged = { s: CharSequence?, start, before, count ->
                    if (!skip) {
                        s?.substring(start, start + count)?.filter { allowed.contains(it) }
                            ?.let { raw = replace(raw, start, start + before, it) }
                    }
                },
                afterTextChanged = { s: Editable? ->
                    Log.d(TAG, "tw=${(s as SpannableStringBuilder).textWatcherDepth}")
                    if (!skip) {
                        val cursor = rawPos(Selection.getSelectionEnd(s))
                        skip = true
                        s?.filters = emptyArray()
                        s?.replace(0, s.length, masked(raw))
                        Selection.setSelection(s, 1, 2)
                        //rawToMask.getOrElse(cursor) { mask.lastIndex })
                        //                       text = SpannableStringBuilder(masked(raw))
                        postDelayed({ setSelection(1, 2) }, 100)
                        setText(s.toString())
                        Log.d(TAG, s.toString())
                        skip = false
                    }
                }
            )
        }

//        val a = "bk.app.testapp.MainActivity\$onCreate\$2"
//
//        (Class.forName(a).newInstance() as Int1).call("1", 1)
//
//        if (lifecycle.currentState == Lifecycle.State.CREATED) {
//            fun1(object : Int1 {
//                override fun call(arg1: String, arg2: Int) {
//                    Log.d(TAG, "$arg1, $arg2")
//                }
//            })
//        }
//
//        Log.d(TAG, "${-30 % 12}")
//            Log.d(TAG, Regex("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(.*)").find("11.12.13.14r")?.groupValues?.joinToString() ?: "null")
        for (start in 0..3) {
            for (stop in start..9) {
                val a = MutableList(12) { true }
                c.clear()
                checkEvents(hideF(a, 0, 2))
                checkEvents(hideF(a, 10, 11))
                checkEvents(hideF(a, 5, 6))
                Log.d(TAG, "---hide---- $start, $stop")
                printF()
                checkEvents(hideF(a, start, stop))
                printF()
                testF(a, c)
            }
        }
        for (start in 0..3) {
            for (stop in start..9) {
                val a = MutableList(12) { false }
                c.clear()
                c.hiddenNodes.add(
                    HidingItemsAdapter2.HiddenNode(
                        0, 0, 0, mutableListOf(
                            HidingItemsAdapter2.HiddenLeaf(0, 0, 12)
                        )
                    )
                )
                checkEvents(showF(a, 0, 2))
                checkEvents(showF(a, 10, 11))
                checkEvents(showF(a, 5, 6))
                Log.d(TAG, "---show---- $start, $stop")
                printF()
                checkEvents(showF(a, start, stop))
                printF()
                testF(a, c)
            }
        }
        for (start in 0..3) {
            for (stop in start..9) {
                val a = MutableList(12) { true }
                c.clear()
                checkEvents(hideF(a, 0, 2))
                checkEvents(hideF(a, 10, 11))
                checkEvents(hideF(a, 5, 6))
                Log.d(TAG, "---insert(hide)---- $start, $stop")
                printF()
                checkEvents(insertF(a, start, stop, true))
                printF()
                testF(a, c)
            }
        }
        for (start in 0..3) {
            for (stop in start..9) {
                val a = MutableList(12) { true }
                c.clear()
                checkEvents(hideF(a, 0, 2))
                checkEvents(hideF(a, 10, 11))
                checkEvents(hideF(a, 5, 6))
                Log.d(TAG, "---insert(show)---- $start, $stop")
                printF()
                checkEvents(insertF(a, start, stop, false))
                printF()
                testF(a, c)
            }
        }
        for (start in 0..3) {
            for (stop in start..11) {
                val a = MutableList(12) { true }
                c.clear()
                checkEvents(hideF(a, 0, 2))
                checkEvents(hideF(a, 10, 11))
                checkEvents(hideF(a, 5, 6))
                Log.d(TAG, "---remove---- $start, $stop")
                printF()
                checkEvents(removeF(a, start, stop))
                printF()
                testF(a, c)
            }
        }
//        for (start in 0..3) {
//            for (stop in start..9) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 10, 11))
//                checkEvents(hide(a, 5, 6))
//                Log.d(TAG, "---hide---- $start, $stop")
//                checkEvents(hide(a, start, stop))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..3) {
//            for (stop in start..9) {
//                val a = MutableList(12) { false }
//                c.clear()
//                c.hiddenItems.alloc {
//                    global = 0
//                    local = 0
//                    amount = 12
//                }
//                checkEvents(show(a, 0, 2))
//                checkEvents(show(a, 10, 11))
//                checkEvents(show(a, 5, 6))
//                Log.d(TAG, "---show---- $start, $stop")
//                checkEvents(show(a, start, stop))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..3) {
//            for (stop in start..9) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 10, 11))
//                checkEvents(hide(a, 5, 6))
//                Log.d(TAG, "---insert(hide)---- $start, $stop")
//                checkEvents(insert(a, start, stop, true))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..3) {
//            for (stop in start..9) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 10, 11))
//                checkEvents(hide(a, 5, 6))
//                Log.d(TAG, "---insert(show)---- $start, $stop")
//                checkEvents(insert(a, start, stop, false))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..3) {
//            for (stop in start..11) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 10, 11))
//                checkEvents(hide(a, 5, 6))
//                Log.d(TAG, "---remove---- $start, $stop")
//                checkEvents(remove(a, start, stop))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..10) {
//            for (stop in start + 1..11) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 3, 3))
//                checkEvents(hide(a, 6, 7))
//                checkEvents(hide(a, 9, 11))
//                Log.d(TAG, "---move---- $start, $stop")
//                checkEvents(move(a, start, stop))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..10) {
//            for (stop in start + 1..11) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 3, 3))
//                checkEvents(hide(a, 6, 7))
//                checkEvents(hide(a, 9, 11))
//                Log.d(TAG, "---move---- $stop, $start")
//                checkEvents(move(a, stop, start))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..5) {
//            for (stop in start..5) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 3, 3))
//                checkEvents(hide(a, 6, 7))
//                checkEvents(hide(a, 9, 11))
//                Log.d(TAG, "---batch hide 1---- $start, $stop, ${start + 6}, ${stop + 6}")
//                checkEvents(hideBatch(a, start, stop, start + 6, stop + 6))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..4) {
//            for (stop in start..4) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 4, 5))
//                checkEvents(hide(a, 8, 8))
//                Log.d(
//                    TAG,
//                    "---batch hide 2---- $start, $stop, ${stop + 2}, ${stop + 2 + stop - start}"
//                )
//                checkEvents(hideBatch(a, start, stop, stop + 2, stop + 2 + stop - start))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..5) {
//            for (stop in start..5) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 3, 3))
//                checkEvents(hide(a, 6, 7))
//                checkEvents(hide(a, 9, 11))
//                Log.d(TAG, "---batch show 1---- $start, $stop, ${start + 6}, ${stop + 6}")
//                checkEvents(showBatch(a, start, stop, start + 6, stop + 6))
//                printc()
//                test(a, c)
//            }
//        }
//        for (start in 0..4) {
//            for (stop in start..4) {
//                val a = MutableList(12) { true }
//                c.clear()
//                checkEvents(hide(a, 0, 2))
//                checkEvents(hide(a, 4, 5))
//                checkEvents(hide(a, 8, 8))
//                Log.d(
//                    TAG,
//                    "---batch show 2---- $start, $stop, ${stop + 2}, ${stop + 2 + stop - start}"
//                )
//                checkEvents(showBatch(a, start, stop, stop + 2, stop + 2 + stop - start))
//                printc()
//                test(a, c)
//            }
//        }
    }

    private var eventStr = ""

    val c = HidingItemsAdapter2(object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            eventStr += ", add $position $count"
            Log.d(TAG, "onInserted($position, $count)")
        }

        override fun onRemoved(position: Int, count: Int) {
            eventStr += ", remove $position $count"
            Log.d(TAG, "onRemoved($position, $count)")
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            eventStr += ", move $fromPosition $toPosition"
            Log.d(TAG, "onMoved($fromPosition, $toPosition)")
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            Log.d(TAG, "onChanged($position, $count)")
        }
    })

    fun checkEvents(s: String) {
        if (s != eventStr) {
            Log.d(TAG, "$s | $eventStr")
            printc()
            throw RuntimeException("bad notify")
        }
    }

    fun hide(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var i2 = -1
        var l = 0
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                if (a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                }
                a[i] = false
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            s += ", remove $i1 $n"
        }
        c.hide(start, stop - start + 1)
        return s
    }

    fun hideF(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var l = 0
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                if (a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                }
                a[i] = false
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            s += ", remove $i1 $n"
        }
        c.hideF(start, stop - start + 1)
        return s
    }

    fun show(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var i2 = -1
        var l = 0
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                if (!a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                } else {
                    if (i1 != -1) {
                        s += ", add $i1 $n"
                    }
                    i1 = -1
                    l += n
                    n = 0
                }
                a[i] = true
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            s += ", add $i1 $n"
        }
        c.show(start, stop - start + 1)
        return s
    }

    fun showF(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var l = 0
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                if (!a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                } else {
                    if (i1 != -1) {
                        s += ", add $i1 $n"
                    }
                    i1 = -1
                    l += n
                    n = 0
                }
                a[i] = true
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            s += ", add $i1 $n"
        }
        c.showF(start, stop - start + 1)
        return s
    }

    fun insert(a: MutableList<Boolean>, start: Int, stop: Int, hide: Boolean): String {
        eventStr = ""
        var l = 0
        var i1 = -1
        var i2 = -1
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                a.add(start, !hide)
                if (a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                }
            }
            if (f) {
                l++
            }
        }
        val s = if (i1 == -1) "" else ", add $i1 $n"
        c.insertItems(start, stop - start + 1, hide)
        c.dispatchLastEvent()
        return s
    }

    fun insertF(a: MutableList<Boolean>, start: Int, stop: Int, hide: Boolean): String {
        eventStr = ""
        var l = 0
        var i1 = -1
        var n = 0
        for (i in 0..stop) {
            val f = a[i]
            if (i >= start) {
                a.add(start, !hide)
                if (a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                }
            }
            if (f) {
                l++
            }
        }
        val s = if (i1 == -1) "" else ", add $i1 $n"
        c.internalInsert(start, stop - start + 1, hide)
        c.dispatchLastEvent()
        return s
    }

    fun remove(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var l = 0
        var i1 = -1
        var i2 = -1
        var n = 0
        var f: Boolean
        for (i in 0..stop) {
            if (i >= start) {
                f = a.removeAt(start)
                if (f) {
                    if (i1 == -1) i1 = l
                    n++
                }
            } else {
                f = a[i]
            }
            if (f) {
                l++
            }
        }
        val s = if (i1 == -1) "" else ", remove $i1 $n"
        c.removeItems(start, stop - start + 1)
        c.dispatchLastEvent()
        return s
    }

    fun removeF(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        var l = 0
        var i1 = -1
        var n = 0
        var f: Boolean
        for (i in 0..stop) {
            if (i >= start) {
                f = a.removeAt(start)
                if (f) {
                    if (i1 == -1) i1 = l
                    n++
                }
            } else {
                f = a[i]
            }
            if (f) {
                l++
            }
        }
        val s = if (i1 == -1) "" else ", remove $i1 $n"
        c.internalRemove(start, stop - start + 1)
        c.dispatchLastEvent()
        return s
    }

    fun move(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        val f = a[start]
        if (start < stop) {
            for (i in start until stop) {
                a[i] = a[i + 1]
            }
        } else {
            for (i in start downTo stop + 1) {
                a[i] = a[i - 1]
            }
        }
        a[stop] = f
        val s = ""
        c.moveItems(start, stop)
        c.dispatchLastEvent()
        return s
    }

    fun moveF(a: MutableList<Boolean>, start: Int, stop: Int): String {
        eventStr = ""
        val f = a[start]
        if (start < stop) {
            for (i in start until stop) {
                a[i] = a[i + 1]
            }
        } else {
            for (i in start downTo stop + 1) {
                a[i] = a[i - 1]
            }
        }
        a[stop] = f
        val s = ""
        c.internalMove(start, stop)
        c.dispatchLastEvent()
        return s
    }

    fun hideBatch(
        a: MutableList<Boolean>,
        start1: Int,
        stop1: Int,
        start2: Int,
        stop2: Int
    ): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var i2 = -1
        var l = 0
        var n = 0
        for (i in 0..stop1) {
            val f = a[i]
            if (i >= start1) {
                if (a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                }
                a[i] = false
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            s += ", remove $i1 $n"
        }
        i2 = -1
        var m = 0
        for (i in stop1 + 1..stop2) {
            val f = a[i]
            if (i >= start2) {
                if (a[i]) {
                    if (i2 == -1) i2 = l
                    m++
                }
                a[i] = false
            }
            if (f) {
                l++
            }
        }
        if (i2 != -1) {
            s = if (i1 + n == i2) {
                ", remove $i1 ${n + m}"
            } else {
                ", remove $i2 $m$s"
            }
        }
        c.batchHide(listOf(Pair(start1, stop1 - start1 + 1), Pair(start2, stop2 - start2 + 1)))
        return s
    }

    fun showBatch(
        a: MutableList<Boolean>,
        start1: Int,
        stop1: Int,
        start2: Int,
        stop2: Int
    ): String {
        eventStr = ""
        var s = ""
        var i1 = -1
        var l = 0
        var n = 0
        var l2 = -1
        var n2 = 0
        for (i in 0..stop1) {
            val f = a[i]
            if (i >= start1) {
                if (!a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                } else {
                    if (i1 != -1) {
                        if (l2 + n2 >= i1) {
                            n2 = (i1 + n) - l2
                        } else {
                            if (l2 != -1) {
                                s += ", add $l2 $n2"
                            }
                            l2 = i1
                            n2 = n
                        }
                    }
                    i1 = -1
                    l += n
                    n = 0
                }
                a[i] = true
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            if (l2 + n2 >= i1) {
                n2 = (i1 + n) - l2
            } else {
                if (l2 != -1) {
                    s += ", add $l2 $n2"
                }
                l2 = i1
                n2 = n
            }
            i1 = -1
            l += n
            n = 0
        }
        for (i in stop1 + 1..stop2) {
            val f = a[i]
            if (i >= start2) {
                if (!a[i]) {
                    if (i1 == -1) i1 = l
                    n++
                } else {
                    if (i1 != -1) {
                        if (l2 + n2 >= i1) {
                            n2 = (i1 + n) - l2
                        } else {
                            if (l2 != -1) {
                                s += ", add $l2 $n2"
                            }
                            l2 = i1
                            n2 = n
                        }
                    }
                    i1 = -1
                    l += n
                    n = 0
                }
                a[i] = true
            }
            if (f) {
                l++
            }
        }
        if (i1 != -1) {
            if (l2 + n2 >= i1) {
                n2 = (i1 + n) - l2
            } else {
                if (l2 != -1) {
                    s += ", add $l2 $n2"
                }
                l2 = i1
                n2 = n
            }
        }
        if (l2 != -1) {
            s += ", add $l2 $n2"
        }
        c.batchShow(listOf(Pair(start1, stop1 - start1 + 1), Pair(start2, stop2 - start2 + 1)))
        return s
    }

    fun test(a: MutableList<Boolean>, c: HidingItemsAdapter2) {
        for (i in 0 until c.hiddenItems.size) {
            val slot = c.hiddenItems[i]
            if (slot.global < 0 || slot.local > slot.global ||
                slot.global >= slot.local + slot.amount
            ) {
                throw RuntimeException("bad slot")
            }
            if (i > 0) {
                val prev = c.hiddenItems[i - 1]
                if (prev.global >= slot.global || prev.local >= slot.local ||
                    prev.local + prev.amount >= slot.global
                ) {
                    throw RuntimeException("bad slot")
                }
            }
        }
        var p = -1
        for (i in 0 until a.size) {
            val j = c.getShownPos(i)
            Log.d(TAG, "$i, ${a[i]}, $j")
            if (a[i] != (j != -1) || (j != p + 1 && j != -1)) {
                printc()
                throw RuntimeException("bad slot")
            }
            if (a[i]) p++
        }
        Log.d(TAG, "Ok")
    }

    fun testF(a: MutableList<Boolean>, c: HidingItemsAdapter2) {
        var prev: HidingItemsAdapter2.HiddenNode? = null
        for (node in c.hiddenNodes) {
            if (prev == null || prev.global < node.global && prev.local < node.local) {
                for (i in 0 until node.leaves.size) {
                    val leaf = node.leaves[i]
                    if (leaf.global < 0 || leaf.local > leaf.global ||
                        leaf.global >= leaf.local + leaf.amount
                    ) throw RuntimeException("bad leaf")
                    if (i > 0) {
                        val pleaf = node.leaves[i - 1]
                        if (pleaf.global >= leaf.global || pleaf.local >= leaf.local ||
                            pleaf.local + pleaf.amount >= leaf.global
                        ) throw RuntimeException("bad leaf")
                    }
                }
            } else throw RuntimeException("bad node")
            prev = node
        }

        var p = -1
        for (i in a.indices) {
            val j = c.shownPos(i)
            Log.d(TAG, "$i, ${a[i]}, $j")
            if (a[i] != (j != -1) || (j != -1 && j != p + 1)) {
                printF()
                throw RuntimeException("bad shownPos")
            }
            if (a[i]) p++
        }
        Log.d(TAG, "Ok")
    }

    fun printc() {
        for (j in 0 until c.hiddenItems.size) {
            Log.d(
                TAG,
                "${c.hiddenItems[j].global}, ${c.hiddenItems[j].local}, ${c.hiddenItems[j].amount}"
            )
        }
    }

    fun printF() {
        for (node in c.hiddenNodes) {
            Log.d(
                TAG,
                "node = ${node.global}, ${node.local}, ${node.amount}"
            )
            for (leaf in node.leaves) {
                Log.d(
                    TAG,
                    "   leaf = ${leaf.global}, ${leaf.local}, ${leaf.amount}"
                )
            }
        }
    }

    interface Int1 {
        fun call(arg1: String, arg2: Int)
    }

    fun fun1(act: Int1): String {
        Log.d(TAG, "$act, ${act::class.java.name}")
        return act::class.java.name
    }

    private fun replace(text: String, start: Int, stop: Int, substring: String) =
        text.replaceRange(
            rawPos(start).coerceAtMost(text.length),
            rawPos(stop).coerceAtMost(text.length),
            substring
        )

    private fun rawPos(pos: Int) = (maskToRaw.getOrElse(pos) { maskToRaw.last() } + pos)

    private fun masked(text: String) = StringBuilder().apply {
        mask.forEachIndexed { i, c ->
            append(if (c == '#') text.getOrElse(rawPos(i)) { '_' } else c)
        }
    }.toString()

}