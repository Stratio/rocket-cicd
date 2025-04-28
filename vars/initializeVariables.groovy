def call(Map props = [:]) {
    return [
        def parameters = promotion["release"]["parameters"]
        def generalParams = parameters["General"]

        env.NODE = generalParams?.JENKINS_NODE_NAME ?: "jenkins-slave-mvn-jdk11"


        env.RELEASE_ID = promotion["releaseId"]

        env.PUBLIC_JENKINS_URL = generalParams?.PUBLIC_JENKINS_URL
        env.REPLACED_BUILD_URL = env.PUBLIC_JENKINS_URL ? env.BUILD_URL.replace(env.JENKINS_URL, PUBLIC_JENKINS_URL) : env.BUILD_URL


        env.ROCKET_URL: props["ROCKET_API_URL_DEV"]
        env.ROCKET_TENANT: props["ROCKET_TENANT_DEV"]

        env.ROCKET_TARGET_URL: props["ROCKET_API_URL_PRO"]
        env.ROCKET_TARGET_TENANT: props["ROCKET_TENANT_PRO"]
        env.TARGET_PROJECT_NAME = generalParams?.TARGET_PROJECT_NAME ?: ""



        def rocketConfig = [:] // Empty map to avoid null pointer exceptions

        try  {
            configFileProvider([configFile(fileId: 'rocketdict', variable: 'rocketdict')]) {
                // Read the config file, if it does not exist, use an empty dict
                 rocketConfig = readYaml file: rocketdict
            }
        } catch (e) {
            echo "Unable to get and parse rocketdict file from the Config File Provider plugin. Using default values..."
        }

        env.ROCKET_ORIGIN_CREDENTIALS_ID = rocketConfig.get(ROCKET_URL, 'rocket-auth-credentials')
        env.ROCKET_TARGET_CREDENTIALS_ID = rocketConfig.get(ROCKET_TARGET_URL, 'rocket-auth-credentials-target')
        env.MAVEN_SETTINGS_FILE = rocketConfig.get(MAVEN_SETTINGS, 'NexusMultiRepoSettings')

        env.ASSET_VERSION_ID: props["assetVersionId"]
        env.RELEASE_ID: props["releaseId"]
        env.ARCHIVE_PATH: "${BUILD_TAG}.zip"
        env.MAVEN_PLUGIN_VERSION: rocketConfig["MAVEN_PLUGIN_VERSION"] ? props["MAVEN_PLUGIN_VERSION"] ? props["MAVEN_PLUGIN_VERSION"] : "1.1.0-d3ffff0"
        env.CONNECT_TIMEOUT: props["CONNECT_TIMEOUT"] ? props["CONNECT_TIMEOUT"] : "2000"
        env.READ_TIMEOUT: props["READ_TIMEOUT"] ? props["READ_TIMEOUT"] : "10000"
        env.SLEEP_TIME = rocketConfig?[SLEEP_TIME] ? generalParams?.SLEEP_TIME ?: 4
}