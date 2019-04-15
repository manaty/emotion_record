package org.manaty.octopus.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_status.view.*
import org.manaty.octopus.R

class StatusButton(context : Context, attr: AttributeSet) : LinearLayout(context, attr){
    enum class State{
        ENABLED,
        DISABLED
    }
//    constructor(context: Context?) : super(context)
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
//        this.attrs = attrs
//    }
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.view_status, this)

        val attributes = context.obtainStyledAttributes(attr, R.styleable.StatusButton)
        imageview_icon.setImageDrawable(attributes.getDrawable(R.styleable.StatusButton_icon))
        attributes.recycle()

    }

    fun setState(state : State){
        when(state){
            State.ENABLED -> imageview_status.visibility = View.GONE
            State.DISABLED -> imageview_status.visibility = View.VISIBLE
        }
    }

    fun setLogo(drawableId : Int){
        imageview_icon.setImageResource(drawableId)
    }
}
