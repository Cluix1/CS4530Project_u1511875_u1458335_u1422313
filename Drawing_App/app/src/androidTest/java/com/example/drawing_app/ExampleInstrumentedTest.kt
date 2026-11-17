/**
 * ExampleInstrumentedTest.kt
 *
 * Instrumented test verifying basic app package context execution on Android device/emulator.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.drawing_app", appContext.packageName)
    }
}