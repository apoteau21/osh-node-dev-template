package com.sample.impl.sensor.simulated;

//import org.vosk.LogLevel;
import org.vosk.Recognizer;
//import org.vosk.LibVosk;
import org.vosk.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * some code referenced from nick garay & vosk code demos on github
 * @author Ashley Poteau
 * @since July 18, 2024
 */
public class SpeechProcessor extends Thread {
    // set up logger
    private static final Logger logger = LoggerFactory.getLogger(SpeechProcessor.class);
    // thread name
    private static final String WORKER_THREAD_NAME = "STREAM-PROCESSOR";
    private final List<AudioTranscriptListener> listenerList = new ArrayList<>(); // ok so this is a separate class......
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final InputStream inputStream;

    private final SpeechRecognizerType speechRecognizerType;

    private Model model;
    public Recognizer recognizer;
    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
    private TargetDataLine microphone;


    public SpeechProcessor(SpeechRecognizerType speechRecognizerType, LanguageModel languageModel, InputStream inputStream) throws IOException {
        super(WORKER_THREAD_NAME);
        this.inputStream = inputStream;
        this.model = new Model(languageModel.getModelPath());
        this.recognizer = new Recognizer(model, 16000f);
        this.speechRecognizerType = speechRecognizerType;
    }

    // process based on audio input type
    public void processStream() throws IOException {
        if (speechRecognizerType == speechRecognizerType.STREAM) { // if wav file
            int nbytes;
            byte[] b = new byte[4096];

            while ((nbytes = inputStream.read(b)) >= 0) {
                System.out.println(nbytes);
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    System.out.println(recognizer.getResult());
                } else {
                    System.out.println(recognizer.getPartialResult());
                }
            }

            System.out.println(recognizer.getFinalResult());
        } else { // if live audio input
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                byte[] buffer = new byte[4096];
                while (processing.get()) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            System.out.println(recognizer.getResult());
                        } else {
                            System.out.println(recognizer.getPartialResult());
                        }
                    }
                }
                System.out.println("tbd");
            } catch (LineUnavailableException e) {
                logger.error("Microphone is unavailable: ", e);
            }
        }

        processing.set(true);
        start();
    }

    // cut the show....!
    public void stopProcessingStream() throws IOException {
        processing.set(false);
        recognizer.close();
        model.close();
    }

    @Override
    public void run() {
        logger.info("starting speech processor...");

       while (processing.get() && ((recognizer.getResult()) != null)) {

            logger.info("Hypothesis: {}\n", recognizer.getResult()); // output the rsults of the speech translation

            for(AudioTranscriptListener listener : listenerList) {
                if (listener != null) {
                    listener.onTranscribedAudio(recognizer.getResult());
                }
            }
        }

        logger.info("terminating speech processor...");
    }

    public void removeListener(AudioTranscriptListener listener) {
        listenerList.remove(listener);
    }

    public void addListener(AudioTranscriptListener listener) {
        if (listener != null && !listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

}
