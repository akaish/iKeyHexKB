package ru.ikey.hexkb.rc

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import net.akaish.ikey.hkb.Util
import net.akaish.ikey.hkb.Util.Companion.hexOnly
import kotlin.math.min

class HexRecyclerEditor(
    private val recyclerView: RecyclerView
) {
    fun dataListener(positionId: Int, maxItemIndex: Int) = DataListener(positionId, maxItemIndex)

    inner class DataListener(
        private val id: Int,
        private val maxItemIndex: Int
    ) : Util.DataNotifier {

        override fun left() {
            if(id == 0)
                return
            val item = recyclerView.findViewHolderForAdapterPosition(id - 1)
            if(item is LineAdapter.ViewHolder) {
                item.line.requestFocus()
                item.line.setSelection(item.line.editableText?.length ?: 0)
            }
        }

        override fun right() {
            if(id == maxItemIndex)
                return
            val item = recyclerView.findViewHolderForAdapterPosition(id + 1)
            if(item is LineAdapter.ViewHolder) {
                item.line.requestFocus()
                item.line.setSelection(0)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onDataSizeChanged(diffSize: Int, diff: String) {
            if(diffSize == 0) {
                return
            }
            if(diffSize < 0 && id <= maxItemIndex) {
                val thisItem = recyclerView.findViewHolderForAdapterPosition(id)
                val nextItem = recyclerView.findViewHolderForAdapterPosition(id + 1)
                if(thisItem is LineAdapter.ViewHolder && nextItem is LineAdapter.ViewHolder) {
                    val thisText = thisItem.line.text?.toString()?.hexOnly() ?: ""
                    val nextText = nextItem.line.text?.toString()?.hexOnly() ?: ""
                    if(nextText.isEmpty())
                        return
                    val borrowed = nextText.substring(0, min(diffSize * -1, nextText.length))
                    val newNextText = nextText.substring(min(diffSize * -1, nextText.length))
                    thisItem.line.setText(thisText + borrowed)
                    nextItem.line.setText(newNextText)
                }
            } else if(id <= maxItemIndex) {
                val nextItem = recyclerView.findViewHolderForAdapterPosition(id + 1)
                if(nextItem is LineAdapter.ViewHolder) {
                    nextItem.line.skipPositionSetting = true
                    nextItem.line.editableText?.insert(0, diff)
                }
            }
            // TODO
            Log.e("111", "Chars updated: $id, $diffSize, $diff")
        }
    }
}