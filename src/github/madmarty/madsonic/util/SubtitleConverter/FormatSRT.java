package github.madmarty.madsonic.util.SubtitleConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.util.Log;


/**
 * This class represents the .SRT subtitle format
 * <br><br>
 * Copyright (c) 2012 J. David Requejo <br>
 * j[dot]david[dot]requejo[at] Gmail
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * <br><br>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <br><br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 * @author J. David Requejo
 *
 */
public class FormatSRT implements TimedTextFileFormat {

	private static final String TAG = "FormatSRT";

	public TimedTextObject parseFile(String fileName, InputStream is) throws IOException {

		TimedTextObject tto = new TimedTextObject();
		Caption caption = new Caption();
		int captionNumber = 1;
		boolean allGood;

		//first lets load the file
		InputStreamReader in= new InputStreamReader(is);
		BufferedReader br = new BufferedReader(in);

		//the file name is saved
		tto.fileName = fileName;

		String line = br.readLine();
		int lineCounter = 0;
		
		try {
			while(line != null)
			{
				lineCounter++;
				line = line.trim();
				//if its a blank line, ignore it, otherwise...
				if (!line.isEmpty()){

					allGood = false;
					
					try {
						// Formatting error on some SRT files??
						if(captionNumber == 1)
							line = "1";
						
						int num = Integer.parseInt(line);
						if (num != captionNumber)
							throw new Exception();
						else {
							captionNumber++;
							allGood = true;
						}
					} catch (Exception e) {
						Log.i(TAG, captionNumber + " expected at line " + lineCounter);
					}
					
					if (allGood){
						//we go to next line, here the begin and end time should be found
						try {
							lineCounter++;
							line = br.readLine().trim();
							String start = line.substring(0, 12);
							String end = line.substring(line.length()-12, line.length());
							Time time = new Time("hh:mm:ss,ms",start);
							caption.start = time;
							time = new Time("hh:mm:ss,ms",end);
							caption.end = time;
						} catch (Exception e){
							Log.i(TAG, "incorrect time format at line "+lineCounter);
							allGood = false;
						}
					}
					if (allGood){
						//we go to next line where the caption text starts
						lineCounter++;
						line = br.readLine().trim();
						String text = "";
						while (!line.isEmpty()){
							text+=line+"<br />";
							line = br.readLine().trim();
							lineCounter++;
						}
						caption.content = text;
						int key = caption.start.mseconds;
						//in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
						while (tto.captions.containsKey(key)) key++;
						if (key != caption.start.mseconds)
							Log.i(TAG, "caption with same start time found...");
						//we add the caption.
						tto.captions.put(key, caption);
					}
					
					//we go to next blank
					while (!line.isEmpty()) {
						line = br.readLine().trim();
						lineCounter++;
					}
					caption = new Caption();
				}
				line = br.readLine();
			}

		}  catch (NullPointerException e){
			Log.i(TAG, "unexpected end of file, maybe last caption is not complete.");
		} finally{
	       // Close the reader
	       is.close();
	     }
		
		tto.built = true;
		return tto;
	}
}
