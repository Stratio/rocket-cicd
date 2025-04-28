#!groovy

def call(Map props = [:]) {

    // Load all the variables into the environment
    initializeVariables(props)

    node(env.NODE) {

        println("Rocket flow for MLTrainer 2 envs")


        stage('Init release') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:init -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DreleaseId=$env.RELEASE_ID -DassetVersionId=$env.ASSET_VERSION_ID -DbuildUrl=$env.BUILD_URL -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Check prod instance') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:checkEnv -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DtargetEnvBaseUrl=$env.ROCKET_TARGET_URL -DtargetEnvUser=$env.ROCKET_TARGET_USER -DtargetEnvPassword=$env.ROCKET_TARGET_PASS -DtargetEnvTenant=$env.ROCKET_TARGET_TENANT -DreleaseId=$env.RELEASE_ID -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Check asset dependencies') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:checkDependencies -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DassetVersionId=$env.ASSET_VERSION_ID -DtargetEnvBaseUrl=$env.ROCKET_TARGET_URL -DtargetEnvUser=$env.ROCKET_TARGET_USER -DtargetEnvPassword=$env.ROCKET_TARGET_PASS -DtargetEnvTenant=$env.ROCKET_TARGET_TENANT -DreleaseId=$env.RELEASE_ID -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Export asset') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:exportAsset -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DassetVersionId=$env.ASSET_VERSION_ID -DreleaseId=$env.RELEASE_ID -DexportPath=$env.ARCHIVE_PATH -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
            archiveArtifacts artifacts: "${env.ARCHIVE_PATH}"
        }

        sleep(time: env.SLEEP_TIME, unit: "SECONDS")

        stage('Import asset') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:importAsset -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DassetVersionId=$env.ASSET_VERSION_ID -DreleaseId=$env.RELEASE_ID -DtargetEnvBaseUrl=$env.ROCKET_TARGET_URL -DtargetEnvUser=$env.ROCKET_TARGET_USER -DtargetEnvPassword=$env.ROCKET_TARGET_PASS -DtargetEnvTenant=$env.ROCKET_TARGET_TENANT -DimportPath=$env.ARCHIVE_PATH -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Set released') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:setAssetState -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DreleaseId=$env.RELEASE_ID -DassetVersionId=$env.ASSET_VERSION_ID -DassetState='Release' -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Lock origin') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:lockAssetVersion -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DreleaseId=$env.RELEASE_ID -DassetVersionId=$env.ASSET_VERSION_ID -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        stage('Lock target') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS'],
                        [$class: 'UsernamePasswordMultiBinding', credentialsId: "ROCKET_AUTH_CREDENTIALS_TARGET", usernameVariable: 'ROCKET_TARGET_USER', passwordVariable: 'ROCKET_TARGET_PASS']
                ]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:lockAssetVersion -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DtargetEnvBaseUrl=$env.ROCKET_TARGET_URL -DtargetEnvUser=$env.ROCKET_TARGET_USER -DtargetEnvPassword=$env.ROCKET_TARGET_PASS -DtargetEnvTenant=$env.ROCKET_TARGET_TENANT -DreleaseId=$env.RELEASE_ID -DassetVersionId=$env.ASSET_VERSION_ID -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }

        sleep(time: env.SLEEP_TIME, unit:"SECONDS")

        stage('Finalize release') {
            configFileProvider([configFile(fileId: env.MAVEN_SETTINGS_FILE, variable: 'MAVEN_SETTINGS')]) {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env.ROCKET_ORIGIN_CREDENTIALS, usernameVariable: 'ROCKET_USER', passwordVariable: 'ROCKET_PASS']]) {
                    sh "mvn -s $MAVEN_SETTINGS com.stratio.rocket:rocket-maven-plugin:${env.MAVEN_PLUGIN_VERSION}:finish -DrocketBaseUrl=$env.ROCKET_URL -Duser=$env.ROCKET_USER -Dpassword=$env.ROCKET_PASS -Dtenant=$env.ROCKET_TENANT -DreleaseId=$env.RELEASE_ID -DconnectTimeout=$env.CONNECT_TIMEOUT -DreadTimeout=$env.READ_TIMEOUT"
                }
            }
        }
    }
}
