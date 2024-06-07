const net = require('net');
const { exec } = require('child_process');

// Create a server instance
const server = net.createServer(socket => {
    console.log('Java socket connected');

    // Execute the Python script
    exec('python AccessControllAccess.py', (error, stdout, stderr) => {
        if (error) {
            console.error(`Error executing Python script: ${error}`);
            return;
        }
        console.log(`Python script output: ${stdout}`);
        // Send the output to the Java client
        socket.write(stdout);
    });

    // Event listener for client disconnection
    socket.on('end', () => {
        console.log('Java socket disconnected');
    });

    // Event listener for errors
    socket.on('error', err => {
        console.error('Socket error:', err.message);
    });
});

// Start listening on port 8080
const PORT = 8080;
server.listen(PORT, () => {
    console.log(`Node.js server listening on port ${PORT}`);
});
