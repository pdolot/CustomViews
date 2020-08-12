package com.dolotdev.customviewslib.extension

import android.view.View
import androidx.constraintlayout.widget.ConstraintSet

fun View.constraintTopToTopOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
}

fun View.constraintBottomToBottomOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
}

fun View.constraintStartToStartOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
}

fun View.constraintEndToEndOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
}

fun View.constraintLeftToLeftOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
}

fun View.constraintRightToRightOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
}

fun View.constraintTopToBottomOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
}

fun View.constraintBottomToTopOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
}

fun View.constraintStartToEndOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END)
}

fun View.constraintEndToStartOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START)
}

fun View.constraintLeftToRightOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
}

fun View.constraintRightToLeftOfParent(set: ConstraintSet){
    set.connect(this.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
}


fun View.constraintTopToTopOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.TOP, view.id, ConstraintSet.TOP)
}

fun View.constraintBottomToBottomOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.BOTTOM, view.id, ConstraintSet.BOTTOM)
}

fun View.constraintStartToStartOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.START, view.id, ConstraintSet.START)
}

fun View.constraintEndToEndOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.END, view.id, ConstraintSet.END)
}

fun View.constraintLeftToLeftOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.LEFT, view.id, ConstraintSet.LEFT)
}

fun View.constraintRightToRightOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.RIGHT, view.id, ConstraintSet.RIGHT)
}

fun View.constraintTopToBottomOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.TOP, view.id, ConstraintSet.BOTTOM)
}

fun View.constraintBottomToTopOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.BOTTOM, view.id, ConstraintSet.TOP)
}

fun View.constraintStartToEndOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.START, view.id, ConstraintSet.END)
}

fun View.constraintEndToStartOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.END, view.id, ConstraintSet.START)
}

fun View.constraintLeftToRightOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.LEFT, view.id, ConstraintSet.RIGHT)
}

fun View.constraintRightToLeftOf(set: ConstraintSet, view: View){
    set.connect(this.id, ConstraintSet.RIGHT, view.id, ConstraintSet.LEFT)
}
