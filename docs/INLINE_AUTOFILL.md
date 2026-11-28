# Inline Autofill integration

Zero Keyboard requests provider-rendered inline suggestions on Android 11 and newer. The IME receives opaque views inflated by Android rather than credential text. Results are generation-guarded so callbacks from an old field cannot populate a new editor, and every input lifecycle transition clears the strip.

The setup toggle controls the feature independently. Android or the active Autofill provider can fall back to its normal UI when inline suggestions are unavailable. Password-manager compatibility requires real provider/device testing and is not verified by compilation alone.
