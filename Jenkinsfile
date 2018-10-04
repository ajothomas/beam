#!/usr/bin/env groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * License); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
  agent { docker 'openjdk:8-jdk' }

  environment {
    GRADLE_OPTS=-Dgradle.user.home=/tmp
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
        sh "export"
        sh "./gradlew clean --stacktrace"
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
                  "target": "maven-customerknowledgeplatform-apache-beam-local"
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