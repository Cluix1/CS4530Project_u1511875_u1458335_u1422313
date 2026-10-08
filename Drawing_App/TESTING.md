# Testing

Run local unit tests from Android Studio using DrawingViewModelTest, or run
`./gradlew testDebugUnitTest` from this directory (Windows: `./gradlew.bat testDebugUnitTest`).
An Android SDK must be configured first.

The unit tests cover tool/color selection, the upper brush-size limit, and clearing
a drawing while keeping the pen settings and allowing new strokes afterward.

Manual checks for clearing:

1. Open the drawing screen. Clear should be disabled on an empty canvas.
2. Draw several strokes. Clear should become enabled.
3. Select a color and size, then press Clear. All strokes should disappear and
   Clear should become disabled. The color and size should stay selected.
4. Draw again and check that the selected color and size are used.
5. Rotate the device after clearing and check that the drawing remains empty.

UI interactions, rotation, splash timing, rendering, and phone/tablet layouts are
not covered by the local unit tests. These require emulator or device checks.
The non-pen tool buttons currently change selection only; their separate drawing
behaviors are not implemented yet.
