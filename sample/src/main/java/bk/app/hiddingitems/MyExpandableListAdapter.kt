package bk.app.hiddingitems

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import bk.app.hiddingitems.databinding.HolderLevel0Binding
import bk.app.hiddingitems.databinding.HolderLevel1Binding
import bk.app.hiddingitems.databinding.HolderLevel2Binding
import bk.hidingitems.ExpandableListAdapter

class MyExpandableListAdapter :
    ExpandableListAdapter<ExpandedListItem, MyExpandableListAdapter.ViewHolder>() {

    override fun getExpansionLevel(index: Int): Int = expandedItem(index).expansionLevel

    override fun isExpanded(index: Int): Boolean = expandedItem(index).isExpanded

    override fun setExpanded(index: Int, value: Boolean) {
        expandedItem(index).isExpanded = value
    }

    override fun getItemViewType(position: Int): Int = getItem(position).expansionLevel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.createHolder(parent, viewType, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    sealed class ViewHolder private constructor(binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun createHolder(
                parent: ViewGroup,
                viewType: Int,
                adapter: ExpandableListAdapter<*, *>
            ): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                return when (viewType) {
                    0 -> ViewHolderLevel0(HolderLevel0Binding.inflate(inflater, parent, false))
                    1 -> ViewHolderLevel1(HolderLevel1Binding.inflate(inflater, parent, false))
                    2 -> ViewHolderLevel2(HolderLevel2Binding.inflate(inflater, parent, false))
                    else -> throw IllegalArgumentException("Unknown view type $viewType")
                }.apply {
                    itemView.setOnClickListener {
                        val index = adapter.expandedIndex(this.adapterPosition)
                        Log.d(ViewHolder::class.java.name, "adapterPosition = ${this.adapterPosition}, raw index = $index")
                        if (adapter.isExpanded(index)) {
                            adapter.collapse(index)
                        } else {
                            adapter.expand(index)
                        }
                    }
                }
            }
        }

        abstract fun bind(item: ExpandedListItem)

        private class ViewHolderLevel0(private val binding: HolderLevel0Binding) :
            ViewHolder(binding) {
            override fun bind(item: ExpandedListItem) {
                binding.textView.text = item.text
            }
        }

        private class ViewHolderLevel1(private val binding: HolderLevel1Binding) :
            ViewHolder(binding) {
            override fun bind(item: ExpandedListItem) {
                binding.textView.text = item.text
            }
        }

        private class ViewHolderLevel2(val binding: HolderLevel2Binding) : ViewHolder(binding) {
            override fun bind(item: ExpandedListItem) {
                binding.textView.text = item.text
            }
        }
    }

}