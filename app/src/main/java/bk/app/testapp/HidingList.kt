package bk.app.testapp

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
open class HidingList<T> internal constructor(private val hidingItems: HidingItemsAdapter) :
    AbstractList<T>(), HidingItems by hidingItems {

    private var items = emptyList<T>()
    private var differ: AsyncListDiffer<T>? = null
    private val list: List<T> get() = differ?.currentList ?: items

    constructor(
        listUpdateCallback: ListUpdateCallback? = null,
        diffCallback: DiffUtil.ItemCallback<T>? = null,
        items: List<T>? = null,
        maxBucketSize: Int = 1024
    ) : this(HidingItemsAdapter(maxBucketSize = maxBucketSize, listener = listUpdateCallback)) {
        if (diffCallback != null) {
            differ =
                AsyncListDiffer<T>(hidingItems, AsyncDifferConfig.Builder(diffCallback).build())
        }
        submitList(items)
    }

    // list

    override fun get(index: Int): T = list[hidingItems.indexByPosition(index)]

    override val size: Int get() = hidingItems.indexByPosition(list.size)

    // differ

    /**
     * Установить новый список в качестве источника элементов
     *
     * @param newList Новый список элементов
     * @param commitCallback Обработчик завершения изменения списка
     */
    fun submitList(newList: List<T>?, commitCallback: Runnable? = null) {
        differ?.let {
            items = emptyList()
            it.submitList(newList, commitCallback)
        } ?: run {
            val oldCount = items.size
            items = newList?.let { Collections.unmodifiableList(it) } ?: emptyList()
            if (oldCount > 0) {
                hidingItems.onRemoved(0, oldCount)
            }
            if (items.isNotEmpty()) {
                hidingItems.onInserted(0, items.size)
            }
            commitCallback?.run()
        }
    }

}