package bk.app.hiddingitems

data class ExpandedListItem(
    var isExpanded: Boolean = true,
    val expansionLevel: Int = 0,
    val text: String
)