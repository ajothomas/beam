#!/usr/bin/env groovy

@Library('ECM@master') _

pipeline {
  agent {
    dockerfile {
      dir 'build-image'
      args """\
           -v /home/jenkins:/home/jenkins
           """
    }
  }

  environment {
    // create one gradle cache per executor
    GRADLE_USER_HOME = "/home/jenkins/.gradle_${env.EXECUTOR_NUMBER}"
  }

  stages {
    stage('Fetch Tags') {
      steps {
        script {
          withCredentials([usernameColonPassword(credentialsId: 'effort_service_account', variable: 'GIT_USERINFO')]) {
            // tags are necessary for calculating the current version number
            // at some point the git plugin for jenkins stopped fetching these
            def gitLocation = new URI(env.GIT_URL)
            // $GIT_USERINFO needs to be in the `sh` block in order to be masked out in the logs
            sh "git fetch --tags ${gitLocation.scheme}://$GIT_USERINFO@${gitLocation.host}${gitLocation.path}"
          }
        }
      }
    }

    stage('Clean') {
      steps {
        sh "./gradlew clean"
      }
    }

    stage('Compile Java Main') {
      steps {
        sh "./gradlew compileJava"
      }
    }

    stage('Compile Java Test') {
      steps {
        sh "./gradlew compileTestJava"
      }
    }

    stage('Unit Test') {
      steps {
        sh "./gradlew test"
      }
    }

    stage('Jar') {
      steps {
        sh "./gradlew shadowJar"
      }
    }

    stage('Publish') {
      steps {
        script {

          if (env.BRANCH_NAME != "master") {
            sh "echo 'Not on master, not publishing'"
          } else {
            sh "echo 'Publishing to artifactory'"

            def artifactoryServer = Artifactory.server('godaddy_artifactory_server')

            def uploadSpec = """{
              "files": [
                {
                  "pattern": "build/libs/*",
                  "target": "maven-customerknowledgeplatform-spanner-io-local"
                }
              ]
            }"""

            def buildInfo = artifactoryServer.upload uploadSpec

            artifactoryServer.publishBuildInfo buildInfo
          }
        }
      }
    }
  }
}