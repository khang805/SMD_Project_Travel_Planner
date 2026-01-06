const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://smdproject-ab3fd-default-rtdb.firebaseio.com"
});

const db = admin.database();
const messaging = admin.messaging();
const startTime = Date.now();

console.log('Starting Notification Server...');
console.log('Listening for new messages...');

const chatsRef = db.ref('chats');

// Helper: extract receiver UID and Sender UID from roomKey
function processRoom(roomKey, message) {
  // Expect roomKey format: "uid1_uid2"
  // Logic: The Sender writes to BOTH "Receiver_Sender" and "Sender_Receiver" rooms ??
  // No, usually Sender writes to:
  // 1. senderRoom = receiverUid + "_" + senderUid
  // 2. receiverRoom = senderUid + "_" + receiverUid

  // We want to detect the message intended for the RECEIVER.
  // The message object contains `senderId`.

  const parts = roomKey.split("_");
  if (parts.length !== 2) return;

  const [part1, part2] = parts;
  const senderId = message.senderId;

  // We are looking for the room where the OWNER is the RECEIVER.
  // If roomKey is "A_B", is the owner A or B?
  // In `ChatActivity`: 
  // Copy 1: `senderRoom` (Sender's View) = `receiverUid + "_" + senderUid`
  // Copy 2: `receiverRoom` (Receiver's View) = `senderUid + "_" + receiverUid`

  // When Sender (B) sends to Receiver (A):
  // Writes to `A_B` (SenderRoom variable in code?? Wait.)
  // Let's re-read ChatActivity logic carefully.
  // senderRoom = receiverUid + "_" + senderUid
  // receiverRoom = senderUid + "_" + receiverUid

  // If I am B (Sender), I write to `senderRoom` (A_B) and `receiverRoom` (B_A).

  // We want to notify A.
  // So we should detect the write to `B_A` (ReceiverRoom) ??
  // NO. `receiverRoom` is for the Receiver (A) to read?
  // Wait.
  // If I am A (Receiver), I open chat with B.
  // My `senderUid` = A. `receiverUid` = B.
  // My `senderRoom` = B_A.
  // My `receiverRoom` = A_B.
  // So I read from B_A ??
  // Usually "SenderRoom" means "Room for Sender".
  // If I am A, I read from MY room.

  // Let's check `ChatActivity` READ logic.
  // `mDbRef.child("chats").child(senderRoom!!).child("messages")`
  // It reads from `senderRoom`.
  // `senderRoom` = `receiverUid + "_" + senderUid`
  // If I am A (Viewer), opening chat with B (Target).
  // `senderUid` = A. `receiverUid` = B.
  // `senderRoom` = B_A.
  // So A reads from B_A.

  // So when B sends to A:
  // B writes to B_A (ReceiverRoom key in B's context).
  // AND B writes to A_B (SenderRoom key in B's context).

  // We want to trigger notification for A.
  // A reads from B_A. 
  // So we should listen to B_A ?
  // B_A starts with B (Sender).
  // So if roomKey starts with Sender, it is the room A reads from.

  // Validating:
  // RoomKey = B_A. part1=B, part2=A.
  // senderId = B.
  // part1 == senderId.
  // So receiverId = part2 (A).

  if (part1 === senderId) {
    const receiverId = part2;
    sendNotification(receiverId, message.message, senderId);
  }
}

// Attach listener to each room
function setupRoomListener(roomKey) {
  const messagesRef = db.ref(`chats/${roomKey}/messages`);

  messagesRef.on('child_added', (snapshot) => {
    const message = snapshot.val();

    // Only process new messages
    if (!message.timestamp || message.timestamp < startTime) {
      return;
    }

    if (!message.senderId) return;

    processRoom(roomKey, message);
  });
}

// Listen for rooms
chatsRef.on('child_added', (snapshot) => {
  setupRoomListener(snapshot.key);
});

// Send notification
async function sendNotification(receiverId, text, senderId) {
  try {
    // Get receiver's token
    const userSnapshot = await db.ref(`users/${receiverId}`).once('value');
    if (!userSnapshot.exists()) return;

    const userData = userSnapshot.val();
    const token = userData.fcmToken;

    if (!token) {
      console.log(`No token for user ${receiverId}`);
      return;
    }

    // Get sender's name
    const senderSnapshot = await db.ref(`users/${senderId}`).once('value');
    const realSenderName = senderSnapshot.exists() ? senderSnapshot.val().Username : "New Message";

    // Build payload
    const messagePayload = {
      token: token,
      notification: {
        title: realSenderName,
        body: text
      },
      data: {
        userId: senderId,
        receiverId: receiverId // Important for client-side filtering
      }
    };

    // Send notification
    const response = await messaging.send(messagePayload);
    console.log(`Notification sent to ${receiverId}`);

  } catch (error) {
    console.error('Error sending notification:', error);
  }
}
