const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');

const DATABASE_ID = 'ai-studio-android-scnsreco-b9495452-59a6-41d5-a91b-ba691fef3ab8';
const PROJECT_ID = 'gen-lang-client-0665361055';

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync('firestore.rules', 'utf8'),
      host: '127.0.0.1',
      port: 8085,
    },
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

function makeLearner(uid, overrides = {}) {
  const now = new Date();
  return {
    ownerId: uid,
    regNo: 'SCNS-2027-001',
    names: 'Sipho',
    surname: 'Ndlovu',
    grade: '10',
    school: 'eZimisele High',
    sex: 'Male',
    race: 'African',
    dob: '2011-05-12',
    age: '16',
    admDate: '2027-01-23',
    subjects: 'Mathematics, Life Sciences',
    status: 'Accepted',
    depPaid: true,
    depDate: '2027-01-23',
    parent: 'Mrs Ndlovu 0821234567',
    docRef: 'Folder A',
    att: [{ d: '2027-01-23', day: 'Saturday', s: 'Present', subject: 'Mathematics' }],
    pay: [{ d: '2027-01-23', t: 'Termly', month: '', term: 'Term 1', amt: 700, note: 'Deposit' }],
    exams: {
      Mathematics_Term_1: {
        subject: 'Mathematics',
        term: 'Term 1',
        date: '2027-02-06',
        t1: 78,
        t2: 82,
        t3: 75,
        t4: 85,
      },
    },
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function makeExecutive(uid, overrides = {}) {
  const now = new Date();
  return {
    ownerId: uid,
    name: 'Thembelani',
    surname: 'Sithole',
    idNumber: '9001015009081',
    cell: '0813600636',
    pin: '2027',
    role: 'Chairperson',
    approved: true,
    otp: '',
    otpPending: false,
    termLabel: 'Term 1',
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function makeChairConfig(uid, overrides = {}) {
  const now = new Date();
  return {
    ownerId: uid,
    chairName: 'Mr Thembelani Sithole',
    chairCell: '0813600636',
    prevChairCell: '',
    prevCellUntilMs: 0,
    otpSender: 'Chairperson',
    trialStartMs: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function makeManualSave(uid, overrides = {}) {
  return {
    ownerId: uid,
    label: 'Backup 2027-02-06',
    savedAtDisplay: '2027-02-06 10:30',
    learnerCount: 1,
    totalCollected: 700,
    snapshotJson: '[]',
    createdAt: new Date(),
    ...overrides,
  };
}

test('unauthenticated requests are denied across all collections', async () => {
  const anon = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(anon.collection('users/alice/learners').get());
  await assertFails(anon.doc('users/alice/learners/l1').set(makeLearner('alice')));
  await assertFails(anon.collection('users/alice/executives').get());
  await assertFails(anon.doc('users/alice/settings/chairperson').get());
  await assertFails(anon.collection('users/alice/manualSaves').get());
});

test('owner can create, query, update, and delete learner records', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);
  const ref = alice.doc('users/alice/learners/learner_1');

  await assertSucceeds(ref.set(makeLearner('alice')));
  await assertSucceeds(alice.collection('users/alice/learners').get());
  await assertSucceeds(
    ref.update({
      status: 'Waiting List',
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(ref.delete());
});

test('cross-user isolation denies Bob from reading or modifying Alice data', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);
  const bob = testEnv.authenticatedContext('bob').firestore(DATABASE_ID);

  await assertSucceeds(alice.doc('users/alice/learners/l1').set(makeLearner('alice')));
  await assertSucceeds(alice.doc('users/alice/executives/e1').set(makeExecutive('alice')));
  await assertSucceeds(alice.doc('users/alice/settings/chairperson').set(makeChairConfig('alice')));
  await assertSucceeds(alice.doc('users/alice/manualSaves/s1').set(makeManualSave('alice')));

  await assertFails(bob.doc('users/alice/learners/l1').get());
  await assertFails(bob.collection('users/alice/learners').get());
  await assertFails(bob.doc('users/alice/learners/l1').update({ status: 'Rejected' }));
  await assertFails(bob.doc('users/alice/learners/l1').delete());
  await assertFails(bob.doc('users/alice/executives/e1').get());
  await assertFails(bob.doc('users/alice/settings/chairperson').get());
  await assertFails(bob.doc('users/alice/manualSaves/s1').get());
});

test('schema validation rejects invalid grade, status, extra keys, or immutable field changes', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);

  await assertFails(
    alice.doc('users/alice/learners/bad_grade').set(makeLearner('alice', { grade: '13' }))
  );
  await assertFails(
    alice.doc('users/alice/learners/bad_status').set(makeLearner('alice', { status: 'Expelled' }))
  );
  await assertFails(
    alice.doc('users/alice/learners/extra_id').set(makeLearner('alice', { id: 'should_not_exist' }))
  );
  await assertFails(
    alice.doc('users/alice/learners/owner_spoof').set(makeLearner('bob'))
  );

  const validRef = alice.doc('users/alice/learners/valid_1');
  await assertSucceeds(validRef.set(makeLearner('alice')));
  await assertFails(validRef.update({ ownerId: 'bob', updatedAt: new Date() }));
  await assertFails(validRef.update({ createdAt: new Date(Date.now() + 60000), updatedAt: new Date() }));
});

test('executives, chairperson settings, and manual saves enforce strict validation', async () => {
  const alice = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);

  await assertSucceeds(alice.doc('users/alice/executives/exec_1').set(makeExecutive('alice')));
  await assertFails(
    alice.doc('users/alice/executives/exec_bad_pin').set(makeExecutive('alice', { pin: '12' }))
  );
  await assertSucceeds(
    alice.doc('users/alice/executives/exec_1').update({ approved: false, otp: '654321', updatedAt: new Date() })
  );

  await assertSucceeds(alice.doc('users/alice/settings/chairperson').set(makeChairConfig('alice')));
  await assertFails(alice.doc('users/alice/settings/other').set(makeChairConfig('alice')));
  await assertSucceeds(
    alice.doc('users/alice/settings/chairperson').update({
      chairCell: '0821234567',
      prevChairCell: '0813600636',
      prevCellUntilMs: Date.now() + 604800000,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(alice.doc('users/alice/manualSaves/save_1').set(makeManualSave('alice')));
  await assertFails(alice.doc('users/alice/manualSaves/save_1').update({ label: 'Tampered' }));
  await assertSucceeds(alice.doc('users/alice/manualSaves/save_1').delete());
});
