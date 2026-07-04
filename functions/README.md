# Automatic RazorpayX payouts

Set the required Firebase Functions secrets, then deploy:

```sh
firebase functions:secrets:set RAZORPAY_KEY_ID
firebase functions:secrets:set RAZORPAY_KEY_SECRET
firebase functions:secrets:set RAZORPAYX_ACCOUNT_NUMBER
firebase deploy --only functions
```

The scheduled worker runs every 15 minutes. The admin panel controls each request's delay through `app_config/main.payoutDelayHours`.
