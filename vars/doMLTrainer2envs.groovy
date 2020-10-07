#!groovy

def call(Map props = [:]) {

    def NODE = props["JENKINS_NODE_NAME"] ? props["JENKINS_NODE_NAME"] : "jenkins-slave-mvn-jdk11"

    node(NODE) {

        println("Rocket flow for MLTrainer 2 envs")

        def ROCKET_URL = props["ROCKET_API_URL_DEV"]
        def ROCKET_TENANT = props["ROCKET_TENANT_DEV"]

        def ROCKET_TARGET_URL = props["ROCKET_API_URL_PRO"]
        def ROCKET_TARGET_TENANT = props["ROCKET_TENANT_PRO"]

        def ASSET_VERSION_ID = props["assetVersionId"]
        def RELEASE_ID = props["releaseId"]
        def ARCHIVE_PATH = "${BUILD_TAG}.zip"
        def MAVEN_PLUGIN_VERSION = props["MAVEN_PLUGIN_VERSION"] ? props["MAVEN_PLUGIN_VERSION"] : "1.1.0-d3ffff0"
        def CONNECT_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "2000"
        def READ_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "10000"


        stage('Init release') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:init -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DbuildUrl=$BUILD_URL -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
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
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkEnv -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check asset dependencies') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkDependencies -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DassetVersionId=$ASSET_VERSION_ID -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Export asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:exportAsset -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DexportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
            archiveArtifacts artifacts: "${ARCHIVE_PATH}"
        }

        sleep(time: sleep_time, unit: "SECONDS")

        stage('Import asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:importAsset -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DimportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Set released') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:setAssetState -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DassetState='Release' -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Lock origin') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:lockAssetVersion -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        stage('Lock target') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:lockAssetVersion -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DtargetEnvBaseUrl=$ROCKET_TARGET_URL -DtargetEnvUser=$ROCKET_TARGET_USER -DtargetEnvPassword=$ROCKET_TARGET_PASS -DtargetEnvTenant=$ROCKET_TARGET_TENANT -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Finalize release') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS", usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:finish -DrocketBaseUrl=$ROCKET_URL -Duser=$ROCKET_USER -Dpassword=$ROCKET_PASS -Dtenant=$ROCKET_TENANT -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
                }
            }
        }
    }
}
