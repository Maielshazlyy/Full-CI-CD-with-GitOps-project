def call() {
    echo "Installing dependencies, running unit tests, and checking the Node.js app"
    sh 'npm install'
    sh 'npm test'
    sh 'node --check server.js'
}
