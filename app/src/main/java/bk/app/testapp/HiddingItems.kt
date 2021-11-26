package bk.app.testapp

class HidingItemsImpl(private val BucketSize: Int = 1024) {

    /**
     * Скрытые элемеенты представлены как список записей с полями:
     *  индекс первого элемента,
     *  позиция первого элемента,
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

    private val table = mutableListOf<Bucket>()

    private class Entry(
        var index: Int,
        var position: Int,
        var hidden: Int
    )

    private class Bucket(
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
        return getBucket(table) { it.index - index }?.let { bucket ->
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
        return getBucket(table) { it.position - position }?.let { bucket ->
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
        var i = table.binarySearch { it.index - fromIndex }
        if (i == -1) {
            // TODO ("если в первый бакет можно добавить записи, берем его, иначе создаем новый")
            table.add(0, Bucket(fromIndex, fromIndex, mutableListOf()))
            i = 0
        } else if (i < 0) {
            i = -i - 2
        }
        val top = fromIndex + count
        var delta = 0
        do {
            table[i++].apply {
                position -= delta
                val shift = top - index
                if (shift > 0) {
                    if (fromIndex >= index) {
                        delta = hide(fromIndex, count) { index, count -> }
                        optimizeBucket(this)
                    } else {
                        delta -= shift(shift, i)
                        if (removeBucketIdNeed(this)) {
                            i--
                        }
                    }
                }
            }
        } while (i < table.size)
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
        var i = table.binarySearch { it.index - fromIndex }
        if (i == -1) {
            i = 0
        } else if (i < 0) {
            i = -i - 2
        }
        val top = fromIndex + count
        var delta = 0
        while (i < table.size) {
            table[i++].apply {
                position += delta
                if (index < top) {
                    delta += show(fromIndex, count) { index, count -> }
                    if (removeBucketIdNeed(this)) {
                        i--
                    }
                }
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
    private fun insert(toIndex: Int, count: Int, visible: Boolean = true) {
        checkIndex(toIndex)
        checkCount(count, toIndex)
        if (visible) {
            if (table.isEmpty()) {
                // TODO ("notify")
                return
            }
            var i = table.binarySearch { it.index - toIndex }
            if (i != -1) {
                i = i.coerceAtLeast(-i - 2)
                table[i].apply {
                    insert(toIndex, count, visible)
                    optimizeBucket(this)
                }
            }
            while (++i < table.size) {
                table[i].apply {
                    index += count
                    position += count
                }
            }
            // TODO ("notify")
        } else {
            var i = table.binarySearch { it.index - toIndex }
            if (i == -1) {
                // TODO ("если в первый бакет можно добавить записи, берем его, иначе создаем новый")
                table.add(0, Bucket(toIndex, toIndex, mutableListOf()))
                i = 0
            } else if (i < 0) {
                i = -i - 2
            }
            table[i].apply {
                insert(toIndex, count, visible)
                optimizeBucket(this)
            }
            while (++i < table.size) table[i].index += count
        }
    }

    /**
     * Удалить элементы
     *
     * @param fromIndex Начальный индекс элемента
     * @param count Количество вставляемых элементов
     */
    private fun remove(fromIndex: Int, count: Int) {
        checkIndex(fromIndex)
        checkCount(count, fromIndex)
        // TODO ("notify")
        if (table.isEmpty()) {
            return
        }
        var i = table.binarySearch { it.index - fromIndex }
        if (i == -1) {
            i = 0
        } else if (i < 0) {
            i = -i - 2
        }
        val top = fromIndex + count
        var delta = 0
        while (i < table.size) {
            table[i++].apply {
                position += delta
                if (index < top) {
                    // TODO ("Нужно учесть ситуацию, когда после удаления, скрытая группа в предыдущем бакете продолжается в текущем")
                    delta += remove(fromIndex, count)
                    position -= index + count - top
                    index = fromIndex
                    if (removeBucketIdNeed(this)) {
                        i--
                    }
                } else {
                    index -= count
                    position -= count
                }
            }
        }
    }

    /**
     * Переместить элементы
     *
     * @param fromIndex Начальный индекс элемента
     * @param toIndex Конечный индекс элемента
     */
    private fun move(fromIndex: Int, toIndex: Int) {
        checkIndex(fromIndex)
        checkIndex(toIndex)
        // TODO ("notify")
        if (table.isEmpty() || fromIndex == toIndex) {
            return
        }
        val a = table.binarySearch { it.index - fromIndex }
        val b = table.binarySearch { it.index - toIndex }
        // когда перемещение в пределах одного бакета
        if (a == b) {
            table[a].apply { internalMove(entries, fromIndex - index, toIndex - index) }
        } else {
            // TODO ("Надо учесть случаи, когда надо объединить скрытые диапазон в соседних бакетах при удалении перемещенного элемента")
            var i = a
            val delta = table[i].remove(fromIndex, 1)
            if (fromIndex < toIndex) {
                while (i < b) table[++i].apply {
                    index--
                    position -= 1 - delta
                }
            } else {
                while (i > b) table[i--].apply {
                    index++
                    position += 1 - delta
                }
            }
            table[i].insert(toIndex, 1, delta == 0)
            optimizeBucket(table[i])
            removeBucketIdNeed(table[a])
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
                do entries[i++].shiftPosition(-delta) while (i < entries.size)
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
                                i++, Entry(stopIndex, stopIndex - hidden, top - stopIndex + hidden)
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
                do entries[i++].shiftPosition(delta) while (i < entries.size)
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
            while (i < entries.size) entries[i++].shiftIndex(count, 0)
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
            while (++i < entries.size) entries[i].shiftIndex(count, 0)
        }
    }

    /**
     * Удалить элементы
     *
     * @param entries Список диапазонов скрытых элементов
     * @param startIndex Начальный индекс элемента
     * @param count Количество удаляемых элементов
     */
    private fun internalRemove(entries: MutableList<Entry>, startIndex: Int, count: Int): Int {
        val stopIndex = startIndex + count
        var delta = 0
        var i = entries.binarySearch { it.index - startIndex }
        var startEntry: Entry? = null
        if (i < 0) {
            i = -1 - 1
            if (i > 0) {
                entries[i - 1].apply {
                    val removed = position + hidden - startIndex
                    if (removed > 0) {
                        delta = removed.coerceAtMost(count)
                        startEntry = this.also { hidden -= delta }
                    }
                }
            }
        }
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.index > stopIndex) {
                do entries[i++].shiftIndex(-count, -delta) while (i < entries.size)
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
                            if (i > entries.size) {
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
            while (i < entries.size) {
                val entry = entries[i++]
                if (entry.index > toIndex) {
                    // случай, когда вставка перед скрытой группой
                    if (entry.index == toIndex + 1) {
                        entry.index -= delta
                        return
                    }
                    break
                }
                entry.shiftIndex(-1, -delta)
            }
            i--
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
                toIndex < index -> if (delta > 0) {
                    entries.add(0, Entry(toIndex, toIndex, 1))
                }
                // случай, когда вставка группы между видимыми группами,
                // ситуации вставки перед скрытой группой обрабатываются выше
                toIndex > top -> if (delta > 0) {
                    entries.add(i + 1, Entry(toIndex, toIndex - hidden, hidden + 1))
                }
                // случай, когда вставка скрытой группы внутрь или сразу после скрытой группы
                delta > 0 -> {
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
     * @param list Список бакетов
     * @param comparison Функция проверки условия поиска бакета,
     *  принимает парамером бакет, возвращает результат сравнения (-1, 0, 1)
     * @return Найденный бакет, либо null
     */
    private fun getBucket(list: List<Bucket>, comparison: (Bucket) -> Int): Bucket? {
        val i = list.binarySearch(comparison = comparison)
        return if (i == -1) null else list[if (i < 0) -i - 2 else i]
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
    private fun Bucket.hide(startIndex: Int, count: Int, notify: (Int, Int) -> Unit): Int =
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
    private fun Bucket.show(startIndex: Int, count: Int, notify: (Int, Int) -> Unit): Int {
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
     * @return Количество удаленных элементов
     */
    private fun Bucket.remove(startIndex: Int, count: Int): Int {
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
    private fun Bucket.insert(startIndex: Int, count: Int, visible: Boolean) {
        internalInsert(entries, startIndex - index, count, visible)
    }

    /**
     * Сдвинуть начало бакета вверх
     *
     * Используется для случаев, когда диапазон элементов, скрываемых в предыдущих бакетах,
     * перекрывает начало бакета.
     *
     * При сдвиге увеличиваем индекс и позицию бакета на велечину сдвига.
     * Записи в бакете, попадающие в перекрываемый диапазон, удаляем, остальные корректируем:
     * Индекс записи = Индекс записи - сдвиг
     * Позиция записи = Позиция записи - (сдвиг - удаленные скрытые элементы)
     * Коичество скрытых элементов = Коичество скрытых элементов - удаленные скрытые элементы
     * Если при удалении записей сдвиг попадает в середину диапазона скрытых элементов,
     * то увеличиваем последний диапазон предыдущего бакета на остаток удаляемого диапазона
     *
     * @param shift Величина сдвига
     * @param bucketIndex Индекс бакета в таблице
     * @return Количество удаленных скрытых элементов
     */
    private fun Bucket.shift(shift: Int, bucketIndex: Int): Int {
        val entries = entries
        var removed = 0
        var i = 0
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.index >= shift) {
                do entries[i++].shiftIndex(-shift, -removed) while (i < entries.size)
                break
            }
            val top = entry.position + entry.hidden
            if (top > shift) {
                table[bucketIndex - 1].entries.last().hidden += top - shift
            }
            removed = entries.removeAt(i).hidden
        }
        index += shift
        position += shift
        return removed
    }

    /**
     * Удалить бакет, если он пустой
     *
     * @param bucket Удаляемый бакет
     * @return true если бакет был удален
     */
    private fun removeBucketIdNeed(bucket: Bucket): Boolean {
        if (bucket.entries.size > 0) {
            return false
        }
        return table.remove(bucket)
    }

    /**
     * Оптимизировать размер бакета
     *
     * @param bucket Сокращаемый бакет
     */
    private fun optimizeBucket(bucket: Bucket) {}

}