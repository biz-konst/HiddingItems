package bk.hidingitems

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.ListUpdateCallback
import java.util.*

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Класс списка с возможностью скрытия/показа элементов
 */
@Suppress("MemberVisibilityCanBePrivate")
open class HidingList<T> private constructor(private val hidingItems: HidingItemsAdapter) :
    AbstractList<T>(), HidingItems by hidingItems {

    private var items = emptyList<T>()
    private var differ: AsyncListDiffer<T>? = null
    private val listeners = arrayListOf<AsyncListDiffer.ListListener<T>>()

    val currentList: List<T> get() = differ?.currentList ?: items

    /**
     * Конструктор класса
     *
     * @param listUpdateCallback Слушатель событий изменения списка
     *  (в т.ч. при скрытии/показе элементов)
     * @param asyncDiffConfig Конфигурация для DiffUtil
     * @param items Начальный список элементов
     * @param maxBucketSize Максимальный размер фрагмента скрытых диапазонов
     *  (подробнее в описани класса HidingItemsAdapter).
     */
    constructor(
        listUpdateCallback: ListUpdateCallback? = null,
        asyncDiffConfig: AsyncDifferConfig<T>? = null,
        items: List<T>? = null,
        maxBucketSize: Int = 1024
    ) : this(HidingItemsAdapter(maxBucketSize = maxBucketSize, listener = listUpdateCallback)) {
        asyncDiffConfig?.let { differ = AsyncListDiffer(hidingItems, it) }
        submitList(items)
    }

    // list

    /**
     * Получить элемент списка по индексу
     *
     * @param index Индекс элемента
     * @return Элемент списка
     */
    override fun get(index: Int): T = currentList[hidingItems.sourceIndex(index)]

    override val size: Int get() = hidingItems.targetPosition(currentList.size)

    // hiding items

    /**
     * Получить индекс элемента по позиции
     *
     * @param position Позиция элемента
     * @return Индекс элемента
     */
    fun sourceIndex(position: Int): Int = hidingItems.sourceIndex(position)

    /**
     * Получить позицию элемента по индексу
     *
     * @param index Индекс элемента
     * @return Позиция элемента, или значение HIDDEN_ITEM
     */
    fun targetPosition(index: Int): Int = hidingItems.targetPosition(index)

    // differ

    /**
     * Добавить слушателя событий изменения списка
     *
     * @param listener Слушатель событий изменения списка
     */
    fun addListListener(listener: AsyncListDiffer.ListListener<T>) {
        differ?.addListListener(listener) ?: run { listeners.add(listener) }
    }

    /**
     * Удалить слушателя событий изменения списка
     *
     * @param listener Слушатель событий изменения списка
     */
    fun removeListListener(listener: AsyncListDiffer.ListListener<T>) {
        differ?.removeListListener(listener) ?: run { listeners.remove(listener) }
    }

    /**
     * Установить новый список в качестве источника элементов
     *
     * @param newList Новый список элементов
     * @param commitCallback Обработчик завершения изменения списка
     */
    fun submitList(newList: List<T>?, commitCallback: Runnable? = null) {
        differ?.submitList(newList, commitCallback) ?: submitItems(newList, commitCallback)
    }

    /**
     * Установить внутренний список
     *
     * @param newList Новый список элементов
     * @param commitCallback Обработчик завершения изменения списка
     */
    private fun submitItems(newList: List<T>?, commitCallback: Runnable? = null) {
        if (items !== newList) {
            val oldList = items
            items = newList?.let { Collections.unmodifiableList(it) } ?: emptyList()
            if (oldList.isNotEmpty()) {
                hidingItems.onRemoved(0, oldList.size)
            }
            if (items.isNotEmpty()) {
                hidingItems.onInserted(0, items.size)
            }
            onCurrentListChanged(oldList)
            commitCallback?.run()
        }
    }

    /**
     * Оповестить слушателей об изменении списка
     *
     * @param previousList Предыдущий список
     */
    private fun onCurrentListChanged(previousList: List<T>) {
        listeners.forEach { it.onCurrentListChanged(previousList, currentList) }
    }

}