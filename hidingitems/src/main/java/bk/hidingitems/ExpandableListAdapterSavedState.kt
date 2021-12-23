package bk.hidingitems

import android.os.Parcel
import android.os.Parcelable

/**
 * Класс сохранения/восстановления состояния групп элементов в иерархическом списке
 */
class ExpandableListAdapterSavedState private constructor() : Parcelable {

    private var adapter: ExpandableListAdapter<*, *>? = null
    private var parcel: Parcel? = null
    private var offset: Int = 0

    constructor(adapter: ExpandableListAdapter<*, *>) : this() {
        this.adapter = adapter
    }

    private constructor(parcel: Parcel) : this() {
        this.parcel = parcel
        this.offset = parcel.dataPosition()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        adapter?.apply {
            var level = ExpandableItems.MIN_LEVEL
            for (i in currentList.lastIndex downTo 0) {
                with(getExpansionLevel(i)) {
                    if (this < level) {
                        if (isExpanded(i)) {
                            parcel.writeInt(i)
                        }
                    } else if (this > level) {
                        level = this
                    }
                }
            }
            parcel.writeInt(-1)
        }
    }

    override fun describeContents() = 0

    fun <T> restore(adapter: ExpandableListAdapter<T, *>) {
        parcel?.let {
            val savePos = it.dataPosition()
            it.setDataPosition(offset)
            var index = it.readInt()
            while (index > -1) {
                adapter.setExpanded(index, true)
                index = it.readInt()
            }
            it.setDataPosition(savePos)
            adapter.updateExpandedState()
        }
    }

    companion object CREATOR : Parcelable.Creator<ExpandableListAdapterSavedState> {
        override fun createFromParcel(parcel: Parcel): ExpandableListAdapterSavedState {
            return ExpandableListAdapterSavedState(parcel)
        }

        override fun newArray(size: Int): Array<ExpandableListAdapterSavedState?> {
            return arrayOfNulls(size)
        }
    }
}