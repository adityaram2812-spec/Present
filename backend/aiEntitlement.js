import { getFirestore } from 'firebase-admin/firestore';

const DEFAULT_LIMIT = 2;

export async function getAiEntitlement(uid) {
    const db = getFirestore();
    const userRef = db.collection('users').doc(uid);

    return await db.runTransaction(async (transaction) => {
        const doc = await transaction.get(userRef);

        let used = 0;
        let limit = DEFAULT_LIMIT;

        if (doc.exists) {
            const data = doc.data();
            let needsUpdate = false;
            let updateData = {};

            if (typeof data.freeAiImportsUsed === 'number') {
                used = data.freeAiImportsUsed;
            } else {
                needsUpdate = true;
                updateData.freeAiImportsUsed = used;
            }

            if (typeof data.freeAiImportsLimit === 'number') {
                limit = data.freeAiImportsLimit;
            } else {
                needsUpdate = true;
                updateData.freeAiImportsLimit = limit;
            }

            if (needsUpdate) {
                // Merge safely without affecting other fields like createdAt
                transaction.set(userRef, updateData, { merge: true });
            }
        } else {
            // User doesn't exist yet, initialize Safely
            transaction.set(userRef, {
                uid: uid,
                freeAiImportsUsed: used,
                freeAiImportsLimit: limit
            }, { merge: true });
        }

        const remaining = Math.max(0, limit - used);

        return { used, limit, remaining };
    });
}

export async function reserveAiImport(uid) {
    const db = getFirestore();
    const userRef = db.collection('users').doc(uid);

    return await db.runTransaction(async (transaction) => {
        const doc = await transaction.get(userRef);

        let used = 0;
        let limit = DEFAULT_LIMIT;
        let isPremium = false;
        let documentExists = doc.exists;

        if (documentExists) {
            const data = doc.data();
            if (data.isPremium === true) {
                isPremium = true;
            }
            if (typeof data.freeAiImportsUsed === 'number') {
                used = data.freeAiImportsUsed;
            }
            if (typeof data.freeAiImportsLimit === 'number') {
                limit = data.freeAiImportsLimit;
            }
        }

        if (isPremium) {
            return {
                reserved: true,
                isPremium: true,
                used,
                limit,
                remaining: 'unlimited'
            };
        }

        let remaining = Math.max(0, limit - used);

        if (used >= limit) {
            return {
                reserved: false,
                isPremium: false,
                used,
                limit,
                remaining
            };
        }

        used += 1;
        remaining = Math.max(0, limit - used);

        const updateData = {
            freeAiImportsUsed: used,
            freeAiImportsLimit: limit
        };

        if (documentExists) {
            transaction.set(userRef, updateData, { merge: true });
        } else {
            transaction.set(userRef, {
                uid: uid,
                ...updateData
            }, { merge: true });
        }

        return {
            reserved: true,
            isPremium: false,
            used,
            limit,
            remaining
        };
    });
}

export async function refundAiImport(uid) {
    const db = getFirestore();
    const userRef = db.collection('users').doc(uid);

    return await db.runTransaction(async (transaction) => {
        const doc = await transaction.get(userRef);
        if (!doc.exists) return; // Should not fail arbitrarily

        const data = doc.data();
        if (data.isPremium === true) return;

        let used = data.freeAiImportsUsed || 0;

        if (used > 0) {
            used -= 1;
        }

        transaction.set(userRef, {
            freeAiImportsUsed: used
        }, { merge: true });
    });
}
