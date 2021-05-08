// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//   functions.logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

const functions = require("firebase-functions");

// The Firebase Admin SDK to access Firestore.
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Adds two numbers together.
 * @param {int} valuesX Number of items.
 * @param {int} valuesY Wait times.
 * @param {int} nItems Current n Items in queue.
 * @return {int} Expected wait time.
 */
function findLineByLeastSquares(valuesX, valuesY, nItems) {
  let sumX = 0;
  let sumY = 0;
  let sumXY = 0;
  let sumXX = 0;
  let count = 0;


  let x = 0;
  let y = 0;
  const valuesLength = valuesX.length;

  if (valuesLength != valuesY.length) {
    throw new Error("The parameters need to have same size!");
  }

  if (valuesLength === 0) {
    return -1;
  }

  for (let v = 0; v < valuesLength; v++) {
    x = valuesX[v];
    y = valuesY[v];
    sumX += x;
    sumY += y;
    sumXX += x*x;
    sumXY += x*y;
    count++;
  }

  const m = (count*sumXY - sumX*sumY) / (count*sumXX - sumX*sumX);
  const b = (sumY/count) - (m*sumX)/count;

  return nItems * m + b;
}

/**
 * Adds two numbers together.
 * @param {int} lat1 Latitude 1.
 * @param {int} lon1 Longitude 1.
 * @param {int} lat2 Latitude 2.
 * @param {int} lon2 Longitude 2.
 * @return {int} Distance between locations.
 */
function distance(lat1, lon1, lat2, lon2) {
  if ((lat1 == lat2) && (lon1 == lon2)) {
    return 0;
  } else {
    const radlat1 = Math.PI * lat1/180;
    const radlat2 = Math.PI * lat2/180;
    const theta = lon1-lon2;
    const radtheta = Math.PI * theta/180;
    let dist = Math.sin(radlat1) * Math.sin(radlat2) +
    Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
    if (dist > 1) {
      dist = 1;
    }
    dist = Math.acos(dist);
    dist = dist * 180/Math.PI;
    dist = dist * 60 * 1.1515;
    return (dist * 1.609344) * 1000;
  }
}


exports.computeQueueWaitTime = functions.https.onCall(async (data, context) => {
// exports.computeQueueWaitTime = functions.https.onRequest(async (req, res) => {
  // Get store ID
  const storeId = data.store;
  // const storeId = req.query.store;
  console.log(storeId);
  const storeListRef = admin.firestore().collection("StoreList").doc(storeId);
  const userStore = await storeListRef.get();
  if (!userStore.exists) {
    console.log("No such document!");
  } else {
    // console.log("Document data:", userStore.data());
  }


  const usLatitude = userStore.data().latitude;
  const usLongitude = userStore.data().longitude;

  const waitTimes = [];
  const nItems = [];

  const allStoreListRef = admin.firestore().collection("StoreList");
  const snapshot = await allStoreListRef.get();
  snapshot.forEach((doc) => {
    const sLatitude = doc.data().latitude;
    const sLongitude = doc.data().longitude;

    if (distance(usLatitude, usLongitude, sLatitude, sLongitude) < 20) {
      console.log(doc.data().nCartItemsAtArrival);
      console.log(doc.data().nCartItemsAtArrival.length);
      for (let i = 0; i < doc.data().nCartItemsAtArrival.length; i++) {
        console.log(doc.data().timeInQueue[i]);
        console.log(doc.data().nCartItemsAtArrival[i]);
        waitTimes.push(doc.data().timeInQueue[i]);
        nItems.push(doc.data().nCartItemsAtArrival[i]);
      }
    }
  });
  console.log(waitTimes);
  console.log(nItems);
  const result = findLineByLeastSquares(nItems,
      waitTimes, userStore.data().nQueueItems);
  // res.json({"result": result});
  console.log(result);
  return {result: result};
});
