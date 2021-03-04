#!groovy

def call(String promomtionParameters) {

    def promotion = readJSON text: promomtionParameters
    doPromotion(promotion)

}
