const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-robbyai-e76dc5df-cd11-42df-b49d-caec348e862b";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
      databaseId: DATABASE_ID
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("unauthenticated user cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.doc("users/alice").get());
  await assertFails(unauthDb.doc("users/alice").set({ userId: "alice", language: "en" }));
});

test("authenticated owner can create and read their profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore(DATABASE_ID);
  await assertSucceeds(aliceDb.doc("users/alice").set({
    userId: "alice",
    language: "en",
    name: "Alice",
    voiceEnabled: true
  }));
  await assertSucceeds(aliceDb.doc("users/alice").get());
});

test("cross user access to profile is denied", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore(DATABASE_ID);
  await assertFails(bobDb.doc("users/alice").get());
  await assertFails(bobDb.doc("users/alice").set({
    userId: "alice",
    language: "en"
  }));
});

test("owner can create chat messages and notes", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore(DATABASE_ID);
  await assertSucceeds(aliceDb.collection("users/alice/messages").doc("msg1").set({
    userId: "alice",
    role: "user",
    content: "Open Chrome",
    language: "en"
  }));

  await assertSucceeds(aliceDb.collection("users/alice/notes").doc("note1").set({
    userId: "alice",
    title: "Grocery list",
    body: "Apples, Milk"
  }));
});

test("other users cannot access alice notes or messages", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore(DATABASE_ID);
  await assertFails(bobDb.doc("users/alice/messages/msg1").get());
  await assertFails(bobDb.doc("users/alice/notes/note1").get());
});
