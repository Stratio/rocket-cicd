#!groovy

def call(String promomtionParameters) {

    ansiColor('xterm') {
        def promotion = readJSON text: promomtionParameters
        println(promotion)
    }
}
