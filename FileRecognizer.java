package recognizer;

/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * A simple Sphinx-4 application that decodes a .WAV file containing
 * connnected-digits audio data. The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 * "${file_prompt}"
 * 
 * Set up the default eclipse jre to be the one included
 * 
 * Classpath lib order
 * JRE System Lib (custom 5.x version)
 * jl1.0.jar
 * tritonus.jar
 * tritonus_share.jar
 * tritonus_remaining.jar
 * tritonus_mp3.jar
 * sphinx4.jar
 * tools.jar
 * jsapi.jar
 * junit4.1.jar
 * javalayer.jar
 * corpora
 * sphinx4
 * 
 */
public class FileRecognizer {


    /**
     * Main method for running the WavFile demo.
     * 
     * 
     */
	public boolean convertedFile = false;
	
    public static void main(String[] args) {
        try {
            
            URL audioFileURL;
            
            if (args.length > 0) {
                audioFileURL = new File(args[0]).toURI().toURL();
            } else {
            	//if the ${file_prompt} isn't in the program arguments, it'll go with this:
                audioFileURL = FileRecognizer.class.getResource("");
            }
            URL configURL = FileRecognizer.class.getResource("config.xml");

            System.out.println("Loading Recognizer...\n");

            ConfigurationManager cm = new ConfigurationManager(configURL);

            Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();

            //System.out.println("Decoding " + audioFileURL.getFile());
            //System.out.println(AudioSystem.getAudioFileFormat(audioFileURL));

            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");

            AudioInputStream ais  = AudioSystem.getAudioInputStream(audioFileURL);
            
            FileRecognizer wavFile = new FileRecognizer();
			// Convert it to the proper format
			AudioFormat targetFormat =  
				
				new AudioFormat(16000f,
	                16,    // sample size in bits
	                1,     // mono
	                true,  // signed
	                true);
				
				//new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
			AudioInputStream convertedAis = wavFile.convertAudioInputStream(ais, targetFormat);
			File newFile = null;
			if (wavFile.convertedFile)
			{
			    newFile = wavFile.writeConvertedFile(convertedAis, audioFileURL.toString());
			    audioFileURL = newFile.toURI().toURL();
	            	    ais = AudioSystem.getAudioInputStream(audioFileURL);
			}
            
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());

            /* decode the audio file */
            Result result = recognizer.recognize();
            
            /* print out the results */
            if (result != null) {
                System.out.println("\nRESULT: " + 
                                   result.getBestFinalResultNoFiller() + "\n");
            } else {
                System.out.println("Result: null\n");
            }
            
            if (newFile != null)
            	newFile.delete();
            
        } catch (IOException e) {
            System.err.println("Problem when loading WavFile: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring WavFile: " + e);
            e.printStackTrace();
        } 
        catch (InstantiationException e) {System.err.println("Problem creating WavFile: " + e); e.printStackTrace();} 
        catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        }
    }
    
    private AudioInputStream convertAudioInputStream(AudioInputStream sourceAis, AudioFormat targetFormat) {
		AudioFormat baseFormat = sourceAis.getFormat();
		AudioFormat intermediateFormat;
		AudioInputStream convertedAis = sourceAis;
		
		// First convert the encoding, if necessary
		if (!baseFormat.getEncoding().equals(targetFormat.getEncoding())) {
			intermediateFormat = new AudioFormat(
					targetFormat.getEncoding(),
					baseFormat.getSampleRate(), baseFormat.getSampleSizeInBits(), baseFormat.getChannels(),
					baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
					false);
			convertedAis = AudioSystem.getAudioInputStream(intermediateFormat, sourceAis);
			//this.writeConvertedFile(convertedAis, "C:\\encoding.wav");
			baseFormat = intermediateFormat;
			sourceAis = convertedAis;
			convertedFile = true;
		}

		// Then convert the sample rate
		if (baseFormat.getSampleRate() != targetFormat.getSampleRate()) {
			intermediateFormat = new AudioFormat(
					baseFormat.getEncoding(),
					targetFormat.getSampleRate(), baseFormat.getSampleSizeInBits(), baseFormat.getChannels(),
					baseFormat.getChannels() * 2, targetFormat.getSampleRate(),
				false);
			convertedAis = AudioSystem.getAudioInputStream(intermediateFormat, sourceAis);
			//this.writeConvertedFile(convertedAis, "C:\\sample.wav");
			baseFormat = intermediateFormat;
			sourceAis = convertedAis;
			convertedFile = true;
		}
	
		// Then convert the number of channels
		if (baseFormat.getChannels() > targetFormat.getChannels()) {
			intermediateFormat = new AudioFormat(
					baseFormat.getEncoding(),
					baseFormat.getSampleRate(), baseFormat.getSampleSizeInBits(), targetFormat.getChannels(),
					targetFormat.getChannels() * 2, baseFormat.getSampleRate(),
					false);
			convertedAis = AudioSystem.getAudioInputStream(intermediateFormat, sourceAis);
			//this.writeConvertedFile(convertedAis, "C:\\channels.wav");
			baseFormat = intermediateFormat;
			sourceAis = convertedAis;
			convertedFile = true;
		}
		return convertedAis;
	}
    
	private File writeConvertedFile(AudioInputStream sourceAis, String fileName)
	{
		File tempfile = null;
		fileName = "tempwavfile.wav";
		//fileName = fileName.substring(6, fileName.length()-4) + "_new.wav";

		try
		{
			//This just takes an audio stream, writes it to disk, then plays it the way TALL usually does.
			//it's a test to see if the input stream is readable by the Java audio providers like Tritonus
			//System.out.println(fileName);
			tempfile = new File(fileName);
			AudioSystem.write(sourceAis, AudioFileFormat.Type.WAVE, tempfile);
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		return  tempfile;
	}

}
