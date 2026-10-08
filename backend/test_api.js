import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import dotenv from 'dotenv';

dotenv.config();

async function run() {
    initializeApp({
        credential: applicationDefault(),
        projectId: 'present-backend-508517'
    });

    const db = getFirestore();
    const snapshot = await db.collection('users').limit(1).get();

    if (snapshot.empty) {
        console.log("No users found");
        process.exit(1);
    }

    const realUserUid = snapshot.docs[0].id;
    console.log("Found real user UID:", realUserUid);

    // Generate custom token
    const customToken = await getAuth().createCustomToken(realUserUid);

    const WEB_API_KEY = "AIzaSyDrBhocHdYShoUnY3l3aHcF7-uarFhT1xA"; // from google-services.json

    // Exchange custom token for ID token
    const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${WEB_API_KEY}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            token: customToken,
            returnSecureToken: true
        })
    });

    const authData = await response.json();
    if (!authData.idToken) {
        console.error("Failed to get ID token:", authData);
        process.exit(1);
    }

    const idToken = authData.idToken;
    console.log("Successfully got ID token.");

    // Now test the local endpoints
    // Ensure the backend server is running in another terminal
    const baseUrl = 'http://localhost:8080/api/auth/entitlement';

    console.log("\n--- TEST C (GET Entitlement) ---");
    let res = await fetch(baseUrl, {
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    console.log(res.status, await res.json());

    console.log("\n--- TEST D (POST consume once) ---");
    res = await fetch(`${baseUrl}/test-consume`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    console.log(res.status, await res.json());

    console.log("\n--- TEST E (POST consume second time) ---");
    res = await fetch(`${baseUrl}/test-consume`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    console.log(res.status, await res.json());

    console.log("\n--- TEST F (POST consume third time - should fail 403) ---");
    res = await fetch(`${baseUrl}/test-consume`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    console.log(res.status, await res.json());

    console.log("\n--- TEST G (GET Entitlement again) ---");
    res = await fetch(baseUrl, {
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    console.log(res.status, await res.json());

    process.exit(0);
}

run().catch(console.error);
