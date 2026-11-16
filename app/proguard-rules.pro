# App-specific R8 rules.
#
# Readium, Hilt, Room, Coil, and AndroidX ship their own consumer rules. Avoid broad
# keeps here; they would hide real shrinking problems and inflate the release APK.
-keepattributes *Annotation*
