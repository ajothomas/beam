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

  agent { dockerfile true }

  environment {
    GRADLE_OPTS='-Dgradle.user.home=/tmp'
    HOME = '/tmp/'
  }

  stages {
      stage('Build , Publish to artifactory') {
        steps {
          script {
            checkout scm
            sh 'env'
            List<String> buildOptions = [
                      './gradlew',
                      'publish',
                      '-PisRelease',
                      '-PnoSigning=true',
                      '--no-daemon',
                      '--no-parallel'
                    ]
            String buildCmd = "${buildOptions.join(' ')} --stacktrace"
            echo "Build Command: $buildCmd"
            withCredentials([usernamePassword(credentialsId: 'effort_artifactory_user', passwordVariable: 'GRGIT_PASS', usernameVariable: 'GRGIT_USER')])
            {
              withEnv(["ORG_GRADLE_PROJECT_ARTIFACTORY_PASS=${env.GRGIT_PASS}",
                       "ORG_GRADLE_PROJECT_ARTIFACTORY_USER=${env.GRGIT_USER}"])
              {
                sh "$buildCmd"
              }
            }
          }
        }
      }
    }
}