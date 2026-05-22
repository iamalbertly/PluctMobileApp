# Tester Rollout

## Android testers

- Send `app-debug.apk` directly for manual testing, or use Google Play Internal Testing later.
- Android can be tested immediately once APK builds.

## iOS testers

- Use TestFlight.
- Dad, wife, and 10 testers should be added as external testers unless they are App Store Connect users.
- External TestFlight builds require Apple beta review before testers can install.
- Testers install Apple's TestFlight app, accept invite, install Pluct, then test.

Apple reality:

- TestFlight supports external testing up to 10,000 testers.
- External testers need beta review.
- Internal testers are team users in App Store Connect.
- Ad Hoc needs registered devices and uses device quotas.

Sources:
- Apple TestFlight: https://developer.apple.com/testflight
- Apple app distribution docs: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- Apple Xcode distribution methods: https://help.apple.com/xcode/mac/current/en.lproj/dev31de635e5.html

## Minimum tester list

1. Albert
2. Wife
3. Dad
4. Tester 4
5. Tester 5
6. Tester 6
7. Tester 7
8. Tester 8
9. Tester 9
10. Tester 10

## Test script

- Install app
- Open app
- Paste TikTok URL
- Tap TikTok link -> Text
- Confirm progress appears
- Confirm transcript appears
- Tap Copy
- Close app
- Reopen app
- Confirm previous transcript appears
- Try bad URL
- Confirm app blocks before charging/processing
