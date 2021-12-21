package bk.app.testapp

import android.view.View
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
        expandableList.collapse(expandableList.sourceIndex(index), recursive)
    }

    override fun collapseAll(recursive: Boolean) {
        expandableList.collapseAll(recursive)
    }

    override fun expand(index: Int, expansionLevel: Int) {
        expandableList.expand(expandableList.sourceIndex(index), expansionLevel)
    }

    override fun expandAll(expansionLevel: Int) {
        expandableList.expandAll(expansionLevel)
    }

    override fun getItemCount(): Int = expandableList.size

    fun getItem(index: Int): T = expandableList[index]

    fun submitList(newList: List<T>?, commitCallback: Runnable? = null) {
        expandableList.submitList(newList, commitCallback)
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

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