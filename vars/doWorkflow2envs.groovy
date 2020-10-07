#!groovy

def call(Map props = [:]) {

    node("rocket") {

        println("Rocket flow for workflow 2 envs")

        def sleep_time = 4

        def ROCKET_URL = props["ROCKET_API_URL_DEV"]
        def TENANT_DEV = props["ROCKET_TENANT_DEV"]

        def ROCKET_URL_PROD = props["ROCKET_API_URL_PRO"]
        def TENANT_PROD = props["ROCKET_TENANT_PRO"]

        def ASSET_VERSION_ID = props["assetVersionId"]
        def RELEASE_ID = props["releaseId"]
        def ARCHIVE_PATH = "${BUILD_TAG}.zip"
        def MAVEN_PLUGIN_VERSION = props["MAVEN_PLUGIN_VERSION"] ? props["MAVEN_PLUGIN_VERSION"] : "1.1.0-SNAPSHOT"
        def CONNECT_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "2000"
        def READ_TIMEOUT = props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "10000"

        stage('Init release') {
            getToken("ROCKET_AUTH_CREDENTIALS_DEV", ROCKET_URL, TENANT_DEV, "token_dev")
            getToken("ROCKET_AUTH_CREDENTIALS_PRO", ROCKET_URL_PROD, TENANT_PROD, "token_prod")
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:init -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DbuildUrl=$BUILD_URL -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check prod instance') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkEnv -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DtargetEnvBaseUrl=$ROCKET_URL_PROD -DtargetEnvCookie=\$(cat token_prod) -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Validate workflow') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:validate -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Check asset dependencies') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:checkDependencies -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DassetVersionId=$ASSET_VERSION_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=\$(cat token_prod) -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Export asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:exportAsset -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DexportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
            archiveArtifacts artifacts: "${ARCHIVE_PATH}"
        }

        sleep(time: sleep_time, unit: "SECONDS")

        stage('Import asset') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:importAsset -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DassetVersionId=$ASSET_VERSION_ID -DreleaseId=$RELEASE_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=\$(cat token_prod) -DimportPath=$ARCHIVE_PATH -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Set released') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:setReleased -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DrocketBaseUrlProd=$ROCKET_URL_PROD -DcookieProd=\$(cat token_prod) -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Lock dev') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:lockDev -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DreleaseId=$RELEASE_ID -DassetVersionId=$ASSET_VERSION_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }

        sleep(time: sleep_time, unit:"SECONDS")

        stage('Finalize release') {
            configFileProvider([configFile(fileId: 'NexusMultiRepoSettings', variable: 'MAVEN_SETTINGS')]) {
                sh "mvn com.stratio.rocket:rocket-maven-plugin:${MAVEN_PLUGIN_VERSION}:finish -DrocketBaseUrl=$ROCKET_URL -Dcookie=\$(cat token_dev) -DreleaseId=$RELEASE_ID -DconnectTimeout=$CONNECT_TIMEOUT -DreadTimeout=$READ_TIMEOUT"
            }
        }
    }
}

def getToken(String credentialsId, String url, String tenant, String resultPath) {

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${credentialsId}", usernameVariable: 'USER', passwordVariable: 'PASS']]) {
        def authScript = libraryResource "scripts/getAuthToken.sh"
        writeFile file: "/tmp/getAuthToken.sh", text: authScript
        sh(script: "bash /tmp/getAuthToken.sh ${url} ${USER} ${PASS} ${tenant} ${resultPath}")
    }
}
