package bk.app.testapp

class HidingItemsImpl(private val maxBucketSize: Int = 1024) {

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
    fun positionByIndex(index: Int): Int {
        checkIndex(index)
        return getBucket { it.index - index }?.let { bucket ->
            val pos = bucket.positionByIndex(index - bucket.index)
            if (pos == HIDDEN_ITEM) HIDDEN_ITEM else pos + bucket.position
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
    fun indexByPosition(position: Int): Int {
        checkIndex(position)
        return getBucket { it.position - position }?.let { bucket ->
            bucket.indexByPosition(position - bucket.position) + bucket.index
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
    fun isHidden(index: Int): Boolean = positionByIndex(index) == HIDDEN_ITEM

    /**
     * Скрыть элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    fun hide(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        checkCount(count, fromIndex)
        val topIndex = fromIndex + count
        var i = requireBucket(fromIndex)
        var hidden = 0
        while (i < table.size) {
            val bucket = table[i]
            if (bucket.index >= topIndex) {
                table.forEachRemaining(i) { position -= hidden }
                break
            }
            if (bucket.index <= fromIndex) {
                hidden += bucket.hideEntries(fromIndex, count) { i, c -> }
                if (bucket.splitIfBig()) {
                    i++
                }
            } else {
                bucket.position -= hidden
                i = mergeContiguousEntries(i)
            }
            i++
        }
    }

    /**
     * Показать элементы
     *
     * @param fromIndex Индекс первого элемента
     * @param count Количество элементов
     */
    fun show(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        checkCount(count, fromIndex)
        if (table.isEmpty()) {
            return
        }
        val topIndex = fromIndex + count
        var delta = 0
        var i = findBucket(fromIndex).coerceAtLeast(0)
        while (i < table.size) {
            val bucket = table[i]
            if (bucket.index >= topIndex) {
                table.forEachRemaining(i) { position += delta }
                break
            }
            bucket.position += delta
            delta += bucket.showEntries(fromIndex, count) { i, c -> }
            if (!bucket.removeIfEmpty()) {
                i++
            }
        }
    }

    /**
     * Вставить элементы
     *
     * @param toIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     * @param visible Признак видимости вставляемых элементов
     */
    fun insert(toIndex: Int, count: Int, visible: Boolean = true) {
        checkIndex(toIndex)
        checkCount(count, toIndex)
        if (visible) {
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
            // TODO ("notify")
        } else {
            val i = requireBucket(toIndex)
            obtainClosestBucket(table[i], i, toIndex, count)
                .apply {
                    insertEntries(toIndex, count, visible)
                    table.forEachRemaining(i + 1) { index += count }
                    splitIfBig()
                }
        }
    }

    /**
     * Удалить элементы
     *
     * @param fromIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     */
    fun remove(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        checkCount(count, fromIndex)
        if (table.isEmpty()) {
            // TODO ("notify")
            return
        }
        val topIndex = fromIndex + count
        var removedHidden = 0
        var i = findBucket(fromIndex).coerceAtLeast(0)
        while (i < table.size) {
            val bucket = table[i]
            if (bucket.index >= topIndex) {
                break
            }
            if (bucket.index <= fromIndex) {
                removedHidden += bucket.removeEntries(fromIndex, count)
            } else {
                bucket.position += removedHidden
                removedHidden += bucket.removeEntries(fromIndex, count)
                bucket.position -= bucket.index - fromIndex
                bucket.index = fromIndex
            }
            if (!bucket.removeIfEmpty()) {
                i++
            }
        }
        table.forEachRemaining(mergeContiguousEntries(i)) {
            index -= count
            position -= count + removedHidden
        }
        // TODO ("notify")
    }

    /**
     * Переместить элементы
     *
     * Перемещается только один элемент
     *
     * @param fromIndex Начальный индекс элемента
     * @param toIndex Конечный индекс элемента
     */
    fun move(fromIndex: Int, toIndex: Int) {
        checkIndex(fromIndex)
        checkIndex(toIndex)
        if (table.isEmpty() || fromIndex == toIndex) {
            // TODO ("notify")
            return
        }
        var i = findBucket(fromIndex).coerceAtLeast(0)
        var bucket = table[i]
        var moveShown = 1
        when {
            bucket.index <= fromIndex -> {
                if (bucket.index <= toIndex && (i == table.lastIndex || toIndex < table[i + 1].index)) {
                    bucket.moveEntry(fromIndex, toIndex)
                    // TODO ("notify")
                    return
                }
                moveShown -= bucket.removeEntries(fromIndex, 1)
            }
            toIndex < bucket.index -> {
                // TODO ("notify")
                return
            }
            else -> {
                bucket.index--
                bucket.position--
            }
        }
        if (fromIndex < toIndex) {
            while (i < table.lastIndex) {
                bucket = table[++i]
                if (bucket.index > toIndex) {
                    break
                }
                bucket.index--
                bucket.position -= moveShown
            }
        } else {
            while (i > 0) {
                bucket = table[--i]
                if (bucket.index <= toIndex) {
                    break
                }
                bucket.index++
                bucket.position += moveShown
            }
        }
        if (moveShown == 0) {
            bucket = obtainClosestBucket(bucket, i, toIndex, 1)
            bucket.insertEntries(toIndex, 1, false)
        } else {
            bucket.insertEntries(toIndex, 1, true)
        }
        bucket.splitIfBig()
        // TODO ("notify")
    }

    /**
     * Скрыть элементы
     *
     * В список диапазонов скрытых элементов добавляется новая запись.
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Индекс элемента
     * @param count Количество скрываемых элементов
     * @param notify Коллбэк
     * @return Количество новых скрытых элементов
     */
    private fun internalHide(
        entries: MutableList<Entry>,
        startIndex: Int,
        count: Int,
        notify: (Int, Int) -> Unit
    ): Int {
        notify(startIndex, count)
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
     * @param count Количество удаленных скрытых элементов
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
     */
    private fun internalMove(entries: MutableList<Entry>, fromIndex: Int, toIndex: Int) {
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
                                return
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
            delta = -delta
            while (i < entries.size) {
                val entry = entries[i]
                if (entry.index > toIndex) {
                    // случай, когда вставка перед скрытой группой
                    if (entry.index == toIndex + 1) {
                        entry.index += delta
                        return
                    }
                    break
                }
                i++
                entry.shiftIndex(-1, delta)
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
                        return
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
     * Проверить счетчик на корректность
     *
     * Если счетчик < 0 вызывается исключение
     *
     * @param count Проверяемый счетчик
     * @param index Проверяемый индекс
     */
    private fun checkCount(count: Int, index: Int) {
        check(count >= 0) { "Count out of bounds" }
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
     * @param index Индекс текущего бакета
     * @param entryIndex Индекс вставляемых элементов
     * @param count Количество вставляемых элементов
     * @return Лучший бакет для вставки
     */
    private fun obtainClosestBucket(curr: Bucket, index: Int, entryIndex: Int, count: Int): Bucket {
        if (curr.index == entryIndex && index > 0) {
            val prev = table[index - 1]
            with(prev.entries.last()) {
                if (prev.index + position + hidden == entryIndex) {
                    curr.index += count
                    return prev
                }
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
     * @param index Индекс текущего бакета
     * @return Индекс текущего бакета после переноса элементов
     */
    private fun mergeContiguousEntries(index: Int): Int {
        if (index > 0 && index < table.size) {
            val prev = table[index - 1]
            val curr = table[index]
            prev.entries.lastOrNull()?.let { head ->
                var end = prev.index + head.position + head.hidden - curr.index
                var hidden = 0
                while (curr.entries.isNotEmpty()) {
                    val entry = curr.entries[0]
                    if (entry.index > end) {
                        break
                    }
                    val left = entry.position + entry.hidden - end
                    hidden = curr.entries.removeAt(0).hidden
                    if (left > 0) {
                        end += left
                        head.hidden += left
                        break
                    }
                }
                if (curr.entries.isEmpty()) {
                    curr.removeIfEmpty()
                    return index - 1
                }
                if (end > 0) {
                    curr.entries.forEach { it.shiftIndex(-end, -hidden) }
                    curr.index += end
                    curr.position += end - hidden
                }
            }
        }
        return index
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
    private fun Bucket.positionByIndex(index: Int): Int =
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
    private fun Bucket.indexByPosition(position: Int): Int =
        position + (getEntry(entries) { it.position - position }?.hidden ?: 0)

    /**
     * Скрыть элементы в пределах бакета
     *
     * Обертка над функцией скрытия элементов
     *
     * @param startIndex Индекс элемента
     * @param count Количество скрываемых элементов
     * @param notify Коллбэк
     * @return Количество новых скрытых элементов
     */
    private fun Bucket.hideEntries(startIndex: Int, count: Int, notify: (Int, Int) -> Unit): Int =
        internalHide(entries, startIndex - index, count) { i, c -> notify(i + index, c) }

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
    private fun Bucket.showEntries(startIndex: Int, count: Int, notify: (Int, Int) -> Unit): Int {
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
        val offset = (index - startIndex).coerceAtLeast(0)
        return internalRemove(
            entries, startIndex - index + offset, count - offset
        )
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
     */
    private fun Bucket.moveEntry(fromIndex: Int, toIndex: Int) {
        internalMove(entries, fromIndex - index, toIndex - index)
    }

    /**
     * Сдвинуть начало бакета на новый индекс
     *
     * @param newIndex Новый индекс
     */
    private fun Bucket.moveStart(newIndex: Int) {
        val offset = index - newIndex
        index = newIndex
        position -= offset
        entries.forEach { it.shiftIndex(offset, 0) }
    }

    /**
     * Удалить бакет, если он пустой
     *
     * @return True если бакет был удален
     */
    private fun Bucket.removeIfEmpty(): Boolean {
        if (entries.isNotEmpty()) {
            return false
        }
        return table.remove(this)
    }

    /**
     * Разделить бакет, если его размер стал слишком большим
     */
    private fun Bucket.splitIfBig(): Boolean {
        if (entries.size < maxBucketSize) {
            return false
        }
        val i = entries.size shr 1
        var entry = entries[i]
        val delta = entry.index
        val hidden = entry.index - entry.position
        val newBucket =
            Bucket(index + delta, position + delta - hidden, mutableListOf())
        while (i < entries.size) {
            entry = entries.removeAt(i)
            entry.shiftIndex(-delta, -hidden)
            newBucket.entries.add(entry)
        }
        return true
    }

}