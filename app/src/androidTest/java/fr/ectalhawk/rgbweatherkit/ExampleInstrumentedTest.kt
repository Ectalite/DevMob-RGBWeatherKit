package fr.ectalhawk.rgbweatherkit

import android.content.Intent
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat.startActivity
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TodoDetailsActivityTest {

    @Test
    fun verifySeekBarX() {
        launchActivity<TestActivity>().use {
            val r = Random
            val progressX = r.nextInt(0,63)
            onView(withId(R.id.pixelXBar)).perform(setProgress(progressX))
            onView(withText("PixelX: $progressX")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun verifySeekBarY() {
        val r = Random
        val progressY = r.nextInt(0,31)
        onView(withId(R.id.pixelYBar)).perform(setProgress(progressY))
        onView(withText("PixelY: $progressY")).check(matches(isDisplayed()))
    }

    private fun setProgress(progress: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController?, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
            }

            override fun getDescription(): String {
                return "Set a progress on a SeekBar"
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(SeekBar::class.java)
            }
        }
    }
}