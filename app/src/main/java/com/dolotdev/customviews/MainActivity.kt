package com.dolotdev.customviews

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeLayout.setOnSwipeListener(
            onSwipe = { progress, direction ->

            },
            onSwiped = {

            }
        )

        reset.setOnClickListener {
            swipeLayout.reset(true)
        }
    }
}
