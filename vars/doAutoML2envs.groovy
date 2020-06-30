#!groovy

def call(Map props = [:]) {

    node("rocket") {

        println("Rocket flow for AutoML 2 envs")

        def sleep_time = 4
        def ROCKET_URL = props["ROCKET_API_URL_DEV"]
        def ROCKET_COOKIE = ""
        def ROCKET_URL_PROD = props["ROCKET_API_URL_PRO"]
        def ROCKET_COOKIE_PROD = ""
        def ASSET_VERSION_ID = props["assetVersionId"]
        def RELEASE_ID = props["releaseId"]
        def ARCHIVE_PATH = "${BUILD_TAG}.zip"
        def MAVEN_PLUGIN_VERSION = props["MAVEN_PLUGIN_VERSION"] ? props["MAVEN_PLUGIN_VERSION"] : "1.1.0-SNAPSHOT"
        def CONNECT_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "2000"
        def READ_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "10000"

        stage('Init release') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:init -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_COOKIE -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DbuildUrl=$BUILD_URL -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check prod instance') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkEnv -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_COOKIE -DtargetEnvBaseUrl=$ROCKET_URL_PROD -DtargetEnvCookie=$ROCKET_COOKIE_PROD -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check asset dependencies') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkDependencies -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_COOKIE -DassetVersionId=$ASSET_VERSION_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=$ROCKET_COOKIE_PROD -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Export asset') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:exportAsset -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_COOKIE -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DexportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            archiveArtifacts artifacts: "${ARCHIVE_PATH}"
        }

        sleep(time: sleep_time, unit: "SECONDS")

        stage('Import asset') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:importAsset -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_COOKIE -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=$ROCKET_COOKIE_PROD -DimportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Set released') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:setReleased -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_URL -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=$ROCKET_COOKIE_PROD -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Lock dev') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:lockDev -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_URL -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Finalize release') {
            sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:finish -DrocketBaseUrl=$ROCKET_URL -Dcookie=$ROCKET_URL -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
        }

    }

}
