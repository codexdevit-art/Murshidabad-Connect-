const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const SUPER_ADMIN_UID = "super_admin_uid_001";
const MANAGER_UID = "manager_uid_002";
const SUPER_ADMIN_EMAIL = "rjjejsjs29@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
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

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("news").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("community_posts").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("admins").get());
  await assertFails(unauthDb.collection("settings").doc("public").get());
});

test("User profile: first-login create, self-update, and privilege escalation denial", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true })
    .firestore();

  const created = pastDate(10000);
  // Valid first-login create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Begum",
      email: "alice@gmail.com",
      photoUrl: "https://lh3.googleusercontent.com/a/default-user",
      provider: "google.com",
      role: "USER",
      status: "ACTIVE",
      district: "Murshidabad",
      block: "Berhampore",
      village: "Khagra",
      preferredLanguage: "bn",
      selectedDashboards: ["student", "general"],
      primaryDashboard: "student",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Permitted profile edit succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      village: "Cossimbazar",
      preferredLanguage: "en",
      updatedAt: pastDate(1000),
    })
  );

  // Self-promotion to ADMIN must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(500),
    })
  );

  // Self-changing status must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      status: "SUSPENDED",
      updatedAt: pastDate(500),
    })
  );

  // Creating user with ADMIN role must fail
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob",
      email: "bob@gmail.com",
      provider: "google.com",
      role: "ADMIN",
      status: "ACTIVE",
      district: "Murshidabad",
      selectedDashboards: ["general"],
      primaryDashboard: "general",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user PII read and list must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: SUPER_ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofedDb.collection("news").doc("news_spoof").set({
      title: "Fake News",
      description: "Spoofed admin attempt",
      category: "District",
      isPublished: true,
      status: "ACTIVE",
      createdBy: "spoof_uid",
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
});

test("Bootstrap Super Admin & Content CRUD + User Read Query Alignment", async () => {
  const adminDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true })
    .firestore();

  const ts = pastDate(8000);

  // Admin creates published news & unpublished draft
  await assertSucceeds(
    adminDb.collection("news").doc("news_1").set({
      title: "Berhampore New Bridge Update",
      description: "Construction completed ahead of schedule.",
      imageUrl: "https://example.com/bridge.jpg",
      category: "District",
      block: "Berhampore",
      isPublished: true,
      status: "ACTIVE",
      createdBy: SUPER_ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("news").doc("news_draft").set({
      title: "Internal Draft",
      description: "Not yet published.",
      category: "District",
      isPublished: false,
      status: "DRAFT",
      createdBy: SUPER_ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // User filtered query for published news succeeds
  await assertSucceeds(
    aliceDb.collection("news").where("isPublished", "==", true).get()
  );

  // User unfiltered query or reading draft fails
  await assertFails(aliceDb.collection("news").get());
  await assertFails(aliceDb.collection("news").doc("news_draft").get());

  // Ordinary user cannot publish news
  await assertFails(
    aliceDb.collection("news").doc("news_user").set({
      title: "User News",
      description: "Unauthorized",
      category: "District",
      isPublished: true,
      status: "ACTIVE",
      createdBy: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Manager RBAC: module permissions enforced; cannot self-escalate or access security settings", async () => {
  const adminDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const managerDb = testEnv
    .authenticatedContext(MANAGER_UID, {
      email: "manager@gmail.com",
      email_verified: true,
    })
    .firestore();

  const ts = pastDate(8000);

  await assertSucceeds(
    adminDb.collection("managers").doc(MANAGER_UID).set({
      uid: MANAGER_UID,
      email: "manager@gmail.com",
      name: "News Manager",
      role: "MANAGER",
      status: "ACTIVE",
      permissions: {
        news: ["view", "add", "edit"],
      },
      createdBy: SUPER_ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Manager can add news
  await assertSucceeds(
    managerDb.collection("news").doc("mgr_news_1").set({
      title: "Local Mandi Update",
      description: "Updated timings for Lalbagh.",
      category: "Block",
      isPublished: true,
      status: "ACTIVE",
      createdBy: MANAGER_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Manager CANNOT delete news (no 'delete' perm)
  await assertFails(managerDb.collection("news").doc("mgr_news_1").delete());

  // Manager CANNOT add jobs (no 'jobs' module perm)
  await assertFails(
    managerDb.collection("jobs").doc("job_1").set({
      title: "Clerk",
      description: "District office",
      category: "Government",
      isPublished: true,
      status: "ACTIVE",
      createdBy: MANAGER_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Manager CANNOT modify own permissions or access system settings
  await assertFails(
    managerDb.collection("managers").doc(MANAGER_UID).update({
      permissions: { jobs: ["view", "add", "edit", "delete"] },
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    managerDb.collection("settings").doc("security").set({
      updatedBy: MANAGER_UID,
      updatedAt: ts,
    })
  );
});

test("Community posts, comments, likes, lost & found, and reports work securely", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true })
    .firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    aliceDb.collection("community_posts").doc("post_1").set({
      title: "Road Repair Needed in Jiaganj",
      description: "Main bazaar road needs urgent repair.",
      imageUrl: "https://i.ibb.co/example/road.jpg",
      category: "Civic",
      block: "Jiaganj",
      authorUid: ALICE_UID,
      authorName: "Alice Begum",
      likedBy: [],
      likesCount: 0,
      commentsCount: 0,
      isPublished: true,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can like Alice's post
  await assertSucceeds(
    bobDb.collection("community_posts").doc("post_1").update({
      likedBy: [BOB_UID],
      likesCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Bob CANNOT edit Alice's post title
  await assertFails(
    bobDb.collection("community_posts").doc("post_1").update({
      title: "Hijacked Title",
      updatedAt: pastDate(1000),
    })
  );

  // Bob can comment on post_1
  await assertSucceeds(
    bobDb.collection("community_comments").doc("cmt_1").set({
      postId: "post_1",
      authorUid: BOB_UID,
      authorName: "Bob",
      text: "Agreed, very urgent!",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice can report post_1
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      targetCollection: "community_posts",
      targetId: "post_1",
      reason: "Duplicate post",
      reporterUid: ALICE_UID,
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Lost & found report
  await assertSucceeds(
    aliceDb.collection("lost_found").doc("lf_1").set({
      title: "Lost Wallet near Hazarduari",
      description: "Brown leather wallet lost near gate 2.",
      imageUrl: "https://example.com/wallet.jpg",
      category: "LOST",
      location: "Lalbagh",
      contactInfo: "9876543210",
      dateText: "26 Nov 2026",
      reporterUid: ALICE_UID,
      reporterName: "Alice Begum",
      isPublished: true,
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
