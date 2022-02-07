package bk.expandablelist

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Интерфейс поддержки свертки/развертки групп элементов
 */
interface ExpandableItems {

    companion object {
        const val MIN_LEVEL = 0
    }

    /**
     * Свернуть группу
     *
     * @param index Индекс элемента в развернутом списке
     * @param recursive Флаг свернутки вложенных групп
     */
    fun collapse(index: Int, recursive: Boolean = false)

    /**
     * Свернуть все группы до верхнего уровня
     *
     * @param recursive Флаг свернутки вложенных групп
     */
    fun collapseAll(recursive: Boolean = false)

    /**
     * Развернуть группу
     *
     * @param index Индекс элемента в развернутом списке
     * @param expansionLevel Уровень, до которого рекурсивно раскрываются вложенные группы
     */
    fun expand(index: Int, expansionLevel: Int = MIN_LEVEL)

    /**
     * Развернуть все группы
     *
     * @param expansionLevel Уровень, до которого рекурсивно раскрываются вложенные группы
     */
    fun expandAll(expansionLevel: Int = Int.MAX_VALUE)

    /**
     * Получить уровень вложенности группы в иерархическом списке
     *
     * @param index Индекс элемента в развернутом списке
     * @return Уровень вложенности группы в иерархическом списке
     */
    fun getExpansionLevel(index: Int): Int

    /**
     * Проверить, развернута ли группа
     *
     * @param index Индекс элемента в развернутом списке
     * @return True - если группа развернута
     */
    fun isExpanded(index: Int): Boolean

    /**
     * Установить признак свертки/развернутки группы
     *
     * @param index Индекс элемента в развернутом списке
     * @param value True - если группа развернута
     */
    fun setExpanded(index: Int, value: Boolean = true)
}