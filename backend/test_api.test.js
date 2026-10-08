import { jest } from '@jest/globals';
import request from 'supertest';
import { Buffer } from 'node:buffer';

let firestoreDB = {};

// Accurate emulated Firestore Transaction Lock ensuring atomic concurrency safety
let transactionLock = false;
async function acquireLock() {
    return new Promise(resolve => {
        const attempt = () => {
            if (!transactionLock) {
                transactionLock = true;
                resolve();
            } else {
                setTimeout(attempt, 5);
            }
        };
        attempt();
    });
}
function releaseLock() { transactionLock = false; }

const mockTransaction = {
    get: async (ref) => {
        const data = firestoreDB[ref.id];
        return {
            exists: !!data,
            data: () => data
        }
    },
    set: (ref, data, options) => {
        const existing = firestoreDB[ref.id] || {};
        if (options && options.merge) {
            firestoreDB[ref.id] = { ...existing, ...data };
        } else {
            firestoreDB[ref.id] = { ...data };
        }
    }
};

jest.unstable_mockModule('firebase-admin/app', () => ({
    initializeApp: jest.fn(),
    cert: jest.fn(() => ({}))
}));

jest.unstable_mockModule('firebase-admin/auth', () => ({
    getAuth: jest.fn(() => ({
        verifyIdToken: async (token) => {
            if (token === 'invalid_token') throw new Error('Invalid token');

            // Extract the user definition dynamically based on token keys 
            switch (token) {
                case 'valid_free_user': return { uid: 'free123' };
                case 'valid_premium_user': return { uid: 'premium123' };
                case 'user_gemini_fail': return { uid: 'fail123' };
                case 'user_gemini_malf': return { uid: 'malf123' };
                case 'concurrent_user': return { uid: 'conc123' };
            }
            throw new Error('Fallback block error');
        }
    }))
}));

jest.unstable_mockModule('firebase-admin/firestore', () => ({
    getFirestore: () => ({
        collection: (col) => ({
            doc: (id) => ({ id })
        }),
        runTransaction: async (cb) => {
            await acquireLock();
            try {
                // Must evaluate callback resolution awaiting internally
                return await cb(mockTransaction);
            } finally {
                releaseLock();
            }
        }
    })
}));

let geminiBehavior = 'success';
jest.unstable_mockModule('@google/genai', () => ({
    GoogleGenAI: class {
        constructor() {
            this.models = {
                generateContent: async () => {
                    if (geminiBehavior === 'timeout') {
                        await new Promise(r => setTimeout(r, 200));
                    } else {
                        await new Promise(r => setTimeout(r, 10));
                    }

                    if (geminiBehavior === 'fail') {
                        const err = new Error('503 Service Unavailable');
                        err.status = 503;
                        throw err;
                    }
                    if (geminiBehavior === 'malformed') {
                        return { text: "this is definitively not json {}" };
                    }

                    return {
                        text: JSON.stringify({
                            division: "A",
                            groups_found: ["A"],
                            classes: [
                                {
                                    day: "Monday",
                                    startTime: "09:00",
                                    endTime: "10:00",
                                    subject: "CM",
                                    teacher: "TN",
                                    room: "311",
                                    group: "shared"
                                }
                            ]
                        })
                    };
                }
            }
        }
    }
}));

// Provide the strictly required environment variable before the ES Module initializes Native Firebase locally
jest.unstable_mockModule('firebase-admin/app-check', () => ({
    getAppCheck: jest.fn(() => ({
        verifyToken: async (token) => {
            if (token === 'valid_appcheck_token') return { appId: 'com.adityaram.present' };
            throw new Error('Invalid App Check token');
        }
    }))
}));

process.env.FIREBASE_SERVICE_ACCOUNT_JSON = JSON.stringify({
    type: "service_account",
    project_id: "present-backend-508517"
});

const { app } = await import('./index.js');
const dummyImage = Buffer.from('fake-image-bytes');

describe('Present AI Extraction Backend API Requirements', () => {

    beforeEach(() => {
        // Reset state completely per standard test mechanics boundaries
        geminiBehavior = 'success';
        firestoreDB = {};

        // Setup premium mapping initially
        firestoreDB['premium123'] = { isPremium: true };
    });

    test('1. unauthenticated request -> 401', async () => {
        const res = await request(app).post('/api/extract-timetable').attach('image', dummyImage, 'test.jpg');
        expect(res.status).toBe(401);
    });

    test('2. invalid Firebase token -> 401', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer invalid_token')
            .attach('image', dummyImage, 'test.jpg');
        expect(res.status).toBe(401);
    });

    test('3 & 4. new free user init & first successful import -> used = 1', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200);
        expect(firestoreDB['free123'].freeAiImportsUsed).toBe(1);
    });

    test('5. free user second successful import -> used = 2', async () => {
        firestoreDB['free123'] = { freeAiImportsUsed: 1, freeAiImportsLimit: 2 };
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200);
        expect(firestoreDB['free123'].freeAiImportsUsed).toBe(2);
    });

    test('6. third free import -> rejected 403', async () => {
        firestoreDB['free123'] = { freeAiImportsUsed: 2, freeAiImportsLimit: 2 };
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(403);
        expect(res.body.error).toBe('AI_IMPORT_LIMIT_REACHED');
        expect(firestoreDB['free123'].freeAiImportsUsed).toBe(2);
    });

    test('7. Premium user -> unlimited, quota not incremented', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_premium_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200);
        // Quota values remain uninitialized logically since unlimited bypass intercepted natively
        expect(firestoreDB['premium123'].freeAiImportsUsed).toBeUndefined();
    });

    test('8. Gemini failure after reservation -> quota refunded (503 status)', async () => {
        firestoreDB['fail123'] = { freeAiImportsUsed: 1, freeAiImportsLimit: 2 };
        geminiBehavior = 'fail';

        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer user_gemini_fail')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(503);
        expect(res.body.error).toBe('AI_EXTRACTION_FAILED');
        // Originally 1 -> reserved to 2 -> refunded safely back to 1
        expect(firestoreDB['fail123'].freeAiImportsUsed).toBe(1);
    });

    test('9. malformed Gemini response -> quota refunded (500 status)', async () => {
        firestoreDB['malf123'] = { freeAiImportsUsed: 0, freeAiImportsLimit: 2 };
        geminiBehavior = 'malformed';

        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer user_gemini_malf')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(500);
        // Original 0 -> reserved 1 -> refunded 0
        expect(firestoreDB['malf123'].freeAiImportsUsed).toBe(0);
    });

    test('10. concurrent final-import requests -> only one succeeds, the other blocks dynamically safely at limit cap', async () => {
        // User has exactly 1 import remaining mapped organically
        firestoreDB['conc123'] = { freeAiImportsUsed: 1, freeAiImportsLimit: 2 };

        // Blast dual simultaneous HTTP payloads against the isolated backend
        const req1 = request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer concurrent_user')
            .attach('image', dummyImage, 'test.jpg');

        const req2 = request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer concurrent_user')
            .attach('image', dummyImage, 'test.jpg');

        const [res1, res2] = await Promise.all([req1, req2]);

        const statuses = [res1.status, res2.status].sort();

        // Exact expectation mapped structurally: One succeeds (200), one bounces limits inherently (403)
        expect(statuses).toEqual([200, 403]);
        expect(firestoreDB['conc123'].freeAiImportsUsed).toBe(2);
    });

    test('11. successful response matches the existing Android timetable JSON schema', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200);
        expect(res.body.division).toBe("A");
        expect(Array.isArray(res.body.classes)).toBe(true);
        expect(res.body.classes[0].day).toBe("Monday");
    });

    test('12. missing App Check token -> logs diagnostic but does not block', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200); // Observational mode: does NOT block
    });

    test('13. invalid App Check token -> logs diagnostic but does not block', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .set('X-Firebase-AppCheck', 'invalid_token')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200); // Observational mode: does NOT block
    });

    test('14. valid Auth + valid App Check token -> passes with verification', async () => {
        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer valid_free_user')
            .set('X-Firebase-AppCheck', 'valid_appcheck_token')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(200);
    });

    test('15. Upstream timeout -> 503 + quota refund exactly once', async () => {
        firestoreDB['fail123'] = { freeAiImportsUsed: 1, freeAiImportsLimit: 2 };
        geminiBehavior = 'timeout';

        const res = await request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer user_gemini_fail')
            .attach('image', dummyImage, 'test.jpg');

        expect(res.status).toBe(503);
        expect(res.body.error).toBe('AI_EXTRACTION_FAILED');
        // Originally 1 -> reserved to 2 -> refunded safely back to 1
        expect(firestoreDB['fail123'].freeAiImportsUsed).toBe(1);
    });

    test('16. Client disconnect -> quota refund exactly once', (done) => {
        firestoreDB['fail123'] = { freeAiImportsUsed: 1, freeAiImportsLimit: 2 };
        geminiBehavior = 'timeout'; // Make it hang so it doesn't complete naturally

        // Start request but don't await immediately
        const reqPost = request(app)
            .post('/api/extract-timetable')
            .set('Authorization', 'Bearer user_gemini_fail')
            .attach('image', dummyImage, 'test.jpg');

        // Use an HTTP event listener mechanism mapping to the active socket to forcefully kill it
        const connectionObject = reqPost.end();

        // Wait exactly 30ms -> Server has reserved the quota and entered the Gemini delay loop
        setTimeout(() => {
            // Unplug the virtual TCP socket exactly as OkHttp would via Render Proxy Timeout
            connectionObject.abort();

            // Wait for Express/Node.js event loop to process "req.on('close')" and Firestore writes
            setTimeout(() => {
                try {
                    // It should increment upwards to 2 securely, then safely drop back down to 1
                    expect(firestoreDB['fail123'].freeAiImportsUsed).toBe(1);
                    done();
                } catch (e) { done(e); }
            }, 100);
        }, 30);
    });
});
