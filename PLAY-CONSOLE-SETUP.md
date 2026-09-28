# Shevi Walls — Google Play setup and test runbook

## App identity
- App name: **Shevi Walls**
- Package: `com.sheviwalls.app`
- Version: `1.1.0` / version code `2`
- Target/compile SDK: `36`
- Billing Library: `9.1.0`

## Premium subscriptions
Create two **subscriptions** in Play Console:
- `shevi_premium_monthly` — £1.99/month suggested price
- `shevi_premium_yearly` — £14.99/year suggested price

Each subscription needs an active base plan/offer before the app can launch its checkout flow. The app now reports a clear message if a product or offer has not been configured.

## Test order
1. Upload the signed release AAB to **Internal testing**.
2. Add the Google account used on the test phone as a Play **license tester**.
3. Install the app from the Google Play internal-test link.
4. Test Monthly purchase with the Google Play test payment method.
5. Confirm Premium unlocks and the purchase is acknowledged.
6. Force-stop/reopen the app and use **Restore Premium**; Premium should return.
7. Test the Yearly product.
8. Test a declined payment and a pending/slow test payment.
9. Test subscription state changes/renewal with Play Billing Lab.
10. Check that an expired/revoked subscription no longer remains unlocked after restore.

Google recommends license testers and Play Billing Lab for subscription testing. Test-track purchases by non-license-testers can involve real charges, so use a license tester for the normal billing QA cycle.

## Release path
Internal test → fix issues → Closed test → satisfy any account-specific production-access testing requirements → Production.

For newer personal Play developer accounts, Google currently requires a closed test with at least 12 opted-in testers continuously for 14 days before production access can be requested.

## Still account/device dependent
A Play Console upload, real Play installation, purchase, and production submission require access to the developer's Google Play Console account and a real Android device. Those actions cannot be truthfully marked complete from this source workspace alone.
