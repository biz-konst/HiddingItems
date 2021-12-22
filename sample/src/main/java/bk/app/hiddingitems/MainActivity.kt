package bk.app.hiddingitems

import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import bk.app.hiddingitems.databinding.ActivityMainBinding
import bk.hidingitems.ExpandableItems

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
        val items = listOf(
            ExpandedListItem(isExpanded = true, expansionLevel = 0, text = "Holder 0"),
            ExpandedListItem(isExpanded = true, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 0, text = "Holder 0"),
            ExpandedListItem(isExpanded = true, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 1, text = "Holder 1"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
            ExpandedListItem(isExpanded = true, expansionLevel = 2, text = "Holder 2"),
        )
    }

}