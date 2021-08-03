#!groovy

milestone 0
timestamps {
    node('docker') {
        checkout scm

        docker.withRegistry('https://harbor.cyverse.org', 'jenkins-harbor-credentials') {
            def dockerImage
            stage('Build') {
                milestone 50
                dockerImage = docker.build("harbor.cyverse.org/de/porklock:${env.BUILD_TAG}")
                milestone 51
                dockerImage.push();
            }
            stage('Test') {
                try {
                    sh "docker run --rm -v \$(pwd)/test2junit:/usr/src/app/test2junit --entrypoint 'lein' ${dockerImage.imageName()} test2junit"
                } finally {
                    junit 'test2junit/xml/*.xml'

                    sh "docker run --rm -v \$(pwd):/build -w /build alpine rm -r test2junit"
                }
            }
            stage('Docker Push') {
                milestone 100
                dockerImage.push("${env.BRANCH_NAME}")
                milestone 101
            }
        }
    }
}
