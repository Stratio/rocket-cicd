#!groovy

def call(Map promotion = [:]) {

    def parameters = promotion["release"]["parameters"]
    def generalParams = parameters["General"]

    def NODE = generalParams?.JENKINS_NODE_NAME ?: "jenkins-slave-mvn-jdk11"
    node(NODE) {

        println("Starting promotion for release ${promotion['releaseId']}")
        def RELEASE_ID = promotion["releaseId"]

        def PUBLIC_JENKINS_URL = generalParams?.PUBLIC_JENKINS_URL
        def REPLACED_BUILD_URL = PUBLIC_JENKINS_URL ? env.BUILD_URL.replace(env.JENKINS_URL, PUBLIC_JENKINS_URL) : env.BUILD_URL

        def ROCKET_URL = generalParams["ROCKET_SOURCE_URL"]
        def ROCKET_TENANT = generalParams?.ROCKET_SOURCE_TENANT

        def ROCKET_TARGET_URL = generalParams["ROCKET_TARGET_URL"]
        def ROCKET_TARGET_TENANT = generalParams?.ROCKET_TARGET_TENANT
        def TARGET_PROJECT_NAME = generalParams?.TARGET_PROJECT_NAME ?: ""

        def ARCHIVE_PATH = "${BUILD_TAG}.zip".replace(" ", "-")
        def MAVEN_PLUGIN_VERSION = generalParams?.MAVEN_PLUGIN_VERSION ?: "2.0.0-SNAPSHOT"

        def CONNECT_TIMEOUT = generalParams?.CONNECT_TIMEOUT ?: "2000"
        def READ_TIMEOUT = generalParams?.READ_TIMEOUT ?: "10000"

        def REPOSITORY_URL = generalParams?.REPOSITORY_URL
        def REPOSITORY_NAME = generalParams?.REPOSITORY_NAME
        def REPOSITORY_CREDENTIALS_ID = generalParams?.REPOSITORY_CREDENTIALS_ID ?: "REPOSITORY_CREDENTIALS"
        def REPOSITORY_TYPE = generalParams?.REPOSITORY_TYPE

        def sleep_time = generalParams?.PAUSE_TIME ?: 4

        stage('Init release') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -U -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:init -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DbuildUrl=$REPLACED_BUILD_URL -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check prod instance') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkEnv -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Validate workflow') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]){
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:validateAssets -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Export asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:exportAssets -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DexportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
            archiveArtifacts artifacts: "${ARCHIVE_PATH}"
        }

        sleep(time: sleep_time, unit: "SECONDS")

        if (REPOSITORY_URL && REPOSITORY_NAME && REPOSITORY_CREDENTIALS_ID && REPOSITORY_TYPE) {
            stage('Upload artifact') {
                configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: REPOSITORY_CREDENTIALS_ID, usernameVariable: 'REPOSITORY_USER', passwordVariable: 'REPOSITORY_PASSWORD']]) {
                            sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:uploadArtifact -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DfilePath=$ARCHIVE_PATH -DrepositoryUrl=$REPOSITORY_URL -DrepositoryName=$REPOSITORY_NAME -DrepositoryUser=$REPOSITORY_USER -DrepositoryPassword=$REPOSITORY_PASSWORD -DrepositoryType=$REPOSITORY_TYPE -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                        }
                    }
                }
            }
        }

        sleep(time: sleep_time, unit: "SECONDS")

        stage('Import asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:importAssets -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DtargetProjectName=$TARGET_PROJECT_NAME -DimportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Set released') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:setAssetStates -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DassetState='Release' -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Lock origin') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:lockAssetVersions -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Finalize release') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s '$MAVEN_SETTINGS' com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:finish -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }


    }
}