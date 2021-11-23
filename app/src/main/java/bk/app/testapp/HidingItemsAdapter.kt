package bk.app.testapp

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import java.util.*
import kotlin.NoSuchElementException

interface HidingItems {
    fun hide(position: Int, count: Int)
    fun isHidden(index: Int): Boolean
    fun show(position: Int, count: Int)
}

// TODO ("replace internal with private")
class HidingItemsAdapter(private val callback: ListUpdateCallback?) : HidingItems,
    ListUpdateCallback {

    private val nodeSize = 2//Int.MAX_VALUE

    internal val hiddenNodes = mutableListOf<HiddenNode>()

    internal open class HiddenLeaf(
        var global: Int,
        var local: Int,
        var amount: Int,
    )

    internal class HiddenNode(
        var global: Int,
        var local: Int,
        var amount: Int,
        val leaves: MutableList<HiddenLeaf>
    )

    fun shownPos(position: Int): Int {
        val node = getNode { it.global - position } ?: return position
        val leaf = node.getLeaf { it.global - position } ?: return position

        val sp = position - leaf.amount
        return if (sp < leaf.local) -1 else sp - node.amount
    }

    fun rawPos(position: Int): Int {
        val node = getNode { it.local - position } ?: return position
        val leaf = node.getLeaf { it.local - position } ?: return position

        return position + leaf.amount + node.amount
    }

    fun hideF(position: Int, count: Int) {
        check(position >= 0)
        check(count > 0)
        internalHide(position, count)
        dispatchLastEvent()
    }

    fun showF(position: Int, count: Int) {
        check(position >= 0)
        check(count > 0)
        internalShow(position, count)
        dispatchLastEvent()
    }

    private fun findNode(
        fromIndex: Int = 0,
        toIndex: Int = hiddenNodes.lastIndex,
        comparison: (HiddenNode) -> Int
    ): Int {
        var low = fromIndex
        var high = toIndex

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val cmp = comparison(hiddenNodes[mid])

            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }

        return high
    }

    private fun getNode(comparison: (HiddenNode) -> Int) =
        findNode(comparison = comparison).let {
            if (it < 0) null else hiddenNodes[it]
        }

    private fun HiddenNode.findLeaf(comparison: (HiddenLeaf) -> Int) =
        leaves.binarySearch(comparison = comparison)

    private fun HiddenNode.getLeaf(comparison: (HiddenLeaf) -> Int) =
        findLeaf(comparison = comparison).let {
            if (it == -1) null else leaves[if (it < 0) -it - 2 else it]
        }

    private fun addNode(position: Int): HiddenNode {
        return HiddenNode(position, position, 0, mutableListOf())
            .also { hiddenNodes.add(0, it) }
    }

    private fun correctLeaves(li: Int, ni: Int, amount: Int) {
        val leaves = hiddenNodes[ni].leaves
        var i = li
        while (i < leaves.size) {
            leaves[i++].let {
                it.local -= amount
                it.amount += amount
            }
        }
        i = adjustNodeSize(ni)
        while (++i < hiddenNodes.size) {
            hiddenNodes[i].amount += amount
        }
    }

    private fun getNodeSize(): Int = nodeSize

    private fun adjustNodeSize(index: Int): Int {
        var result = index
        val node = hiddenNodes[result]
        if (node.leaves.size == 0) {
            hiddenNodes.removeAt(result--)
        } else {
            node.leaves[0].let {
                node.global = it.global
                node.local = it.local
            }
            if (node.leaves.size > getNodeSize()) {
                val mid = node.leaves.size.ushr(1)
                val leaf = node.leaves.removeAt(mid)
                val new = HiddenNode(leaf.global, leaf.local, node.amount, mutableListOf(leaf))
                while (node.leaves.size > mid) {
                    new.leaves.add(node.leaves.removeAt(mid))
                }
                hiddenNodes.add(++result, new)
            }
        }
        return result
    }

    private fun internalHide(position: Int, count: Int) {
        notifyRemoved(position, count)
        var ni = findNode { it.global - position }
        var node = if (ni < 0) {
            ni = 0
            addNode(position)
        } else {
            hiddenNodes[ni]
        }
        var li = node.findLeaf { it.global - position }
        var leaves = node.leaves
        val endPos = position + count
        var amount = count
        var leaf: HiddenLeaf
        if (li < 0) {
            li = -li - 1
            if (li > 0) {
                leaf = leaves[li - 1]
                val topPos = leaf.local + leaf.amount
                if (position <= topPos) {
                    amount = endPos - topPos
                    if (amount <= 0) {
                        return
                    }
                    leaf.amount += amount
                } else {
                    leaf = HiddenLeaf(position, position - leaf.amount, amount + leaf.amount)
                    leaves.add(li++, leaf)
                }
            } else {
                leaf = HiddenLeaf(position, position, amount)
                leaves.add(li++, leaf)
            }
        } else {
            leaf = leaves[li++]
            val topPos = leaf.local + leaf.amount
            amount = endPos - topPos
            if (amount <= 0) {
                return
            }
            leaf.amount += amount
        }
        do {
            while (li < leaves.size) {
                val next = leaves[li]
                if (next.global > endPos) {
                    correctLeaves(li, ni, amount)
                    return
                }
                val topPos = next.local + next.amount
                if (topPos > endPos) {
                    leaf.amount += topPos - endPos
                }
                amount = leaf.amount - next.amount
                leaves.removeAt(li)
            }
            ni = adjustNodeSize(ni)
            if (++ni >= hiddenNodes.size) {
                break
            }
            node = hiddenNodes[ni]
            li = 0
            leaves = node.leaves
        } while (true)
    }

    private fun internalShow(position: Int, count: Int) {
        if (hiddenNodes.size == 0) {
            return
        }
        var ni = findNode { it.global - position }
        var node = if (ni < 0) {
            ni = 0
            addNode(position)
        } else {
            hiddenNodes[ni]
        }
        var li = node.findLeaf { it.global - position }
        var leaves = node.leaves
        val endPos = position + count
        var amount = 0
        if (li < 0) {
            li = -li - 1
            if (li > 0) {
                var leaf = leaves[li - 1]
                val topPos = leaf.local + leaf.amount
                if (position < topPos) {
                    amount = topPos - position
                    leaf.amount -= amount
                    if (topPos > endPos) {
                        amount = endPos - position
                        leaf =
                            HiddenLeaf(endPos, endPos - leaf.amount, topPos - endPos + leaf.amount)
                        leaves.add(li++, leaf)
                    }
                    notifyInserted(position, amount)
                }
            }
        } else {
            val leaf = leaves[li]
            val topPos = leaf.local + leaf.amount
            if (topPos > endPos) {
                amount = endPos - position
                leaf.global = endPos
                leaf.local += amount
                leaf.amount -= amount
                li++
            } else {
                amount = topPos - position
                leaves.removeAt(li)
            }
            notifyInserted(position, amount)
        }
        do {
            while (li < leaves.size) {
                val leaf = leaves[li]
                if (leaf.global > endPos) {
                    correctLeaves(li, ni, -amount)
                    return
                }
                val savePos = leaf.global
                val topPos = leaf.local + leaf.amount
                var shown: Int
                if (topPos > endPos) {
                    shown = endPos - leaf.global
                    amount += shown
                    leaf.global = endPos
                    leaf.local += amount
                    leaf.amount -= amount
                    li++
                } else {
                    shown = topPos - leaf.global
                    amount += shown
                    leaves.removeAt(li)
                }
                notifyInserted(savePos, shown)
            }
            ni = adjustNodeSize(ni)
            if (++ni >= hiddenNodes.size) {
                break
            }
            node = hiddenNodes[ni]
            li = 0
            leaves = node.leaves
        } while (true)
    }

    internal fun internalInsert(position: Int, count: Int, hide: Boolean = false) {
        if (hiddenNodes.size == 0 && !hide) {
            notifyInserted(position, count)
            return
        }
        var ni = findNode { it.global - position }
        var node = if (ni < 0) {
            ni = 0
            addNode(position)
        } else {
            hiddenNodes[ni]
        }
        var li = node.findLeaf { it.global - position }
        var leaves = node.leaves
        val endPos = position + count
        if (hide) {
            if (li < 0) {
                li = -li - 1
                if (li > 0) {
                    var leaf = leaves[li - 1]
                    if (leaf.local + leaf.amount < position) {
                        leaf = HiddenLeaf(position, position - leaf.amount, leaf.amount + count)
                        leaves.add(li++, leaf)
                    } else {
                        leaf.amount += count
                    }
                } else {
                    leaves.add(li++, HiddenLeaf(position, position, count))
                }
                ni = adjustNodeSize(ni)
            } else {
                leaves[li++].amount += count
            }
            do {
                while (li < leaves.size) {
                    leaves[li++].let {
                        it.global += count
                        it.amount += count
                    }
                }
                if (++ni >= hiddenNodes.size) {
                    break
                }
                node = hiddenNodes[ni]
                li = 0
                leaves = node.leaves
            } while (true)
        } else {
            if (li < 0) {
                li = -li - 1
                if (li > 0) {
                    var leaf = leaves[li - 1]
                    val left = leaf.local + leaf.amount - position
                    if (left > 0) {
                        leaf.amount -= left
                        leaf = HiddenLeaf(endPos, endPos - leaf.amount, leaf.amount + left)
                        leaves.add(li++, leaf)
                        ni = adjustNodeSize(ni)
                    }
                }
            }
            do {
                while (li < leaves.size) {
                    leaves[li++].let {
                        it.global += count
                        it.local += count
                    }
                }
                if (++ni >= hiddenNodes.size) {
                    break
                }
                node = hiddenNodes[ni]
                li = 0
                leaves = node.leaves
            } while (true)
            notifyInserted(position, count)
        }
    }

    internal fun internalRemove(position: Int, count: Int) {
        notifyRemoved(position, count)
        if (hiddenNodes.size == 0) {
            return
        }
        var ni = findNode { it.global - position }
        var node = if (ni < 0) {
            ni = 0
            addNode(position)
        } else {
            hiddenNodes[ni]
        }
        var li = node.findLeaf { it.global - position }
        var leaves = node.leaves
        val endPos = position + count
        var amount = 0
        var leaf: HiddenLeaf? = null
        if (li < 0) {
            li = -li - 1
            if (li > 0) {
                leaf = leaves[li - 1]
                val topPos = leaf.local + leaf.amount
                if (topPos >= position) {
                    amount = if (topPos < endPos) topPos - position else count
                    leaf.amount -= amount
                } else {
                    leaf = null
                }
            }
        }
        do {
            while (li < leaves.size) {
                val next = leaves[li]
                if (next.global > endPos) {
                    do {
                        leaves[li++].let {
                            it.global -= count
                            it.local -= count
                            it.local += amount
                            it.amount -= amount
                        }
                    } while (li < leaves.size)
                    break
                }
                val topPos = next.local + next.amount
                if (topPos > endPos) {
                    amount += endPos - next.global
                    if (leaf != null) {
                        leaf.amount += topPos - endPos
                        leaves.removeAt(li)
                    } else {
                        next.global = position
                        next.local -= count - amount
                        next.amount -= amount
                        li++
                    }
                } else {
                    amount += topPos - next.global
                    leaves.removeAt(li)
                }
            }
            ni = adjustNodeSize(ni)
            if (++ni >= hiddenNodes.size) {
                break
            }
            node = hiddenNodes[ni]
            li = 0
            leaves = node.leaves
        } while (true)
    }

    internal fun internalMove(fromPosition: Int, toPosition: Int) {
        if (hiddenNodes.size == 0 || fromPosition == toPosition) {
            return
        }
        // ищем fromIndex + 1 чтобы сразу учесть краевой случай,
        // когда удаляется строка, предшествующая скрытой группе
        var ni = findNode { it.global - fromPosition - 1 }
        var node = if (ni < 0) {
            ni = 0
            addNode(fromPosition + 1)
        } else {
            hiddenNodes[ni]
        }
        var li = node.findLeaf { it.global - fromPosition - 1 }
        var leaves = node.leaves
        var amount = 0
        if (li < 0) {
            li = -li - 1
            if (li > 0) {
                val leaf = leaves[li - 1]
                val topPos = leaf.local + leaf.amount
                if (topPos > fromPosition) {
                    amount++
                    if (topPos - 1 == leaf.global) {
                        leaves.removeAt(li)
                        adjustNodeSize(ni)
                    } else {
                        leaf.amount--
                    }
                }
            }
        } else if (li > 0) {
            val leaf = leaves[li - 1]
            if (leaf.local + leaf.amount == fromPosition) {
                leaf.amount = leaves[li].amount
                leaves.removeAt(li)
                adjustNodeSize(ni)
            }
        }
        val local = 1 - amount
        if (fromPosition < toPosition) {
            do {
                while (li < leaves.size) {
                    val leaf = leaves[li]
                    if (leaf.global > toPosition) {
                        if (leaf.global == toPosition + 1) {
                            leaf.global -= amount
                            return
                        }
                        break
                    }
                    leaf.global--
                    leaf.local -= local
                    leaf.amount -= amount
                    li++
                }
                if (++ni >= hiddenNodes.size) {
                    break
                }
                node = hiddenNodes[ni]
                li = 0
                leaves = node.leaves
            } while (true)
            if (li != 0) {
                li--
            }
        } else {
            do {
                while (li > 0) {
                    val leaf = leaves[--li]
                    if (leaf.global <= toPosition) {
                        if (leaf.global == toPosition) {
                            leaf.global += local
                            leaf.local += local
                            leaf.amount += amount
                            return
                        }
                        break
                    }
                    leaf.global++
                    leaf.local += local
                    leaf.amount += amount
                }
                if (++ni >= hiddenNodes.size) {
                    break
                }
                node = hiddenNodes[ni]
                li = 0
                leaves = node.leaves
            } while (true)
        }
        val leaf = leaves[li]
        val topPos = leaf.local + leaf.amount
        when {
            // случай, когда toIndex < fromIndex и < первой скрытой группы
            // или переносим первую скрытую группу размером 1 (удалили выше)
            toPosition < leaf.global -> {
                if (amount > 0) {
                    leaves.add(0, HiddenLeaf(toPosition, toPosition, 1))
                    adjustNodeSize(ni)
                }
            }
            toPosition > topPos -> {
                if (amount > 0) {
                    leaves.add(
                        li + 1,
                        HiddenLeaf(toPosition, toPosition - leaf.amount, leaf.amount + 1)
                    )
                    adjustNodeSize(ni)
                }
            }
            toPosition == topPos -> {
                if (amount > 0) {
                    leaf.amount++
                }
            }
            else -> {
                if (amount > 0) {
                    leaf.amount++
                } else {
                    leaves.add(
                        li + 1,
                        HiddenLeaf(toPosition + 1, leaf.local + 1, leaf.amount)
                    )
                    leaf.amount = toPosition - leaf.local
                    adjustNodeSize(ni)
                }
            }
        }
    }


    internal val hiddenItems = RecyclerList { HiddenMeta(0, 0, 0) }

    private var lastEventType = EventType.None
    private var lastEventPos = 0
    private var lastEventCount = 0
    private val removeEvents = RecyclerList { RemoveEvent(0, 0, 0) }

    override fun hide(position: Int, count: Int) {
        check(position >= 0)
        check(count > 0)
        hideItems(position, count, Int.MAX_VALUE, arrayOf(0, indexOfMeta(position)))
        { pos, cnt -> doRemovedEvent(requireShownPos(pos), cnt) }
        dispatchLastEvent()
    }

    override fun isHidden(index: Int) = getShownPos(index) == -1

    override fun show(position: Int, count: Int) {
        check(position >= 0)
        check(count > 0)
        if (hiddenItems.size == 0) {
            return
        }
        showItems(position, count, Int.MAX_VALUE, arrayOf(0, indexOfMeta(position)))
        dispatchLastEvent()
    }

    override fun onInserted(position: Int, count: Int) {
        insertItems(position, count)
        dispatchLastEvent()
    }

    override fun onRemoved(position: Int, count: Int) {
        removeItems(position, count)
        dispatchLastEvent()
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        moveItems(fromPosition, toPosition)
        callback?.let {
            val shownFrom = requireShownPos(fromPosition)
            val shownTo = requireShownPos(toPosition)
            if (shownFrom != shownTo) it.onMoved(shownFrom, shownTo)
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        callback?.let {
            val shownPos = requireShownPos(position)
            val shownCnt = requireShownPos(position + count) - shownPos
            if (shownCnt > 0) it.onChanged(shownPos, shownCnt, payload)
        }
    }

    fun getShownPos(position: Int): Int {
        val meta = metaByRawPos(position) ?: return position
        val result = position - meta.amount
        return if (result < meta.local) -1 else result
    }

    fun getRawPos(position: Int): Int {
        val meta = metaByShownPos(position) ?: return position
        return position + meta.amount
    }

    fun batchHide(list: List<Pair<Int, Int>>) {
        check(list.isNotEmpty())
        removeEvents.clear()
        var i = 0
        var position = list[i].first
        val params = arrayOf(0, indexOfMeta(position))
        do {
            val count = list[i++].second
            val nextPos = if (i < list.size) list[i].first else Int.MAX_VALUE
            check(nextPos > position + count)
            hideItems(position, count, nextPos, params) { pos, cnt ->
                removeEvents.alloc {
                    this.position = pos
                    this.count = cnt
                    this.amount = params[0]
                }
            }
            position = nextPos
        } while (position < Int.MAX_VALUE)
        for (j in removeEvents.lastIndex downTo 0) removeEvents[j].let {
            doRemovedEvent(requireShownPos(it.position) + it.amount, it.count)
        }
        dispatchLastEvent()
    }

    fun batchShow(list: List<Pair<Int, Int>>) {
        check(list.isNotEmpty())
        var i = 0
        var position = list[i].first
        val params = arrayOf(0, indexOfMeta(position))
        do {
            val count = list[i++].second
            val nextPos = if (i < list.size) list[i].first else Int.MAX_VALUE
            check(nextPos > position + count)
            showItems(position, count, nextPos, params)
            position = nextPos
        } while (position < Int.MAX_VALUE)
        dispatchLastEvent()
    }

    fun clear() {
        hiddenNodes.clear()
        hiddenItems.clear()
    }

    private fun hideItems(
        position: Int,
        count: Int,
        stopPos: Int,
        params: Array<Int>,
        eventCallback: (Int, Int) -> Unit
    ) {
        val endPos = position + count
        val amount = params[0]
        var i = params[1]
        var hidden = count
        var meta: HiddenMeta
        var topPos: Int
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                meta = hiddenItems[i - 1]
                topPos = meta.local + meta.amount
                if (position <= topPos) {
                    hidden = endPos - topPos
                    if (hidden <= 0) {
                        hidden = 0
                    } else {
                        meta.amount += hidden
                    }
                } else {
                    meta = allocMeta(
                        i++,
                        position,
                        position - meta.amount,
                        hidden + meta.amount
                    )
                }
            } else {
                meta = allocMeta(i++, position, position, hidden)
            }
        } else {
            meta = hiddenItems[i++]
            topPos = meta.local + meta.amount
            hidden = endPos - topPos
            if (hidden <= 0) {
                hidden = 0
            } else {
                meta.amount += hidden
            }
        }
        hidden += amount
        var next: HiddenMeta
        while (i < hiddenItems.size) {
            next = hiddenItems[i]
            if (next.global > endPos) {
                do {
                    next = hiddenItems[i]
                    if (next.global >= stopPos) {
                        if (next.global == stopPos) {
                            next.local -= hidden
                            next.amount += hidden
                            i = -i - 1
                        }
                        break
                    }
                    next.local -= hidden
                    next.amount += hidden
                    i++
                } while (i < hiddenItems.size)
                break
            }
            topPos = next.local + next.amount
            if (topPos > endPos) {
                meta.amount += topPos - endPos
            }
            hidden = meta.amount - next.amount
            releaseMeta(i)
        }
        eventCallback(position, hidden - params[0])
        params[0] = hidden
        params[1] = -i - 1
    }

    private fun showItems(position: Int, count: Int, nextPos: Int, params: Array<Int>) {
        val endPos = position + count
        var amount = params[0]
        var i = params[1]
        var shown = 0
        val meta: HiddenMeta
        var topPos: Int
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                val prev = hiddenItems[i - 1]
                topPos = prev.local + prev.amount
                if (position < topPos) {
                    shown = topPos - position
                    prev.amount -= shown
                    if (topPos > endPos) {
                        shown = endPos - position
                        allocMeta(
                            i++,
                            endPos,
                            endPos - prev.amount,
                            topPos - endPos + prev.amount
                        )
                    }
                    notifyInserted(position, shown)
                }
            }
        } else {
            meta = hiddenItems[i]
            topPos = meta.local + meta.amount
            if (topPos > endPos) {
                shown = endPos - position
                meta.global = endPos
                meta.local += shown
                meta.amount -= shown
                i++
            } else {
                shown = topPos - position
                releaseMeta(i)
            }
            notifyInserted(position, shown)
        }
        var next: HiddenMeta
        var savePos: Int
        amount += shown
        while (i < hiddenItems.size) {
            next = hiddenItems[i]
            if (next.global >= endPos) {
                do {
                    next = hiddenItems[i]
                    if (next.global >= nextPos) {
                        if (next.global == nextPos) {
                            next.local += amount
                            next.amount -= amount
                            i = -i - 1
                        }
                        break
                    }
                    next.local += amount
                    next.amount -= amount
                    i++
                } while (i < hiddenItems.size)
                break
            }
            savePos = next.global
            topPos = next.local + next.amount
            if (topPos > endPos) {
                shown = endPos - next.global
                amount += shown
                next.global = endPos
                next.local += amount
                next.amount -= amount
                i++
            } else {
                shown = topPos - next.global
                amount += shown
                releaseMeta(i)
            }
            notifyInserted(savePos, shown)
        }
        params[0] = amount
        params[1] = -i - 1
    }

    internal fun insertItems(position: Int, count: Int, hide: Boolean = false) {
        check(position >= 0)
        check(count > 0)
        if (hiddenItems.size == 0 && !hide) {
            notifyInserted(position, count)
            return
        }
        val endPos = position + count
        var i = indexOfMeta(position)
        var meta: HiddenMeta
        if (hide) {
            if (i < 0) {
                i = -i - 1
                if (i > 0) {
                    meta = hiddenItems[i - 1]
                    if (meta.local + meta.amount < position) {
                        allocMeta(
                            i++,
                            position,
                            position - meta.amount,
                            meta.amount + count
                        )
                    } else {
                        meta.amount += count
                    }
                } else {
                    allocMeta(i++, position, position, count)
                }
            } else {
                meta = hiddenItems[i++]
                meta.amount += count
            }
            while (i < hiddenItems.size) {
                meta = hiddenItems[i++]
                meta.global += count
                meta.amount += count
            }
        } else {
            if (i < 0) {
                i = -i - 1
                if (i > 0) {
                    meta = hiddenItems[i - 1]
                    val left = meta.local + meta.amount - position
                    if (left > 0) {
                        meta.amount -= left
                        allocMeta(
                            i++,
                            endPos,
                            endPos - meta.amount,
                            meta.amount + left
                        )
                    }
                }
            }
            while (i < hiddenItems.size) {
                meta = hiddenItems[i++]
                meta.global += count
                meta.local += count
            }
            notifyInserted(position, count)
        }
    }

    internal fun removeItems(position: Int, count: Int) {
        check(position >= 0)
        check(count > 0)
        if (hiddenItems.size == 0) {
            notifyRemoved(position, count)
            return
        }
        val endPos = position + count
        var i = indexOfMeta(position)
        var meta: HiddenMeta? = null
        var hidden = 0
        var topPos: Int
        notifyRemoved(position, count)
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                meta = hiddenItems[i - 1]
                topPos = meta.local + meta.amount
                if (topPos >= position) {
                    hidden = if (topPos < endPos) topPos - position else count
                    meta.amount -= hidden
                } else {
                    meta = null
                }
            }
        }
        var next: HiddenMeta
        while (i < hiddenItems.size) {
            next = hiddenItems[i]
            if (next.global > endPos) {
                do {
                    next = hiddenItems[i]
                    next.global -= count
                    next.local -= count
                    next.local += hidden
                    next.amount -= hidden
                    i++
                } while (i < hiddenItems.size)
                break
            }
            topPos = next.local + next.amount
            if (topPos > endPos) {
                hidden += endPos - next.global
                if (meta != null) {
                    meta.amount += topPos - endPos
                    releaseMeta(i)
                } else {
                    next.global = position
                    next.local -= count - hidden
                    next.amount -= hidden
                    i++
                }
            } else {
                hidden += topPos - next.global
                releaseMeta(i)
            }
        }
    }

    internal fun moveItems(fromPosition: Int, toPosition: Int) {
        check(fromPosition >= 0)
        check(toPosition >= 0)
        if (hiddenItems.size == 0 || fromPosition == toPosition) {
            return
        }
        // ищем fromIndex + 1 чтобы сразу учесть краевой случай,
        // когда удаляется строка, предшествующая скрытой группе
        var i = indexOfMeta(fromPosition + 1)
        var meta: HiddenMeta
        var hidden = 0
        var topPos: Int
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                meta = hiddenItems[i - 1]
                topPos = meta.local + meta.amount
                if (topPos > fromPosition) {
                    hidden++
                    if (topPos - 1 == meta.global) {
                        releaseMeta(--i)
                    } else {
                        meta.amount--
                    }
                }
            }
        } else if (i > 0) {
            meta = hiddenItems[i - 1]
            if (meta.local + meta.amount == fromPosition) {
                meta.amount = hiddenItems[i].amount
                releaseMeta(i)
            }
        }
        val local = 1 - hidden
        if (fromPosition < toPosition) {
            while (i < hiddenItems.size) {
                meta = hiddenItems[i]
                if (meta.global > toPosition) {
                    if (meta.global == toPosition + 1) {
                        meta.global -= hidden
                        return
                    }
                    break
                }
                meta.global--
                meta.local -= local
                meta.amount -= hidden
                i++
            }
            if (i != 0) {
                i--
            }
        } else {
            while (i > 0) {
                meta = hiddenItems[--i]
                if (meta.global <= toPosition) {
                    if (meta.global == toPosition) {
                        meta.global += local
                        meta.local += local
                        meta.amount += hidden
                        return
                    }
                    break
                }
                meta.global++
                meta.local += local
                meta.amount += hidden
            }
        }
        meta = hiddenItems[i]
        topPos = meta.local + meta.amount
        when {
            // случай, когда toIndex < fromIndex и < первой скрытой группы
            // или переносим первую скрытую группу размером 1 (удалили выше)
            toPosition < meta.global -> {
                if (hidden > 0) {
                    allocMeta(0, toPosition, toPosition, 1)
                }
            }
            toPosition > topPos -> {
                if (hidden > 0) {
                    allocMeta(i + 1, toPosition, toPosition - meta.amount, meta.amount + 1)
                }
            }
            toPosition == topPos -> {
                if (hidden > 0) {
                    meta.amount++
                }
            }
            else -> {
                if (hidden > 0) {
                    meta.amount++
                } else {
                    allocMeta(i + 1, toPosition + 1, meta.local + 1, meta.amount)
                    meta.amount = toPosition - meta.local
                }
            }
        }
    }


    private fun metaByRawPos(position: Int): HiddenMeta? {
        val i = hiddenItems.binarySearch { it.global - position }
        return if (i == -1) null else hiddenItems[if (i < 0) -i - 2 else i]
    }

    private fun metaByShownPos(position: Int): HiddenMeta? {
        val i = hiddenItems.binarySearch { it.local - position }
        return if (i == -1) null else hiddenItems[if (i < 0) -i - 2 else i]
    }

    private fun indexOfMeta(position: Int, fromIndex: Int = 0) =
        hiddenItems.binarySearch(fromIndex = fromIndex) { it.global - position }

    private fun allocMeta(index: Int, global: Int, local: Int, amount: Int) =
        hiddenItems.alloc(index) {
            this.global = global
            this.local = local
            this.amount = amount
        }

    private fun releaseMeta(index: Int) {
        hiddenItems.release(index)
    }

    private fun requireShownPos(position: Int): Int {
        val node = getNode { it.global - position } ?: return position
        val leaf = node.getLeaf { it.global - position } ?: return position

        return (position - leaf.amount).coerceAtLeast(leaf.local) - node.amount
//        val meta = metaByRawPos(position) ?: return position
//        return (position - meta.amount).coerceAtLeast(meta.local)
    }

    private fun notifyInserted(position: Int, count: Int) {
        val shownPos = requireShownPos(position)
        val shownCnt = requireShownPos(position + count) - shownPos
        if (shownCnt == 0) {
            return
        }
        if (lastEventType == EventType.Add &&
            lastEventPos <= shownPos && lastEventPos + lastEventCount >= shownPos
        ) {
            lastEventCount = shownPos + shownCnt - lastEventPos
            return
        }
        dispatchLastEvent()
        lastEventPos = shownPos
        lastEventCount = shownCnt
        lastEventType = EventType.Add
    }

    private fun notifyRemoved(position: Int, count: Int) {
        val shownPos = requireShownPos(position)
        val shownCnt = requireShownPos(position + count) - shownPos
        doRemovedEvent(shownPos, shownCnt)
    }

    private fun doRemovedEvent(shownPos: Int, shownCnt: Int) {
        if (shownCnt == 0) {
            return
        }
        val shownEnd = shownPos + shownCnt
        if (lastEventType == EventType.Remove &&
            shownPos <= lastEventPos && lastEventPos <= shownEnd
        ) {
            lastEventCount = (lastEventPos + lastEventCount).coerceAtLeast(shownEnd) - shownPos
            lastEventPos = shownPos
            return
        }
        dispatchLastEvent()
        lastEventPos = shownPos
        lastEventCount = shownCnt
        lastEventType = EventType.Remove
    }

    internal fun dispatchLastEvent() {
        when (lastEventType) {
            EventType.Add -> callback?.onInserted(lastEventPos, lastEventCount)
            EventType.Remove -> callback?.onRemoved(lastEventPos, lastEventCount)
        }
        lastEventType = EventType.None
    }

    internal enum class EventType { None, Add, Remove }

    internal class HiddenMeta(var global: Int, var local: Int, var amount: Int)

    private class RemoveEvent(var position: Int, var count: Int, var amount: Int)

}

class RecyclerList<T>(
    private val list: MutableList<T> = mutableListOf(),
    private val creator: () -> T
) : AbstractList<T>() {

    override var size: Int = list.size
        private set

    override operator fun get(index: Int): T =
        if (index < size) list[index] else throw NoSuchElementException(index.toString())

    val items get() = list.subList(0, size)

    fun alloc(i: Int = size, init: T.() -> Unit): T {
        val item: T
        if (size < list.size) {
            item = if (i == size) {
                list[i]
            } else {
                list.removeLast().also { list.add(i, it) }
            }
            size++
        } else {
            item = creator().also { list.add(i, it) }
            size = list.size
        }
        return item.apply { init(item) }
    }

    override fun clear() {
        size = 0
    }

    fun release(i: Int) {
        size--
        if (i < size) {
            list.removeAt(i).also { list.add(it) }
        }
    }

}

open class HidingList<T> private constructor(
    private val hidingItems: HidingItemsAdapter
) : AbstractList<T>(), HidingItems by hidingItems {

    private var items: List<T> = emptyList()
    private var differ: AsyncListDiffer<T>? = null
    private val list get() = differ?.currentList ?: items

    constructor(
        diffCallback: DiffUtil.ItemCallback<T>? = null,
        list: List<T>? = null,
        listUpdateCallback: ListUpdateCallback? = null
    ) : this(
        HidingItemsAdapter(listUpdateCallback)
    ) {
        if (diffCallback != null) {
            differ =
                AsyncListDiffer<T>(hidingItems, AsyncDifferConfig.Builder(diffCallback).build())
        }
        submitList(list)
    }

    // list
    override val size get() = hidingItems.getShownPos(list.size)

    override fun get(index: Int): T = list[hidingItems.getRawPos(index)]

    // hiding

    // differ
    fun submitList(newList: List<T>?) {
        differ?.submitList(newList) ?: run {
            items = newList?.let { Collections.unmodifiableList(it) } ?: emptyList()
            hidingItems.clear()
        }
    }

}