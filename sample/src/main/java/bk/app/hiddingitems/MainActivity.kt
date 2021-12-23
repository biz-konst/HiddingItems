package bk.app.hiddingitems

import android.os.Bundle
import android.os.Parcel
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import bk.app.hiddingitems.databinding.ActivityMainBinding
import bk.hidingitems.ExpandableItems
import bk.hidingitems.ExpandableListAdapterSavedState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MyExpandableListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MyExpandableListAdapter()
        binding.itemsList.adapter = adapter
        adapter.submitList(items)
        savedInstanceState?.getParcelable<ExpandableListAdapterSavedState>(ADAPTER_SAVED_STATE_KEY)
            ?.restore(adapter)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ADAPTER_SAVED_STATE_KEY, ExpandableListAdapterSavedState(adapter))
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.collapseAll -> adapter.collapseAll()
            R.id.collapseAllRecursive -> adapter.collapseAll(recursive = true)
            R.id.expandAll -> adapter.expandAll(expansionLevel = ExpandableItems.MIN_LEVEL)
            R.id.expandAllRecursive -> adapter.expandAll()
        }
        return super.onContextItemSelected(item)
    }

    companion object {
        private const val ADAPTER_SAVED_STATE_KEY = "MyExpandableListAdapter.state"

        val items = listOf(
            ExpandedListItem(isExpanded = false, expansionLevel = 0, text = "Holder 0"),
            ExpandedListItem(isExpanded = false, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 0, text = "Holder 0"),
            ExpandedListItem(isExpanded = false, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = false, expansionLevel = 2, text = "Holder 2"),
        )
    }

}