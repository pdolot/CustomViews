package com.dolotdev.customviews

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.dolotdev.customviewslib.swipeLayout.SwipeLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val list = listOf<Any>(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true)
    private val adapter by lazy { Adapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = this@MainActivity.adapter
        }



        adapter.setData(list)
    }
}
