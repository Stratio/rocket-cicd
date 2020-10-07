#!groovy

def call(Map props = [:]) {

    //ansiColor('xterm') {

    if (props["assetType"] == "Workflow") {
        doWorkflow2envs(props)
    } else if (props["assetType"] == "MlModel") {
        doMLModel2envs(props)
    } else if (props["assetType"] == "MlFlowProject") {
        doMLProject2envs(props)
    } else if (props["assetType"] == "AutoMl") {
        doAutoML2envs(props)
    } else if (props["assetType"] == "MlTrainer") {
        doMLTrainer2envs(props)
    } else {
        error "Invalid asset type ${props['assetType']}"
    }
    //}
}
