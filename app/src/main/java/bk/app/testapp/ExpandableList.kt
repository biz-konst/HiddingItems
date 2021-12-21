package bk.app.testapp

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Класс многоуровневого списка с поддержкой свертки/развертки групп
 *
 * @param listUpdateCallback Слушатель событий изменения списка
 * (в т.ч. при скрытии/показе элементов)
 * @param asyncDifferConfig Конфигурация для DiffUtil
 * @param items Начальный список элементов
 * @param maxBucketSize Максимальный размер фрагмента скрытых диапазонов
 * (подробнее в описани класса HidingItemsAdapter).
 */
abstract class ExpandableList<T>(
    listUpdateCallback: ListUpdateCallback? = null,
    asyncDifferConfig: AsyncDifferConfig<T>? = null,
    items: List<T>? = null,
    maxBucketSize: Int = 1024
) : HidingList<T>(listUpdateCallback, asyncDifferConfig, items, maxBucketSize), ExpandableItems {

    companion object {
        private const val MIN_LEVEL = 0
    }

    /**
     * Свернуть группу
     *
     * @param index Позиция элемента группы в развенутом списке
     * @param recursive Флаг свернутки вложенных групп
     */
    override fun collapse(index: Int, recursive: Boolean) {
        val level = getExpansionLevel(index)
        if (isExpanded(index) || recursive) {
            setExpanded(index, false)
            var i = index
            while (++i < currentList.size) {
                if (getExpansionLevel(i) <= level) {
                    break
                }
                if (recursive) {
                    setExpanded(i, false)
                }
            }
            val count = i - index - 1
            if (count > 0) {
                hide(index + 1, count)
            }
        }
    }

    /**
     * Свернуть все группы до верхнего уровня
     *
     * @param recursive Флаг свернутки вложенных групп
     */
    override fun collapseAll(recursive: Boolean) {
        // все делаем на "виртуальном" списке
        for (i in lastIndex downTo 0) {
            if (getExpansionLevel(i) == MIN_LEVEL) {
                collapse(sourceIndex(i), recursive)
            }
        }
    }

    /**
     * Развернуть группу
     *
     * @param index Позиция элемента группы в развернутом списке
     * @param expansionLevel Уровень, до которого рекурсивно раскрываются вложенные группы
     */
    override fun expand(index: Int, expansionLevel: Int) {
        var firstShown = 0
        var lastShown = 0

        fun expandChildren(index: Int, parentLevel: Int, parentExpanded: Boolean): Int {
            var i = index
            if (!parentExpanded) {
                // если родитель не раскрыт, пропускаем все его элементы
                while (i < currentList.size) {
                    if (getExpansionLevel(i) <= parentLevel) {
                        break
                    }
                    i++
                }
                if (i > index) {
                    // что-то пропустили, значит нужно показать накопленное...
                    if (firstShown < lastShown) {
                        show(firstShown, lastShown - firstShown - 1)
                    }
                    // ...и начать заново
                    firstShown = i
                }
            } else {
                while (i < currentList.size) {
                    val level = getExpansionLevel(i)
                    if (level <= parentLevel) {
                        break
                    }
                    if (level < expansionLevel) {
                        setExpanded(i, true)
                    }
                    val isExpanded = isExpanded(i)
                    lastShown = ++i
                    i = expandChildren(i, level, isExpanded)
                }
            }
            return i
        }

        var level = getExpansionLevel(index)
        // нужно открыть родительские группы
        var i = index
        while (--i >= 0 && level > MIN_LEVEL) {
            if (getExpansionLevel(i) < level) {
                if (!isExpanded(i)) {
                    setExpanded(index, true)
                    expand(i, MIN_LEVEL)
                    break
                }
                level = getExpansionLevel(i)
            }
        }
        if (!isExpanded(index) || expansionLevel > getExpansionLevel(index) + 1) {
            setExpanded(index, true)
            firstShown = index + 1
            lastShown = firstShown
            expandChildren(index + 1, getExpansionLevel(index), true)
            if (firstShown < lastShown) {
                show(firstShown, lastShown - firstShown - 1)
            }
        }
    }

    /**
     * Развернуть все группы
     *
     * @param expansionLevel Уровень, до которого рекурсивно раскрываются вложенные группы
     */
    override fun expandAll(expansionLevel: Int) {
        // все делаем на "виртуальном" списке
        for (i in lastIndex downTo 0) {
            if (getExpansionLevel(i) == MIN_LEVEL) {
                expand(sourceIndex(i), expansionLevel)
            }
        }
    }

}