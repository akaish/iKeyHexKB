package ru.ikey.hexkb.rc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.akaish.ikey.hkb.NullableHexInputEditText
import net.akaish.ikey.hkb.IKeyHexKeyboard
import net.akaish.ikey.hkb.Util
import ru.ikey.hexkb.R

class LineAdapter(
    context: Context,
    linesCount: Int,
    private val charactersPerLine: Int,
    private val hexKeyboard: IKeyHexKeyboard,
    private val hexRecyclerEditor: HexRecyclerEditor,
    vararg lines: LineParam
) : RecyclerView.Adapter<LineAdapter.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val linesList: ArrayList<LineItem> = ArrayList()

    init {
        for(i in 0..linesCount) {
            linesList.add(
                LineItem(
                    id = i,
                    data = String(CharArray(charactersPerLine) { 'F' }),
                    dataListener = hexRecyclerEditor.dataListener(i, linesCount - 1)
                )
            )
        }
        lines.forEach {
            if(it.id in 0..linesCount) {
                require(it.data.length == charactersPerLine)
                val line = LineItem(
                    id = it.id,
                    data = it.data,
                    dataListener = hexRecyclerEditor.dataListener(it.id, linesCount - 1)
                )
                linesList[it.id] = line
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.line_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = linesList[position]
        holder.index.text = (position * charactersPerLine).toString(16).padStart(4, '0')
        hexKeyboard.registerInputs(holder.line)
        holder.line.setText(item.data)
        holder.line.setDataNotifier(item.dataListener)
    }

    override fun getItemCount(): Int = linesList.size



    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val index: TextView = view.findViewById(R.id.hkb_line_index)
        val line: NullableHexInputEditText = view.findViewById(R.id.hkb_line)
    }
}