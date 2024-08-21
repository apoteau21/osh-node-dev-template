/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2024 Ashley Poteau
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.simulated;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;

/**
 * @author Ashley Poteau
 * @since July 17, 2024
 * gonna actually have to make the other classes before i get to this one
 * things to configure
 * - wav file (check)
 * - comm settings??? maybe?
 * - source
 * - language (check)
 */
public class SpeechConfig {

    /**
     * Allows user to pick a WAV file to transcribe if wanted.
     */
    @DisplayInfo(label="WAV File", desc="Optional WAV File to process")
    public String wavFile = null;

    /**
     * Allows user to pick a specific language for speech transcription.
     * (English by default)
     */
    @DisplayInfo(label="Language", desc="Language model to use in speech transcription.")
    public LanguageModel languageModel = LanguageModel.ENGLISH;

    /**
     * Communication provider selection.
     */
    @DisplayInfo(desc="Communication settings to connect to data stream")
    public CommProviderConfig<?> commSettings;

    /**
     * type of audio info
     */
    @DisplayInfo(label="Source", desc="Source type of audio information")
    public SpeechRecognizerType speechRecognizerType = SpeechRecognizerType.STREAM;
}
