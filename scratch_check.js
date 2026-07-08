import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs } from 'firebase/firestore';
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyBEKTyo-jdxjloYHg7W_3oASx_UWPC54Yo",
  authDomain: "zyl-vor-bazar-fed58.firebaseapp.com",
  projectId: "zyl-vor-bazar-fed58",
  storageBucket: "zyl-vor-bazar-fed58.firebasestorage.app",
  messagingSenderId: "264981865474",
  appId: "1:264981865474:web:661d232efe58a353a5331b"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

async function run() {
  try {
    console.log("Authenticating with Firebase Auth...");
    await signInWithEmailAndPassword(auth, "admin@bazaar.com", "admin123");
    console.log("Authenticated successfully!");

    console.log("Fetching users from Firestore...");
    const snap = await getDocs(collection(db, "users"));
    console.log(`Total users found: ${snap.size}`);
    snap.forEach(doc => {
      const u = doc.data();
      console.log(`- Email: ${doc.id}, Name: ${u.name}, Role: ${u.role}, Verified: ${u.isSellerVerified}`);
    });
  } catch (e) {
    console.error("Error:", e);
  }
}

run();
