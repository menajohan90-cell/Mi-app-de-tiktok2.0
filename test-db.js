const getDb = require('./backend/database.js');
getDb().then(db => {
  console.log("DB initialized");
}).catch(e => {
  console.error("Error:", e);
});
