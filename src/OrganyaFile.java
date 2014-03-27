/*
 * Org specs by J.J. Treadway (Wedge of Cheese)
	All integer values are unsigned little-endian.
	A "click" is the smallest unit of time in an org file.
	
	6 bytes: ascii string "Org-02" (this is the same regardless of which version of orgmaker you saved with)
	2 bytes: "wait" value (the length of a click in milliseconds)
	1 byte: beats per measure
	1 byte: clicks per beat
	4 bytes: position of the loop start, in clicks (the first click being position 0)
	4 bytes: position of the loop end, in clicks
	for each track:
	   2 bytes: "freq" value*
	   1 byte: instrument
	   1 byte: 1 if "pi" checkbox is checked, 0 otherwise*
	   2 bytes: number of resources
	for each track:
	   for each resource:
	      4 bytes: position of the resource, in clicks
	   for each resource:
	      1 byte: note (0=lowest note, 45=A440, 95=highest note, 255=no change)
	      1 byte: duration (in clicks, I believe this is ignored if note value is "no change")
	      1 byte: volume (0=silent, 200=default, 254=max, 255=no change)
	      1 byte: pan (0=full left, 6=center, 12=full right, 255=no change)
	
	 *Even though orgmaker only allows you to edit these for melody tracks, percussion tracks also have this data, 
		with the default values of freq=1000, pi=0. 
	 	I haven't tested to see if modifying these has any effect on playback.
 */
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class OrganyaFile {
	String filename;
	String headerStr;
	int waitVal;
	int beatPerMeasure;
	int clickPerBeat;
	int loopStartPos;
	int loopEndPos;
	OrgTrack[] trackArray;
	
	public static final int NUM_TRACK = 16;
	
	OrganyaFile(File inFile)
	{
		try {
			//get the name
			filename = inFile.getName();
			FileInputStream inStream = new FileInputStream(inFile);
			FileChannel inChan = inStream.getChannel();
			
			//read header
			ByteBuffer headerBuf = ByteBuffer.allocate(18);
			headerBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(headerBuf);
			headerBuf.flip();
			//get file tag
			byte[] headStringDat = new byte[6];
			headerBuf.get(headStringDat, 0, 6);
			headerStr = new String(headStringDat);
			if (!(headerStr.equals("Org-01") || headerStr.equals("Org-02") || headerStr.equals("Org-03")))
				throw new IOException("Illegal file header " + headerStr);
			//get other values
			waitVal = headerBuf.getShort();
			beatPerMeasure = headerBuf.get();
			clickPerBeat = headerBuf.get();
			loopStartPos = headerBuf.getInt();
			loopEndPos = headerBuf.getInt();
			
			//read track info
			trackArray = new OrgTrack[NUM_TRACK];
			for (int i = 0; i < NUM_TRACK; i++)
			{
				ByteBuffer trackBuf = ByteBuffer.allocate(6);
				trackBuf.order(ByteOrder.LITTLE_ENDIAN);
				inChan.read(trackBuf);
				trackBuf.flip();
				
				int freqVal = trackBuf.getShort();
				int instrumentNum = trackBuf.get();
				int piVal = trackBuf.get();
				int nEve = trackBuf.getShort();
				
				trackArray[i] = new OrgTrack(freqVal, instrumentNum, (piVal != 0), nEve);
			}
			//read event info
			for (int i = 0; i < NUM_TRACK; i++)
			{
				int[] posArray = new int[trackArray[i].numEvent];
				int[] valArray = new int[trackArray[i].numEvent];
				int[] durArray = new int[trackArray[i].numEvent];
				int[] volArray = new int[trackArray[i].numEvent];
				OrgTrackEvent[] eventArray = new OrgTrackEvent[trackArray[i].numEvent];
				//read positions
				for (int eve = 0; eve < trackArray[i].numEvent; eve++)
				{
					ByteBuffer pBuf = ByteBuffer.allocate(4);
					pBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.read(pBuf);
					pBuf.flip();
					posArray[eve] = pBuf.getInt();
				}
				//read note values
				for (int eve = 0; eve < trackArray[i].numEvent; eve++)
				{
					ByteBuffer eBuf = ByteBuffer.allocate(1);
					eBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.read(eBuf);
					eBuf.flip();
					valArray[eve] = eBuf.get();
				}
				//read note duration
				for (int eve = 0; eve < trackArray[i].numEvent; eve++)
				{
					ByteBuffer eBuf = ByteBuffer.allocate(1);
					eBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.read(eBuf);
					eBuf.flip();
					durArray[eve] = eBuf.get();
				}
				//read note volume
				for (int eve = 0; eve < trackArray[i].numEvent; eve++)
				{
					ByteBuffer eBuf = ByteBuffer.allocate(1);
					eBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.read(eBuf);
					eBuf.flip();
					volArray[eve] = eBuf.get();
				}
				//read note pan and add events
				for (int eve = 0; eve < trackArray[i].numEvent; eve++)
				{
					ByteBuffer eBuf = ByteBuffer.allocate(1);
					eBuf.order(ByteOrder.LITTLE_ENDIAN);
					inChan.read(eBuf);
					eBuf.flip();
					int nPan = eBuf.get();
					eventArray[eve] = new OrgTrackEvent(posArray[eve],
							valArray[eve],
							durArray[eve],
							volArray[eve],
							nPan);
				}
				trackArray[i].setEventArray(eventArray);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toString()
	{
		String retVal = "Organya file:\n\n";
		for (int i = 0; i < NUM_TRACK; i++)
		{
			retVal += "Track " + i + "\n";
			retVal += "\tInstrument #" + trackArray[i].instrumentNum + "\n";
			retVal += "\tFrequency: " + trackArray[i].freqVal + "\n";
			retVal += "\tPizzicato: ";
			if (trackArray[i].pi)
				retVal += "YES\n";
			else
				retVal += "NO\n";
			retVal += "\tEvents: " + trackArray[i].numEvent + "\n";
		}
		return retVal;
	}
	
	int getNumEvent()
	{
		int retVal = 0;
		for (int i = 0; i < NUM_TRACK; i++)
		{
			retVal += trackArray[i].numEvent;
		}
		return retVal;
	}
	
	public class OrgTrack {
		public int freqVal;
		public int instrumentNum;
		public boolean pi;
		public int numEvent;
		private OrgTrackEvent[] eventArray;
		
		OrgTrack(int freq, int inst, boolean isPi, int nEve)
		{
			freqVal = freq;
			instrumentNum = inst;
			pi = isPi;
			numEvent = nEve;
			eventArray = new OrgTrackEvent[nEve];
		}
		
		public void setEventArray(OrgTrackEvent[] arg) {eventArray = arg;}
		public OrgTrackEvent[] getEvents() {return eventArray;}
	}
	
	public class OrgTrackEvent {
		public int resourcePos;
		public short noteVal;
		public short noteLen;
		public short noteVol;
		public short notePan;
		
		OrgTrackEvent(int rPos, int nVal, int nLen, int nVol, int nPan)
		{
			resourcePos = rPos;
			noteVal = (short) (nVal & 0xFF);
			noteLen = (short) (nLen & 0xFF);
			noteVol = (short) (nVol & 0xFF);
			notePan = (short) (nPan & 0xFF);
		}
	}
}
