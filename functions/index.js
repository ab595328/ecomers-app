const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");

initializeApp();

const razorpayKeyId = defineSecret("RAZORPAY_KEY_ID");
const razorpayKeySecret = defineSecret("RAZORPAY_KEY_SECRET");
const razorpayAccountNumber = defineSecret("RAZORPAYX_ACCOUNT_NUMBER");

function parseRegisteredBankDetails(value) {
  const match = String(value || "").trim().match(/^(\d+)\s*\(([A-Z0-9]{6,14})\)$/i);
  if (!match) throw new Error("Registered bank details must contain account number and IFSC.");
  return { accountNumber: match[1], ifsc: match[2].toUpperCase() };
}

async function razorpayRequest(path, body, idempotencyKey) {
  const auth = Buffer.from(`${razorpayKeyId.value()}:${razorpayKeySecret.value()}`).toString("base64");
  const response = await fetch(`https://api.razorpay.com/v1${path}`, {
    method: "POST",
    headers: {
      Authorization: `Basic ${auth}`,
      "Content-Type": "application/json",
      ...(idempotencyKey ? { "X-Payout-Idempotency": idempotencyKey } : {})
    },
    body: JSON.stringify(body)
  });
  const payload = await response.json();
  if (!response.ok) throw new Error(payload?.error?.description || `Razorpay request failed (${response.status}).`);
  return payload;
}

exports.processScheduledPayouts = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Kolkata",
    secrets: [razorpayKeyId, razorpayKeySecret, razorpayAccountNumber],
    retryCount: 3
  },
  async () => {
    const db = getFirestore();
    const now = Date.now();
    const snapshot = await db.collection("withdrawal_requests")
      .where("status", "==", "Scheduled")
      .where("scheduledAt", "<=", now)
      .limit(25)
      .get();

    for (const document of snapshot.docs) {
      const claimed = await db.runTransaction(async transaction => {
        const fresh = await transaction.get(document.ref);
        if (!fresh.exists || fresh.get("status") !== "Scheduled" || Number(fresh.get("scheduledAt")) > now) return false;
        transaction.update(document.ref, { status: "Processing", processingStartedAt: now });
        return true;
      });
      if (!claimed) continue;

      try {
        const request = document.data();
        const userDocument = await db.collection("users").doc(request.accountEmail).get();
        if (!userDocument.exists) throw new Error("Withdrawal account no longer exists.");
        const user = userDocument.data();
        const registeredBank = request.accountRole === "Seller"
          ? user.sellerBankAccount
          : user.deliveryBankAccount;
        if (!registeredBank || registeredBank !== request.bankDetails) {
          throw new Error("Registered bank details changed; submit a new withdrawal.");
        }
        const bank = parseRegisteredBankDetails(registeredBank);
        const contact = await razorpayRequest("/contacts", {
          name: user.name || request.accountEmail,
          email: request.accountEmail,
          contact: user.sellerMobile || user.deliveryMobile || user.phone || "",
          type: request.accountRole === "Seller" ? "vendor" : "employee",
          reference_id: request.accountEmail
        });
        const fundAccount = await razorpayRequest("/fund_accounts", {
          contact_id: contact.id,
          account_type: "bank_account",
          bank_account: {
            name: user.name || request.accountEmail,
            ifsc: bank.ifsc,
            account_number: bank.accountNumber
          }
        });
        const payout = await razorpayRequest("/payouts", {
          account_number: razorpayAccountNumber.value(),
          fund_account_id: fundAccount.id,
          amount: Math.round(Number(request.amount) * 100),
          currency: "INR",
          mode: "IMPS",
          purpose: "payout",
          queue_if_low_balance: true,
          reference_id: document.id,
          narration: "ZYL VOR payout"
        }, document.id);
        await document.ref.update({
          status: "Paid",
          payoutId: payout.id,
          paidAt: FieldValue.serverTimestamp(),
          failureReason: FieldValue.delete()
        });
      } catch (error) {
        const retryCount = Number(document.get("retryCount") || 0) + 1;
        logger.error("Scheduled payout failed", { withdrawalId: document.id, error: error.message, retryCount });
        await document.ref.update({
          status: retryCount >= 5 ? "Failed" : "Scheduled",
          retryCount,
          scheduledAt: Date.now() + 60 * 60 * 1000,
          failureReason: error.message
        });
      }
    }
  }
);
