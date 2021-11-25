package bk.app.testapp

class HidingItemsImpl {

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

    private interface Address {
        var index: Int
        var position: Int
    }

    private class Entry(
        override var index: Int,
        override var position: Int,
        var numberOfHidden: Int
    ) : Address

    private class Bucket(
        override var index: Int,
        override var position: Int,
        val entries: MutableList<Entry>
    ) : Address

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
            if (pos == HIDDEN_ITEM) HIDDEN_ITEM else pos + bucket.index
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
            bucket.indexByPosition(position - bucket.position) + bucket.position
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
     * @param index Индекс первого элемента
     * @param count Количество элементов
     */
    fun hide(index: Int, count: Int) {
        checkIndex(index)
        checkCount(count, index)
        var i = table.binarySearch { it.index - index }
        if (i == -1) {
            // TODO ("если в первый бакет можно добавить записи, берем его, иначе создаем новый")
            table.add(0, Bucket(index, index, mutableListOf()))
            i = 0
        } else if (i < 0) {
            i = -i - 2
        }
        val top = index + count
        var delta = 0
        do {
            val bucket = table[i++]
            bucket.position -= delta
            if (bucket.index < top) {
                delta += bucketHide(bucket.entries, index - bucket.index, count)
            }
        } while (i < table.size)
    }

    private fun bucketHide(entries: MutableList<Entry>, startIndex: Int, count: Int): Int {
        var startEntry: Entry
        val stopIndex = startIndex + count
        var delta = count
        var i = entries.binarySearch { it.index - startIndex }
        if (i < 0) {
            i = -i - 1
            if (i > 0) {
                startEntry = entries[--i]
                val top = startEntry.position + startEntry.numberOfHidden
                if (startIndex <= top) {
                    delta = stopIndex - top
                    if (delta <= 0) {
                        return 0
                    }
                    startEntry.numberOfHidden += delta
                } else {
                    startEntry = Entry(
                        startIndex,
                        startIndex - startEntry.numberOfHidden,
                        delta + startEntry.numberOfHidden
                    )
                    entries.add(++i, startEntry)
                }
            } else {
                startEntry = Entry(startIndex, startIndex, count)
                entries.add(i, startEntry)
            }
        } else {
            startEntry = entries[i]
            delta = stopIndex - startEntry.position - startEntry.numberOfHidden
            if (delta <= 0) {
                return 0
            }
            startEntry.numberOfHidden += delta
        }
        while (++i < entries.size) {
            val entry = entries[i]
            if (entry.index <= stopIndex) {
                val top = entry.position + entry.numberOfHidden
                if (top > stopIndex) {
                    startEntry.numberOfHidden += top - stopIndex
                }
                delta = startEntry.numberOfHidden - entry.numberOfHidden
                entries.removeAt(i--)
            } else {
                entry.position -= delta
                entry.numberOfHidden += delta
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
    private fun getEntry(list: List<Entry>, comparison: (Address) -> Int): Entry? {
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
    private fun getBucket(list: List<Bucket>, comparison: (Address) -> Int): Bucket? {
        val i = list.binarySearch(comparison = comparison)
        return if (i == -1) null else list[if (i < 0) -i - 2 else i]
    }

    /**
     * Получить бакет по индексу элемента
     *
     * @param index Индекс элемента
     * @return Найденный бакет, либо null
     */
    private fun getBucket(index: Int): Bucket? = getBucket(table) { it.index - index }

    /**
     * Получить позицию элемента по индексу в пределах бакета
     *
     * @param index Индекс элемента
     * @return Позиция элемента или значение HIDDEN_ITEM, если элемент скрыт
     */
    private fun Bucket.positionByIndex(index: Int): Int =
        getEntry(entries) { it.index - index }?.let { entry ->
            val pos = index - entry.numberOfHidden
            if (pos < entry.position) HIDDEN_ITEM else pos
        } ?: index

    /**
     * Получить индекс элемента по позиции в пределах бакета
     *
     * @param position Позиция элемента
     * @return Индекс элемента
     */
    private fun Bucket.indexByPosition(position: Int): Int =
        getEntry(entries) { it.position - position }?.let { entry ->
            position + entry.numberOfHidden
        } ?: position

}