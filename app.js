const express = require('express');
const bodyParser = require('body-parser');
const path = require('path');

const app = express();
const port = 3000;

app.use(bodyParser.json());

// Set the view engine to ejs
app.set('view engine', 'ejs');

// Serve static files from the "public" directory
app.use(express.static('public'));

let receivedData = '';

app.post('/receive-data', (req, res) => {
    const data = req.body;
    console.log("Received raw body from Java program:");
    console.log(req.body); // Log the entire request body
    console.log("Parsed data field:");
    console.log(data.data); // Log the specific data field

    receivedData = data.data;
    
    res.sendStatus(200);
});

app.get('/', (req, res) => {
    res.render('index', { data: receivedData });
});

app.listen(port, () => {
    console.log(`Node.js app listening at http://localhost:${port}`);
});
