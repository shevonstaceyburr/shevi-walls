# Shevi Walls — Money-Making Android Project

Version 1.1.0 (version code 2). Application ID: `com.sheviwalls.app`.

## Included
- Free + Premium wallpaper model
- Google Play Billing Library 9.1.0 integration
- Monthly `shevi_premium_monthly` and yearly `shevi_premium_yearly` subscription IDs
- Purchase acknowledgement and entitlement restore
- Safer Premium entitlement handling for active Play purchases
- Favourites and private local diary
- Western + Chinese zodiac sections
- PWA manifest + service worker
- Android WebView wrapper and wallpaper saving
- App icons and all referenced local asset paths

## Current verification
- HTML/JavaScript syntax check: passed
- Manifest XML parse: passed
- PWA JSON parse: passed
- Referenced image-path check: passed; no missing referenced images
- Project structure and Billing configuration: checked

## What is not completed in this workspace
A signed `.aab` has **not** been produced here because this runtime has no Android SDK/Gradle build environment and no access to the developer's Google Play Console account or signing setup. It would be misleading to claim that the AAB has been built, uploaded, installed from Play, or purchase-tested.

## Next real-world release steps
1. Build the release AAB in a configured Android build environment.
2. Sign it with the upload key for the Play Console app.
3. Upload the AAB to Internal testing.
4. Create/configure the two Premium subscriptions and active base plans/offers.
5. Add a license tester and install from the Play internal-test link.
6. Test purchase, restore, renewal/state changes, declined and pending payments.
7. Fix any device/Play findings, increment version code, rebuild and retest.
8. Move through Closed testing and then Production according to the developer account's Play requirements.

The included `PLAY-CONSOLE-SETUP.md` is the runbook for the Play Console and billing test sequence.
