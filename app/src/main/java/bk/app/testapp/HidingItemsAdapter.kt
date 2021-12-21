package bk.app.testapp

import androidx.recyclerview.widget.ListUpdateCallback

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Адаптер виртуального скрытия элементов в списках
 *
 * Выполняет преобразование индекса элемента в его позицию в списке,
 * с учетом признака "скрыт". Например, в списке есть элементы 0, 1, 2,...
 * элемент 1 скрыт, в этом случае для элемента с индексом 0 позиция будет 0,
 * для элемента с индексом 1 позиция равна HIDDEN_ITEM, для элемента
 * с индексом 2 позиция - 1.
 *
 * Класс реализцет интерфейс ListUpdateCallback, для обработки событий
 * изменения исходного списка. Входящие события транслируются,
 * с учетом скрытых элементов, в слушатель listener
 *
 * Для ускорения операций модификации списка скрытых элементов, этот список
 * разбивается на отдельные фрагменты (бакеты), максимальный размер которых
 * определяется параметром конструктора maxBucketSize
 */
@Suppress("MemberVisibilityCanBePrivate")
class HidingItemsAdapter(
    private val maxBucketSize: Int = 1024,
    listener: ListUpdateCallback? = null
) : ListUpdateCallback, HidingItems {

    @set:JvmName("_setListener")
    var listener: ListUpdateCallback? = listener
        private set

    /**
     * Скрытые элементы представлены как список записей с полями:
     *  плоский индекс первого элемента,
     *  видимая позиция первого элемента,
     *  количество скрытых элементов до начала следующей записи
     *
     * Для получения позиции с учетом скрытых элементов по индексу элемента
     * используется формула:
     *  позиция = индекс - количество скрытых элементов от начала списка до индекса
     *
     * Для получения индекса элемента по его позиции в показанном списке
     * используется формула:
     *  индекс = позиция + количество скрытых элементов от начала списка до индекса
     *
     */

    companion object {
        const val HIDDEN_ITEM = -1
    }

    internal val table = mutableListOf<Bucket>()

    internal class Entry(
        var index: Int,
        var position: Int,
        var hidden: Int
    )

    internal class Bucket(
        var index: Int,
        var position: Int,
        val entries: MutableList<Entry>
    )

    private data class NotifyData(var x: Int = 0, var y: Int = 0) {

        fun set(x: Int, y: Int) {
            this.x = x
            this.y = y
        }

    }

    private var notifyData = NotifyData()

    /**
     * Очистить списки скрытых диапазонов
     */
    fun clear() {
        table.clear()
    }

    /**
     * Упаковать бакеты, если они сильно разрежены
     */
    fun pack() {
        if (table.isNotEmpty()) {
            var prev = table.first()
            var i = 1
            while (i < table.size) {
                val curr = table[i]
                if (prev.entries.size + curr.entries.size < maxBucketSize) {
                    prev.join(table.removeAt(i))
                } else {
                    prev = curr
                    i++
                }
            }
        }
    }

    /**
     * Получить позицию элемента по индексу
     *
     * Если элемент скрыт, возвращается значение HIDDEN_ITEM
     *
     * Для получения позиции находим ближайшую запись с индексом меньше либо равно заданному
     * если запись отсутствует, значит скрытых предшествующих элементов нет и позиция равна индексу.
     * Если запись существует, вычисляется позиция как (индекс - количество скрытых элементов).
     * Полученная позиция сравнивается с позицией записи, если результат меньше 0,
     * значит элемент скрыт и надо вернуть значение HIDDEN_ITEM
     *
     * @param index Индекс элемента
     * @return Позиция элемента, или значение HIDDEN_ITEM
     */
    fun targetPosition(index: Int): Int {
        checkIndex(index)
        return getBucket { it.index - index }?.run {
            val pos = targetPosition(index - this.index)
            if (pos == HIDDEN_ITEM) HIDDEN_ITEM else pos + position
        } ?: index
    }

    /**
     * Получить индекс элемента по позиции
     *
     * Для получения индекса находим ближайшую запись с позицией меньше либо равно заданной
     * если запись отсутствует, значит скрытых предшествующих элементов нет и индекс равен позиции.
     * Если запись существует, вычисляется индекс как (позиция + количество скрытых элементов).
     *
     * @param position Позиция элемента
     * @return Индекс элемента
     */
    fun sourceIndex(position: Int): Int {
        checkIndex(position)
        return getBucket { it.position - position }?.run {
            sourceIndex(position - this.position) + index
        } ?: position
    }

    /**
     * Проверить, является ли элемент скрытым
     *
     * Для проверки получаем позицию элемента по индексу и сравниваем со значеним HIDDEN_ITEM
     *
     * @param index Индекс элемента
     * @return True если элемент скрыт
     */
    override fun isHidden(index: Int): Boolean = targetPosition(index) == HIDDEN_ITEM

    /**
     * Скрыть элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    override fun hide(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        if (count > 0) {
            setNotifyPositions(notifyData, fromIndex, fromIndex + count)
            var i = requireBucket(fromIndex)
            table[i].apply {
                var addedHidden = hideEntries(fromIndex, count)
                addedHidden -= cutEntries(++i, fromIndex, count)
                if (addedHidden > 0) {
                    table.forEachRemaining(i) { position -= addedHidden }
                }
                splitBigBucket(this)
            }
            notifyRemove(notifyData)
        }
    }

    /**
     * Показать элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    override fun show(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        if (count == 0 || table.isEmpty()) {
            return
        }
        val notifyList = arrayListOf<NotifyData>()
        val topIndex = fromIndex + count
        var i = findBucket(fromIndex).coerceAtLeast(0)
        var removeHidden = 0
        while (i < table.size) {
            val bucket = table[i]
            if (bucket.index >= topIndex) {
                if (removeHidden > 0) {
                    table.forEachRemaining(i) { position += removeHidden }
                }
                break
            }
            bucket.position += removeHidden
            removeHidden += bucket.showEntries(fromIndex, count) { idx, cnt ->
                val pos = closestPosition(idx)
                notifyList.add(NotifyData(pos, pos + cnt))
            }
            if (!removeEmptyBucket(bucket)) {
                i++
            }
        }
        notifyList.forEach { notifyInsert(it) }
    }

    /**
     * Установить слушатель уведомлений об изменении списка элементов
     *
     * @param listener Новый слушатель уведомления об изменении списка элементов
     */
    fun setListener(listener: ListUpdateCallback?) {
        this.listener = listener
    }

    /**
     * Вставить элементы
     *
     * @param toIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     * @param visible Признак видимости вставляемых элементов
     */
    internal fun insert(toIndex: Int, count: Int, visible: Boolean = true) {
        checkIndex(toIndex)
        if (count > 0) {
            if (visible) {
                notifyData.x = closestPosition(toIndex)
                notifyData.y = notifyData.x + count
                if (table.isNotEmpty()) {
                    var i = findBucket(toIndex).coerceAtLeast(0)
                    table[i].apply {
                        if (index < toIndex) {
                            insertEntries(toIndex, count, visible)
                            i++
                        }
                    }
                    table.forEachRemaining(i) {
                        index += count
                        position += count
                    }
                }
                notifyInsert(notifyData)
            } else {
                val i = requireBucket(toIndex)
                obtainClosestBucket(table[i], i, toIndex, count)
                    .apply {
                        insertEntries(toIndex, count, visible)
                        table.forEachRemaining(i + 1) { index += count }
                        splitBigBucket(this)
                    }
            }
        }
    }

    /**
     * Удалить элементы
     *
     * @param fromIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     */
    internal fun remove(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        if (count > 0) {
            if (table.isEmpty()) {
                notifyData.set(fromIndex, fromIndex + count)
                return
            }
            setNotifyPositions(notifyData, fromIndex, fromIndex + count)
            var i = findBucket(fromIndex).coerceAtLeast(0)
            var removedHidden = 0
            table[i].apply {
                if (index <= fromIndex) {
                    removedHidden = removeEntries(fromIndex, count)
                    if (!removeEmptyBucket(this)) {
                        i++
                    }
                }
                removedHidden += cutEntries(i, fromIndex, count)
            }
            table.forEachRemaining(i) {
                index -= count
                position -= count - removedHidden
            }
            notifyRemove(notifyData)
        }
    }

    /**
     * Переместить элементы
     *
     * Перемещается только один элемент
     *
     * @param fromIndex Начальный индекс элемента
     * @param toIndex Конечный индекс элемента
     */
    internal fun move(fromIndex: Int, toIndex: Int) {
        checkIndex(fromIndex)
        checkIndex(toIndex)
        if (fromIndex == toIndex || table.isEmpty()) {
            notifyData.set(fromIndex, toIndex)
            notifyMove(notifyData)
            return
        }
        setNotifyPositions(notifyData, fromIndex, toIndex)
        var i = findBucket(fromIndex).coerceAtLeast(0)
        val src = table[i]
        var moveShown = 1
        when {
            src.index <= fromIndex -> {
                if (src.index <= toIndex && (i == table.lastIndex || toIndex < table[i + 1].index)) {
                    if (src.moveEntry(fromIndex, toIndex) == 0) {
                        notifyMove(notifyData)
                    }
                    return
                }
                moveShown -= src.removeEntries(fromIndex, 1)
            }
            toIndex < src.index -> {
                notifyMove(notifyData)
                return
            }
            else -> {
                src.index--
                src.position--
            }
        }
        var dst = src
        if (fromIndex < toIndex) {
            val topIndex = toIndex + 1
            while (i < table.lastIndex) {
                dst = table[++i]
                if (dst.index > topIndex) {
                    dst = table[--i]
                    break
                }
                dst.index--
                dst.position -= moveShown
            }
        } else {
            while (i > 0) {
                dst.index++
                dst.position += moveShown
                dst = table[--i]
                if (dst.index <= toIndex) {
                    break
                }
            }
        }
        if (moveShown == 0) {
            dst = obtainClosestBucket(dst, i, toIndex, 1)
            dst.insertEntries(toIndex, 1, false)
        } else {
            dst.insertEntries(toIndex, 1, true)
        }
        splitBigBucket(dst)
        removeEmptyBucket(src)
        if (moveShown != 0) {
            notifyMove(notifyData)
        }
    }

    // события

    override fun onInserted(position: Int, count: Int) {
        insert(position, count, true)
    }

    override fun onRemoved(position: Int, count: Int) {
        remove(position, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        move(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        notifyChange(position, count)
    }

    /**
     * Получить ближайшую видимую позицию элемента по индексу
     *
     * @param index Индекс элемента
     * @return Ближайшая возможная позиция элемента
     */
    private fun closestPosition(index: Int): Int =
        getBucket { it.index - index }?.run {
            val localIndex = index - this.index
            (getEntry(entries) { it.index - localIndex }?.let { entry ->
                (localIndex - entry.hidden).coerceAtLeast(entry.position)
            } ?: localIndex) + position
        } ?: index

    /**
     * Отправить уведомление об удалении элементов
     *
     * @param notify Данные удаляемого диапазона
     */
    private fun notifyRemove(notify: NotifyData) {
        listener?.let {
            if (notify.x < notify.y) {
                it.onRemoved(notify.x, notify.y - notify.x)
            }
        }
    }

    /**
     * Отправить уведомление о вставке элементов
     *
     * @param notify Данные вставляемого диапазона
     */
    private fun notifyInsert(notify: NotifyData) {
        listener?.let {
            if (notify.x < notify.y) {
                it.onInserted(notify.x, notify.y - notify.x)
            }
        }
    }

    /**
     * Отправить уведомление о перемещении элементов
     *
     * @param notify Данные перемещения элемента
     */
    private fun notifyMove(notify: NotifyData) {
        listener?.let {
            if (notify.x != notify.y) {
                it.onMoved(notify.x, notify.y)
            }
        }
    }

    /**
     * Отправить уведомление об изменении элементов
     *
     * @param fromIndex Индекс первого элемента изменяемого диапазона
     * @param count Количество изменяемых элементов
     */
    private fun notifyChange(fromIndex: Int, count: Int) {
        listener?.let {
            val position = closestPosition(fromIndex)
            val topPosition = closestPosition(position + count)
            if (position < topPosition) {
                it.onChanged(position, topPosition - position, null)
            }
        }
    }

    /**
     * Скрыть элементы
     *
     * В список диапазонов скрытых элементов добавляется новая запись.
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Индекс элемента
     * @param count Количество скрываемых элементов
     * @return Количество новых скрытых элементов
     */
    private fun internalHide(
        entries: MutableList<Entry>,
        startIndex: Int,
        count: Int
    ): Int {
        var startEntry: Entry
        val stopIndex = startIndex + count
        var delta = count
        var i = entries.binarySearch { it.index - startIndex }
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                startEntry = entries[i - 1]
                val top = startEntry.position + startEntry.hidden
                if (startIndex <= top) {
                    delta = stopIndex - top
                    if (delta <= 0) {
                        return 0
                    }
                    startEntry.hidden += delta
                } else {
                    startEntry =
                        Entry(startIndex, startIndex, delta).shiftPosition(-startEntry.hidden)
                    entries.add(i++, startEntry)
                }
            } else {
                startEntry = Entry(startIndex, startIndex, count)
                entries.add(i++, startEntry)
            }
        } else {
            startEntry = entries[i++]
            delta = stopIndex - startEntry.position - startEntry.hidden
            if (delta <= 0) {
                return 0
            }
            startEntry.hidden += delta
        }
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.index > stopIndex) {
                entries.forEachRemaining(i) { shiftPosition(-delta) }
                break
            }
            val top = entry.position + entry.hidden
            if (top > stopIndex) {
                startEntry.hidden += top - stopIndex
            }
            delta = startEntry.hidden - entry.hidden
            entries.removeAt(i)
        }
        return delta
    }

    /**
     * Показать элементы
     *
     * В списоке диапазонов скрытых элементов убираются записи для показываемых элементоа
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Индекс элемента
     * @param count Количество показываемых элементов
     * @param notify Коллбэк
     * @return Количество новых показанных элементов
     */
    private fun internalShow(
        entries: MutableList<Entry>,
        startIndex: Int,
        count: Int,
        notify: (Int, Int) -> Unit
    ): Int {
        val stopIndex = startIndex + count
        var delta = 0
        var i = entries.binarySearch { it.index - startIndex }
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                entries[i - 1].apply {
                    val top = position + hidden
                    if (startIndex < top) {
                        delta = top - startIndex
                        hidden -= delta
                        if (stopIndex < top) {
                            entries.add(
                                i++,
                                Entry(stopIndex, stopIndex - hidden, top - stopIndex + hidden)
                            )
                            delta = count
                        }
                        notify(startIndex, delta)
                    }
                }
            }
        } else {
            entries[i].apply {
                val top = position + hidden
                if (stopIndex < top) {
                    delta = count
                    index = stopIndex
                    shiftPosition(delta)
                    i++
                } else {
                    entries.removeAt(i)
                    delta = top - startIndex
                }
                notify(startIndex, delta)
            }
        }
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.index >= stopIndex) {
                entries.forEachRemaining(i) { shiftPosition(delta) }
                break
            }
            val top = entry.position + entry.hidden
            val saveIndex = entry.index
            val shown: Int
            if (top > stopIndex) {
                shown = stopIndex - saveIndex
                delta += shown
                entry.index = stopIndex
                entry.shiftPosition(delta)
                i++
            } else {
                shown = top - saveIndex
                delta += shown
                entries.removeAt(i)
            }
            notify(saveIndex, shown)
        }
        return delta
    }

    /**
     * Вставить элементы
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     * @param visible Признак видимости вставляемых элементов
     */
    private fun internalInsert(
        entries: MutableList<Entry>,
        startIndex: Int,
        count: Int,
        visible: Boolean
    ) {
        val stopIndex = startIndex + count
        var i = entries.binarySearch { it.index - startIndex }
        if (visible) {
            if (i < 0) {
                i = -i - 1
                if (i > 0) {
                    entries[i - 1].apply {
                        val moved = position + hidden - startIndex
                        if (moved > 0) {
                            hidden -= moved
                            entries.add(
                                i++,
                                Entry(stopIndex, stopIndex - hidden, hidden + moved)
                            )
                        }
                    }
                }
            }
            entries.forEachRemaining(i) { shiftIndex(count, 0) }
        } else {
            if (i < 0) {
                i = -i - 1
                if (i > 0) {
                    entries[--i].apply {
                        if (position + hidden < startIndex) {
                            entries.add(
                                ++i,
                                Entry(startIndex, startIndex - hidden, count + hidden)
                            )
                        } else {
                            hidden += count
                        }
                    }
                } else {
                    entries.add(i, Entry(startIndex, startIndex, count))
                }
            } else {
                entries[i].hidden += count
            }
            entries.forEachRemaining(i + 1) { shiftIndex(count, count) }
        }
    }

    /**
     * Удалить элементы
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Начальный индекс элемента
     * @param count Количество удаляемых элементов
     * @return Количество удаленных скрытых элементов
     */
    private fun internalRemove(entries: MutableList<Entry>, startIndex: Int, count: Int): Int {
        val stopIndex = startIndex + count
        var delta = 0
        var i = entries.binarySearch { it.index - startIndex }
        var startEntry: Entry? = null
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                entries[i - 1].apply {
                    val removed = position + hidden - startIndex
                    if (removed >= 0) {
                        delta = removed.coerceAtMost(count)
                        startEntry = this.also { hidden -= delta }
                    }
                }
            }
        }
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.index > stopIndex) {
                entries.forEachRemaining(i) { shiftIndex(-count, -delta) }
                break
            }
            val top = entry.position + entry.hidden
            if (top > stopIndex) {
                delta += stopIndex - entry.index
                if (startEntry != null) {
                    startEntry!!.hidden += top - stopIndex
                    entries.removeAt(i)
                } else {
                    entry.index = startIndex
                    entry.position -= count
                    entry.shiftPosition(delta)
                    i++
                }
            } else {
                delta += top - entry.index
                entries.removeAt(i)
            }
        }
        return delta
    }

    /**
     * Переместить элементы
     *
     * @param entries Список диапазонов скрытых элементов
     * @param fromIndex Индекс перемещаемого элемента
     * @param toIndex Новый индекс элемента
     * @return 0 - если переместили видимый элемент
     */
    private fun internalMove(entries: MutableList<Entry>, fromIndex: Int, toIndex: Int): Int {
        var delta = 0
        // ищем fromIndex + 1 чтобы сразу учесть краевой случай,
        // когда удаляется элемент между двух скрытых групп.
        // (при i>0, i будет указывать на следующую группу, а i-1 на предыдущую)
        val searchIndex = fromIndex + 1
        var i = entries.binarySearch { it.index - searchIndex }
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                entries[i - 1].apply {
                    val top = position + hidden
                    if (fromIndex < top) {
                        delta++
                        if (top - index == 1) {
                            // случай, когда переносим единственную скрытую группу
                            if (entries.size == 1) {
                                index = toIndex
                                position = toIndex
                                return delta
                            }
                            entries.removeAt(--i)
                        } else {
                            hidden--
                        }
                    }
                }
            }
        } else if (i > 0) {
            entries[i - 1].apply {
                if (position + hidden == fromIndex) {
                    hidden = entries.removeAt(i).hidden
                }
            }
        }
        // delta = 1, если перемещаемый элемент скрыт
        if (fromIndex < toIndex) {
            while (i < entries.size) {
                val entry = entries[i]
                if (entry.index > toIndex) {
                    // случай, когда вставка перед скрытой группой
                    if (entry.index == toIndex + 1) {
                        entry.index -= delta
                        return delta
                    }
                    break
                }
                i++
                entry.shiftIndex(-1, -delta)
            }
            if (i > 0) {
                i--
            }
        } else {
            while (i > 0) {
                val entry = entries[--i]
                if (entry.index <= toIndex) {
                    // случай, когда вставка перед скрытой группой
                    if (entry.index == toIndex) {
                        entry.index += 1 - delta
                        entry.position += 1 - delta
                        entry.hidden += delta
                        return delta
                    }
                    break
                }
                entry.shiftIndex(1, delta)
            }
        }
        // вставим удаленный элемент
        // i указывает на первую скрытую группу, следующую за toIndex по направлению сдвига
        entries[i].apply {
            val top = position + hidden
            when {
                // случай, когда toIndex < fromIndex и < первой скрытой группы
                toIndex < index -> if (delta != 0) {
                    entries.add(0, Entry(toIndex, toIndex, 1))
                }
                // случай, когда вставка скрытой группы между видимыми группами,
                // ситуации вставки перед скрытой группой обрабатываются выше
                toIndex > top -> if (delta != 0) {
                    entries.add(i + 1, Entry(toIndex, toIndex - hidden, hidden + 1))
                }
                // случай, когда вставка скрытой группы внутрь или сразу после скрытой группы
                delta != 0 -> {
                    hidden++
                }
                // случай, когда вставка видимой группы внутрь скрытой группы
                toIndex < top -> {
                    entries.add(i + 1, Entry(toIndex + 1, position + 1, hidden))
                    hidden = toIndex - position
                }
            }
        }
        return delta
    }

    /**
     * Проверить индекс на корректность
     *
     * Если индекс < 0 вызывается исключение
     *
     * @param index Проверяемый индекс
     */
    private fun checkIndex(index: Int) {
        check(index >= 0) { "Index out of bounds" }
    }

    /**
     * Получть запись по условию
     *
     * @param list Список записей
     * @param comparison Функция проверки условия поиска записи,
     *  принимает парамером запись, возвращает результат сравнения (-1, 0, 1)
     * @return Найденная запись, либо null
     */
    private fun getEntry(list: List<Entry>, comparison: (Entry) -> Int): Entry? {
        val i = list.binarySearch(comparison = comparison)
        return if (i == -1) null else list[if (i < 0) -i - 2 else i]
    }

    /**
     * Получть бакет по условию
     *
     * @param comparison Функция проверки условия поиска бакета,
     *  принимает парамером бакет, возвращает результат сравнения (-1, 0, 1)
     * @return Найденный бакет, либо null
     */
    private fun getBucket(comparison: (Bucket) -> Int): Bucket? {
        val i = table.binarySearch(comparison = comparison)
        return if (i == -1) null else table[if (i < 0) -i - 2 else i]
    }

    /**
     * Получить бакет, содержащий элемент с указанным индексом
     *
     * Создать первый бакет, или расширить существующий,
     * чтобы он содержал элемент с указанным индексом
     *
     * @param startIndex Индекс элемента, для которого нужен бакет
     * @return Индекс бакета
     */
    private fun requireBucket(startIndex: Int): Int {
        if (table.isNotEmpty()) {
            val i = findBucket(startIndex)
            if (i >= 0) {
                return i
            }
            table.first().apply {
                if (entries.size < maxBucketSize) {
                    moveStart(startIndex)
                    return 0
                }
            }
        }
        table.add(0, Bucket(startIndex, startIndex, mutableListOf()))
        return 0
    }

    /**
     * Найти бакет, содержащий элемент с указанным индексом
     *
     * @param index Индекс искомого элемента
     * @return Индекс бакета
     */
    private fun findBucket(index: Int): Int {
        val result = table.binarySearch { it.index - index }
        return if (result < 0) -result - 2 else result
    }

    /**
     * Определить лучший бакет для вставки скрытых элементов
     *
     * Если это вставка скрытых элементов и приходится в начало переданного бакета,
     * при этом новый диапазон будет продолжением скрытых элементов предыдущего бакета,
     * то вставку делаем сразу в предыдущий бакет.
     *
     * @param curr Текущий бакет
     * @param bucketIndex Индекс текущего бакета
     * @param index Индекс вставки элементов
     * @param count Количество вставляемых элементов
     * @return Лучший бакет для вставки
     */
    private fun obtainClosestBucket(
        curr: Bucket, bucketIndex: Int, index: Int, count: Int
    ): Bucket {
        if (curr.index == index && bucketIndex > 0) {
            val prev = table[bucketIndex - 1]
            val last = prev.entries.last()
            if (prev.index + last.position + last.hidden == index) {
                curr.index += count
                return prev
            }
        }
        return curr
    }

    /**
     * Объединить смежные записи в разных бакетах
     *
     * Если текущий бакет начинается со диапазона скрытых элементов,
     * продолжающий последний диапазон предыдущего бакета,
     * то переносим из текущего бакета начальный диапазон в конец предыдущего
     *
     * @param bucketIndex Индекс текущего бакета
     * @param startIndex Индекс начала текущего диапазона
     * @param topIndex Индекс конечного элемента
     */
    private fun mergeContiguousEntries(bucketIndex: Int, startIndex: Int, topIndex: Int) {
        if (bucketIndex > 0 && bucketIndex < table.size) {
            val curr = table[bucketIndex]
            val first = curr.entries.first()
            if (curr.index + first.index == topIndex) {
                val prev = table[bucketIndex - 1]
                val tail = prev.entries.last()
                if (prev.index + tail.position + tail.hidden >= startIndex) {
                    tail.hidden += curr.moveStart(topIndex + first.hidden)
                    removeEmptyBucket(curr)
                }
            }
        }
    }

    /**
     * Вырезать диапазон элементов
     *
     * @param bucketIndex Индекс бакета
     * @param startIndex Индекс первого элемента диапазона
     * @param count Количество элементов диапазона
     * @return Количество удаленных скрытых элементоа
     */
    private fun cutEntries(bucketIndex: Int, startIndex: Int, count: Int): Int {
        val topIndex = startIndex + count
        var removedHidden = 0
        while (bucketIndex < table.size) {
            val curr = table[bucketIndex]
            if (topIndex < curr.index) {
                break
            }
            val last = curr.entries.last()
            if (curr.index + last.position + last.hidden > topIndex) {
                removedHidden += curr.moveStart(topIndex)
                if (!removeEmptyBucket(curr)) {
                    // мержим предыдущий диапазон с хвостом
                    mergeContiguousEntries(bucketIndex, startIndex, topIndex)
                }
                break
            }
            removedHidden += last.hidden
            table.removeAt(bucketIndex)
        }
        return removedHidden
    }

    /**
     * Разделить бакет, если его размер стал слишком большим
     *
     * @return Коичество добавленных бакетов
     */
    private fun splitBigBucket(bucket: Bucket): Int {
        bucket.apply {
            if (entries.size <= maxBucketSize) {
                return 0
            }
            val i = entries.size shr 1
            var entry = entries[i]
            val delta = entry.index
            val hidden = entry.index - entry.position
            val newBucket =
                Bucket(index + delta, position + delta - hidden, mutableListOf())
            table.add(table.indexOf(bucket) + 1, newBucket)
            while (i < entries.size) {
                entry = entries.removeAt(i)
                entry.shiftIndex(-delta, -hidden)
                newBucket.entries.add(entry)
            }
        }
        return 1
    }

    /**
     * Удалить бакет, если он пустой
     *
     * @return True если бакет был удален
     */
    private fun removeEmptyBucket(bucket: Bucket): Boolean {
        if (bucket.entries.isNotEmpty()) {
            return false
        }
        return table.remove(bucket)
    }

    /**
     * Установить позиции уведомления с перещетом из индексов элементов
     *
     * @param notify Данные уведомления
     * @param x Индекс 1 элемента
     * @param y Индекс 2 элемента
     */
    private fun setNotifyPositions(notify: NotifyData, x: Int, y: Int) {
        notify.x = closestPosition(x)
        notify.y = closestPosition(y)
    }

    /**
     * Выполнить действие для всех элементов до конца списка
     *
     * @param startIndex Начальный индекс
     * @param action Действие
     */
    private inline fun <T> List<T>.forEachRemaining(startIndex: Int, action: T.() -> Unit) {
        for (i in startIndex until size) action(get(i))
    }

    /**
     * Сдвинуть позицию диапазона скрытых элементов
     *
     * При сдвиге вниз (-) уменьшаем позицию, и увеличиваем количество скрытых
     * При сдвиге вверх (+) увеличиваем позицию, и уменьшаем количество скрытых
     *
     * @param delta Величина свдига
     * @return Запись диапазона скрытых элементов
     */
    private fun Entry.shiftPosition(delta: Int): Entry {
        position += delta
        hidden -= delta
        return this
    }

    /**
     * Сдвинуть индекс диапазона скрытых элементов
     *
     * При сдвиге вниз (-) уменьшаем индекс и позицию, и увеличиваем количество скрытых
     * При сдвиге вверх (+) увеличиваем индекс и позицию, и уменьшаем количество скрытых
     *
     * @param delta Величина свдига
     * @param deltaHidden Количество пропускаемых скрытых элементов
     * @return Запись диапазона скрытых элементов
     */
    private fun Entry.shiftIndex(delta: Int, deltaHidden: Int): Entry {
        index += delta
        position += delta - deltaHidden
        hidden += deltaHidden
        return this
    }

    /**
     * Получить позицию элемента по индексу в пределах бакета
     *
     * @param index Индекс элемента
     * @return Позиция элемента или значение HIDDEN_ITEM, если элемент скрыт
     */
    private fun Bucket.targetPosition(index: Int): Int =
        getEntry(entries) { it.index - index }?.let { entry ->
            val pos = index - entry.hidden
            if (pos < entry.position) HIDDEN_ITEM else pos
        } ?: index

    /**
     * Получить индекс элемента по позиции в пределах бакета
     *
     * @param position Позиция элемента
     * @return Индекс элемента
     */
    private fun Bucket.sourceIndex(position: Int): Int =
        position + (getEntry(entries) { it.position - position }?.hidden ?: 0)

    /**
     * Скрыть элементы в пределах бакета
     *
     * Обертка над функцией скрытия элементов
     *
     * @param startIndex Индекс элемента
     * @param count Количество скрываемых элементов
     * @return Количество новых скрытых элементов
     */
    private fun Bucket.hideEntries(startIndex: Int, count: Int): Int =
        internalHide(entries, startIndex - index, count)

    /**
     * Показать элементы в пределах бакета
     *
     * Обертка над функцией показа элементов
     *
     * @param startIndex Индекс элемента
     * @param count Количество показываемых элементов
     * @param notify Коллбэк
     * @return Количество новых показанных элементов
     */
    private fun Bucket.showEntries(
        startIndex: Int,
        count: Int,
        notify: (Int, Int) -> Unit
    ): Int {
        val offset = (index - startIndex).coerceAtLeast(0)
        return internalShow(
            entries, startIndex - index + offset, count - offset
        ) { i, c -> notify(i + index, c) }
    }

    /**
     * Удалить элементы в пределах бакета
     *
     * Обертка над функцией удаления элементов
     *
     * @param startIndex Индекс элемента
     * @param count Количество удаляемых элементов
     * @return Количество удаленных скрытых элементов
     */
    private fun Bucket.removeEntries(startIndex: Int, count: Int): Int {
        return internalRemove(entries, startIndex - index, count)
    }

    /**
     * Вставить элементы в пределах бакета
     *
     * Обертка над функцией вставки элементов
     *
     * @param startIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     * @param visible Признак видимости вставляемых элементов
     */
    private fun Bucket.insertEntries(startIndex: Int, count: Int, visible: Boolean) {
        if (startIndex < index) {
            if (visible) {
                index += count
                position += count
                return
            }
            moveStart(startIndex)
        }
        internalInsert(entries, startIndex - index, count, visible)
    }

    /**
     * Переместить элемент в пределах бакета
     *
     * Обертка над функцией перемещения элементов
     *
     * @param fromIndex Начальный индекс элемента
     * @param toIndex Конечный индекс элемента
     * @return 0 - если переместили видимый элемент
     */
    private fun Bucket.moveEntry(fromIndex: Int, toIndex: Int): Int {
        return internalMove(entries, fromIndex - index, toIndex - index)
    }

    /**
     * Сдвинуть начало бакета на новый индекс
     *
     * @param newIndex Новый индекс
     * @return Количество удаленных скрытых элементов
     */
    private fun Bucket.moveStart(newIndex: Int): Int {
        var hidden = 0
        val offset = newIndex - index
        if (offset > 0) {
            hidden = internalRemove(entries, 0, offset)
        } else {
            entries.forEach { it.shiftIndex(-offset, 0) }
        }
        index = newIndex
        position += offset - hidden
        return hidden
    }

    /**
     * Присоединить элементы другого бакета
     *
     * @param other Бакет, диапазоны которого присоединяются
     */
    private fun Bucket.join(other: Bucket) {
        val delta = other.index - index
        val hidden = entries.last().hidden
        other.entries.forEach { entries.add(it.shiftIndex(delta, hidden)) }
    }

}