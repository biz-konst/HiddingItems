package bk.hidingitems

import androidx.recyclerview.widget.*

/**
 * @author Bizyur Konstantin <bkonst2180@gmail.com>
 * @since 21.12.2021
 *
 * Базовый адаптер для представления раскрывающихся списков в RecyclerView
 */
abstract class ExpandableListAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>? = null,
    maxBucketSize: Int = 1024
) : RecyclerView.Adapter<VH>(), ExpandableItems, AsyncListDiffer.ListListener<T> {

    private val expandableList = ExpandableListImpl(diffCallback, maxBucketSize)
        .also { it.addListListener(this) }

    val currentList by expandableList::currentList

    override fun collapse(index: Int, recursive: Boolean) {
        expandableList.collapse(index, recursive)
    }

    override fun collapseAll(recursive: Boolean) {
        expandableList.collapseAll(recursive)
    }

    override fun expand(index: Int, expansionLevel: Int) {
        expandableList.expand(index, expansionLevel)
    }

    override fun expandAll(expansionLevel: Int) {
        expandableList.expandAll(expansionLevel)
    }

    override fun onCurrentListChanged(previousList: List<T>, currentList: List<T>) {
        updateExpandedState()
    }

    override fun getItemCount(): Int = expandableList.size

    /**
     * Получить индекс элемента в развернутом списке
     *
     * @param position Видимая позиция элемента
     * @return Индекс элемента в развернутом списке
     */
    fun expandedIndex(position: Int): Int = expandableList.sourceIndex(position)

    /**
     * Получить элемент по его индексу в развернутом списке
     *
     * @param index Индекс элемента в развернутом списке
     * @return Элемент
     */
    fun expandedItem(index: Int): T = currentList[index]

    /**
     * Получить элемент по видимой позиции
     *
     * @param position Видимая позиция элемента
     * @return Элемент
     */
    fun getItem(position: Int): T = expandableList[position]

    /**
     * Установть новый список элементов
     *
     * @param newList Новый список элементов
     * @param commitCallback Обработчик завершения обновления списка
     */
    fun submitList(newList: List<T>?, commitCallback: Runnable? = null) {
        expandableList.submitList(newList, commitCallback)
    }

    /**
     * Установить видимость элементов в соответствие флагам isExpanded
     */
    fun updateExpandedState() {
        expandableList.updateExpanded()
    }

    private inner class ExpandableListImpl<T>(
        diffCallback: DiffUtil.ItemCallback<T>?,
        maxBucketSize: Int
    ) : ExpandableList<T>(
        listUpdateCallback = AdapterListUpdateCallback(this@ExpandableListAdapter),
        asyncDifferConfig = diffCallback?.let { AsyncDifferConfig.Builder(it).build() },
        maxBucketSize = maxBucketSize
    ) {
        override fun getExpansionLevel(index: Int) =
            this@ExpandableListAdapter.getExpansionLevel(index)

        override fun isExpanded(index: Int) =
            this@ExpandableListAdapter.isExpanded(index)

        override fun setExpanded(index: Int, value: Boolean) {
            this@ExpandableListAdapter.setExpanded(index, value)
        }
    }

}