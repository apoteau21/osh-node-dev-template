/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2024 Ashley Poteau
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.simulated;

/**
 * @author Ashley Poteau
 * @since July 17, 2024
 *
 * giving the ppl language options to use!!
 * jk only english for now cuz i am not putting all those models on my laptop rn
 * but there are 20+ language options available here: https://alphacephei.com/vosk/models
 * also only picking small options for now
 *
 */
public enum LanguageModel {
    //only english for now, will add more later
    ENGLISH("C:\\Users\\ashley\\osh-node-dev-template\\sensors\\sensorhub-driver-vosk\\src\\main\\resources\\models\\vosk-eng");

    private String modelPath;

    LanguageModel(String modelPath) {
        this.modelPath = modelPath;
    }

    // return acoustic model path
    public String getModelPath() {
        return modelPath;
    }
}
