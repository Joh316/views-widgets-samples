/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.androidstudio.motionlayoutexample.histogramdemo

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.core.content.ContextCompat
import com.google.androidstudio.motionlayoutexample.R
import kotlinx.android.synthetic.main.histogram_layout.*
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class HistogramActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_COLOUR_ID = R.color.colorAccent
        private const val HISTOGRAM_BARS_RESTORE_KEY = "HISTOGRAM"
    }

    // List used for save and restoring data to the widget
    private var bars: ArrayList<HistogramBarMetaData> = ArrayList()

    // The main widget
    private var widget: HistogramWidget? = null

    // Animation guard
    private val animating = AtomicBoolean(false)
    private val animationListener: TransitionListener = object : TransitionListener {
        override fun onTransitionStarted(motionLayout: MotionLayout, startId: Int, endId: Int) {
            animating.set(true)
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
            animating.set(false)
        }

        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) { }
        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) { }
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.histogram_layout)
        widget = histogram
        restoreView(savedInstanceState)
        widget!!.setTransitionListener(animationListener)
    }

    /**
     * Add random data to the histogram.
     */
    fun onClickAdd(view: View?) {
        if (animating.get()) {
            return
        }
        add()
        widget!!.animateWidget()
    }

    fun onClickSort(view: View?) {
        if (animating.get()) {
            return
        }
        bars = widget!!.sort()
        widget!!.animateWidget()
    }

    fun onClickRandom(view: View) {
        if (animating.get()) {
            return
        }
        add()
        bars = widget!!.sort()
        widget!!.animateWidget()
    }

    private fun add() {
        val rand = Random()
        var barColour = ContextCompat.getColor(this, DEFAULT_COLOUR_ID)
        val barDataList = ArrayList<HistogramBarMetaData>(widget!!.barsSize)
        var name = 0
        for (id in widget!!.barIds) {
            val barData = HistogramBarMetaData(
                    id,
                    rand.nextFloat(),
                    barColour,
                    ColorHelper.getContrastColor(barColour),
                    name.toString())
            barColour = ColorHelper.getNextColor(barColour)
            barDataList.add(barData)
            name++
        }
        bars = barDataList
        widget!!.setData(barDataList)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (bars.isNotEmpty()) {
            outState.putParcelableArrayList(HISTOGRAM_BARS_RESTORE_KEY, bars)
        }
        super.onSaveInstanceState(outState)
    }

    private fun restoreView(savedInstance: Bundle?) {
        if (savedInstance == null) { // nothing to restore.
            return
        }
        bars = savedInstance.getParcelableArrayList(HISTOGRAM_BARS_RESTORE_KEY) ?: ArrayList()
        if (bars.isEmpty()) {
            return
        }

        widget!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Because we're creating all views in code, all the view ids are recreated.
                if (widget!!.barIds.size != bars.size) {
                    throw RuntimeException("Restoring array doesn't match the view size.")
                }

                bars = ArrayList(bars.mapIndexed{ i, metaData ->
                    HistogramBarMetaData(widget!!.barIds[i], metaData)
                })
                widget!!.setData(bars)
                widget!!.animateWidget()
                widget!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
}
