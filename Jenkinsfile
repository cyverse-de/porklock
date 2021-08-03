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
                dockerImage.inside("--entrypoint=''") {
                  sh "lein test2junit"
                  junit 'test2junit/xml/*.xml'
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
