MineServe Human Brand Asset Pipeline

Drop final human-designed brand assets into these centralized slots:
1. designsystem/src/commonMain/composeResources/drawable/brand_icon_512.png
   - 512x512 PNG used across Android UI, Desktop window/tray icon, Linux desktop packages, and embedded Web Admin (/logo.png).
2. designsystem/src/commonMain/composeResources/files/brand_logo.svg
   - Master vector logo (.svg) for scalable brand usage.
3. app/src/main/res/drawable/ic_launcher_monochrome.xml (or PNG)
   - Android 13+ adaptive icon <monochrome> layer referenced by mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml.
4. app/src/main/res/mipmap-*/ic_launcher_foreground.png (432x432 xxxhdpi safe-zone)
   - Android adaptive foreground layer.
