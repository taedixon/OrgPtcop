
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.math.*;

public class PtcopFile {
	String header;
	final int unknownInt = 0x392;
	Vector blockVec;
	int beat;
	
	PtcopFile(OrganyaFile org)
	{
		header = "PTCOLLAGE-071119";
		blockVec = new Vector();
		
		//add master block
		beat = (org.beatPerMeasure*org.clickPerBeat) / 4;
		blockVec.add(new MasterV5Block(beat, 
				60000/(org.waitVal*4), 
				org.loopStartPos/(org.beatPerMeasure*org.clickPerBeat), 
				org.loopEndPos/(org.beatPerMeasure*org.clickPerBeat)));
		//add EventV5 block
		blockVec.add(new EventV5Block(org.trackArray));
		//add name block
		blockVec.add(new TextNAMEBlock(org.filename));
		//add comment block
		blockVec.add(new TextCOMMBlock("Converted with Noxid's org->ptcop conversion tool"));
		//add material block
		int woicAssign = 0;
		for (int i = 0; i < OrganyaFile.NUM_TRACK; i++)
		{
			if (org.trackArray[i].numEvent > 0)
			{
				if (i < 8) //melody
				{
					Object[] fArray = new Object[1];
					fArray[0] = new Integer(org.trackArray[i].instrumentNum);
					String voiceName = "ORG_M" + String.format("%02d", fArray) + ".wav";
					blockVec.add(new MatePCMBlock(voiceName));
					blockVec.add(new AssiWOICBlock(voiceName, woicAssign));
					woicAssign++;
				} else { //drams
					Object[] fArray = new Object[1];
					fArray[0] = new Integer(org.trackArray[i].instrumentNum);
					String voiceName = "ORG_D" + String.format("%02d", fArray) + ".wav";
					blockVec.add(new MatePCMBlock(voiceName));
					blockVec.add(new AssiWOICBlock(voiceName, woicAssign));
					woicAssign++;
				}
			}//if that track has any events
		}//for each possible track
		//add units block
		blockVec.add(new NumUnitBlock(woicAssign));
		woicAssign = 0;
		for (int i = 0; i < OrganyaFile.NUM_TRACK; i++)
		{
			if (org.trackArray[i].numEvent > 0)
			{
				if (i < 8) //melody
				{
					Object[] fArray = new Object[1];
					fArray[0] = new Integer(org.trackArray[i].instrumentNum);
					String voiceName = "ORG_M" + String.format("%02d", fArray) + ".wav";
					blockVec.add(new AssiUNITBlock(voiceName, woicAssign));
					woicAssign++;
				} else { //drams
					Object[] fArray = new Object[1];
					fArray[0] = new Integer(org.trackArray[i].instrumentNum);
					String voiceName = "ORG_D" + String.format("%02d", fArray) + ".wav";
					blockVec.add(new AssiUNITBlock(voiceName, woicAssign));
					woicAssign++;
				}
			}
		}
		//add end block
		blockVec.add(new PxtoneNDBlock());
	}
	
	public void saveToFile(File output)
	{
		FileOutputStream oFile;
		try {
			oFile = new FileOutputStream(output);
			FileChannel out = oFile.getChannel();
			Iterator it = blockVec.iterator();
			//write the header
			ByteBuffer hBuf = ByteBuffer.allocate(0x14);
			hBuf.order(ByteOrder.LITTLE_ENDIAN);
			hBuf.put(header.getBytes());
			hBuf.putInt(unknownInt);
			hBuf.flip();
			out.write(hBuf);
			//write all other blocks in order created
			while (it.hasNext())
				out.write(((FileBlock) it.next()).toBuf());
			//close
			out.close();
			oFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract class FileBlock {
		String tag;
		int len;
		
		abstract ByteBuffer toBuf();
	}
	
	protected class MasterV5Block extends FileBlock {
		//byte[] data;

		short unknown1 = 0x1E0;
		byte beat;
		short unknown2 = 0;
		short tempo;
		int repeat;
		int last;
		MasterV5Block(int b, int t, int r, int l)		
		{
			tag = "MasterV5";
			len = 15;
			beat = (byte) b;
			tempo = (short) (185.45 * Math.log(t) + 16243.57);
			repeat = r * beat * 0x1E0;
			last = l * beat * 0x1E0;
			//data = new byte[]{(byte) 0xE0, 0x01, 0x04, 0x00, 0x00, (byte) 0xF0, 0x42, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes());
			retVal.putInt(len);
			retVal.putShort(unknown1);
			retVal.put(beat);
			retVal.putShort(unknown2);
			retVal.putShort(tempo);
			retVal.putInt(repeat);
			retVal.putInt(last);
			retVal.flip();
			return retVal;
		}
	}
	protected class EventV5Block extends FileBlock {
		int nEvent;
		PtcopEvent[] eventArray;
		EventV5Block(OrganyaFile.OrgTrack[] trackArray)
		{
			tag = "Event V5";
			//OrganyaFile.OrgTrackEvent[] orgEventArray;
			//count events
			nEvent = 0;
			for (int i = 0; i < OrganyaFile.NUM_TRACK; i++)
			{
				nEvent += trackArray[i].numEvent;
			}
			//check if the file has events
			if (nEvent == 0)
			{
				eventArray = null;
				return;
			}
			//int[] panArray = {
			//		0x6, 0xE, 0x18, 0x20, 0x2A, 0x34,
			//		0x40, 0x4C, 0x54, 0x60, 0x6A, 0x72, 0x7C
			//};
			Vector ptEventVec = new Vector();
			//orgEventArray = new OrganyaFile.OrgTrackEvent[nEvent];
			int woicAssign = 0;
			for (int i = 0; i < OrganyaFile.NUM_TRACK; i++)
			{
				OrganyaFile.OrgTrackEvent[] trackEventArray = trackArray[i].getEvents();
				for (int j = 0; j < trackArray[i].numEvent; j++)
				{
					
					if (j == 0) //if this is the first event, set a VoiceNO event
					{
						ptEventVec.add(new PtcopEvent(0, woicAssign, 0xC, woicAssign));
					}
					if (trackEventArray[j].noteVal != 255)
					{
						//pitch event
						//duration event
						//TODO check if drams track and extendify the len?
						if (i <= 7){ //not drams
							System.out.println(trackEventArray[j].noteVal);
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									2,
									0x3F00 + trackEventArray[j].noteVal * 0x100));
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									1,
									(trackEventArray[j].noteLen) * 0x78));
						}
						else{ //probably drams?
							int orgPitch = 80 - trackEventArray[j].noteVal;
							int ptPitch;
							
							ptPitch = 0x7200 -  orgPitch*orgPitch*2125/1000;
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									2,
									ptPitch));
							
							//System.out.print(trackEventArray[j].noteVal);
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									1,
									(trackEventArray[j].noteLen + OrgPtcopConvApp.comboVal) * 0x78));
						}
						//velocity event
						ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
								woicAssign,
								4,
								0x68));
						//volume event
						if (trackEventArray[j].noteVol != 0x255)
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									5,
									(trackEventArray[j].noteVol * 3 / 7) +12));
						//pan event
						if (trackEventArray[j].notePan <= 12)
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									3,
									trackEventArray[j].notePan *0xB));

					} else {
						//just a volume/pan event
						if (trackEventArray[j].noteVol != 255)
						{
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									5,
									(trackEventArray[j].noteVol * 3 / 7) +12));
						}
						if (trackEventArray[j].notePan != 255)
						{
							ptEventVec.add(new PtcopEvent(trackEventArray[j].resourcePos,
									woicAssign,
									3,
									trackEventArray[j].notePan *0xB));
						}
					}
				}//for each event
				if (trackArray[i].numEvent > 0)
					woicAssign++;
			}//for each track
			//convert event vector to proper event array with relative event positioning
			nEvent = ptEventVec.size();
			eventArray = new PtcopEvent[nEvent];
			//Iterator eIt = ptEventVec.iterator();
			for (int i = 0; i < ptEventVec.size(); i++)
				eventArray[i] = (PtcopEvent)ptEventVec.get(i);
			//convert events from organya style to ptcop style
			sortEvents(eventArray);
			convertAbsToRelative(eventArray);
			//calculate length
			len = 4;
			for (int i = 0; i < eventArray.length; i++)
			{
				len += eventArray[i].calcSize();
			}
		}
		
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes());
			retVal.putInt(len);
			retVal.putInt(nEvent);
			for (int i = 0; i < eventArray.length; i++)
			{
				retVal.put(eventArray[i].toArray());
			}
			retVal.flip();
			return retVal;
		}
		
	public int isNegative(int x)
	{
		if(x<0){return -1;}
		else return 1;
			
	}
		public void sortEvents(PtcopEvent[] array)
		{
			for (int i = 0; i < array.length - 1; i++)
			{
				int smallestIndex = i;
				for (int j = i+1; j< array.length; j++)
				{
					if (array[j].posAbsolute < array[smallestIndex].posAbsolute)
						smallestIndex = j;
				}
				PtcopEvent tmp = array[i];
				array[i] = array[smallestIndex];
				array[smallestIndex] = tmp;
			}
		}
		public void convertAbsToRelative(PtcopEvent[] array)
		{
			long lastPos = 0;
			for (int i = 0; i < array.length; i++)
			{
				long posDif = array[i].posAbsolute - lastPos;
				array[i].posRelative = posDif * 0x78;
				lastPos = array[i].posAbsolute;
			}
		}
		protected class PtcopEvent {
			long posRelative;
			long posAbsolute;
			int unitID;
			int eventID;
			long eventVal;
			
			public int decodePxInt(byte[] array, int pos)
			{
				int v = array[pos];
				if (v > 0x7f)
					return v + 80*(decodePxInt(array, ++pos) - 1);
				else
					return v;
			}
			
			public int int2pxInt(long val, int pos, byte[] array)
			{
				/* wrong
				int bytes = 0;
				if(val >= 0x80)
				 bytes++;
				while(val>0xFF)
				{
				 bytes++;
				 val -= 0x80;
				}
			 	array[pos]=(byte)val;
				pos++;
				if ((bytes / 0x80) != 0)//Then call this again but with bytes
					pos = int2pxInt(bytes, pos, array);
				else if (bytes != 0) {
					if ((bytes > 1) && (bytes != 5))
						array[pos] = (byte) (bytes - 1);
					else
						array[pos] = (byte) (bytes);
					pos++;
				}
				return pos;
				/* more wrong
				long buffer = val & 0x7F;
				while ((val >>>= 7) != 0)
				{
					buffer <<= 8;
					buffer |= ((val & 0x7F) | 0x80);
				}
				
				while (true)
				{
					array[pos] = (byte) (buffer & 0xFF);
					pos++;
					if ((buffer & 0x80) != 0) {
						buffer >>= 8;
					} else {
						array[pos]++;
						break;
					}
				}
				return pos;
				*/
				if (val <= 0x7F) {
					array[pos] = (byte) val;
					return ++pos;
				} else {
					int fPos = int2pxInt(val/0x80, pos+1, array);
					array[pos] = (byte) (val - ((array[pos+1] & 0xFF)-1)*0x80);
					return fPos;
				}
			}
			PtcopEvent(long absPos, int unit, int ID, int val)
			{
				posAbsolute = absPos;
				unitID = unit;
				eventID = ID;
				eventVal = val;
				
				//pan error protection
			}
			public int calcSize()
			{
				int size = 0;
				byte[] dummyArray = new byte[10];
				size += int2pxInt(posRelative, 0, dummyArray);
				size += int2pxInt(unitID, 0, dummyArray);
				size += int2pxInt(eventID, 0, dummyArray);
				size += int2pxInt(eventVal, 0, dummyArray);
				return size;
			}
			public byte[] toArray()
			{
				byte[] retVal = new byte[calcSize()];
				int bytesWritten = 0;
				bytesWritten = int2pxInt(posRelative, 0, retVal);
				bytesWritten = int2pxInt(unitID, bytesWritten, retVal);
				bytesWritten = int2pxInt(eventID, bytesWritten, retVal);
				bytesWritten = int2pxInt(eventVal, bytesWritten, retVal);
				return retVal;
			}
		}
	}
	protected class TextNAMEBlock extends FileBlock {
		String name;
		TextNAMEBlock(String s)
		{
			name = s;
			tag = "textNAME";
			len = s.length();
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes());
			retVal.putInt(len);
			retVal.put(name.getBytes());
			retVal.flip();
			return retVal;
		}
	}
	protected class TextCOMMBlock extends FileBlock {
		String comment;
		TextCOMMBlock(String s)
		{
			tag = "textCOMM";
			comment = s;
			len = s.length();
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes());
			retVal.putInt(len);
			retVal.put(comment.getBytes());
			retVal.flip();
			return retVal;
		}
	}
	protected class MatePCMBlock extends FileBlock {
		short basicKey;
		int voiceFlags;
		short nChannel;
		short bitRate;
		int sampleRate;
		int keyCorrect;
		int nSample;
		short[] sampleArray;
		MatePCMBlock(String name)
		{
			//fill in basic data
			tag = "matePCM ";
			if (name.startsWith("ORG_M"))
				voiceFlags = 0x3;
			else
				voiceFlags = 0x2;
			basicKey = 69;
			keyCorrect = 0x3F800000;
			try {
				//construct an input stream from the resource
				InputStream resStream = OrgPtcopConvApp.class.getResourceAsStream("Organya22KHz8bit/" + name);
				//read the tag that says "RIFF" to get it out of the way
				byte[] riffTag = new byte[4];
				resStream.read(riffTag);
				//read the size of the data chunk
				byte[] chunkSizeDat = new byte[4];
				resStream.read(chunkSizeDat);
				int chunkSize = byteArray2Int(chunkSizeDat);
				byte[] fileData = new byte[chunkSize];
				resStream.read(fileData);
				//and put it into a bytebuffer for easy manipulation
				ByteBuffer wBuf = ByteBuffer.wrap(fileData);
				wBuf.order(ByteOrder.LITTLE_ENDIAN);
				//skip the tags
				//read the fmt block
				wBuf.position(8);
				int cksize = wBuf.getInt();
				short wFormatTag = wBuf.getShort();
				short nChannels = wBuf.getShort();
				nChannel = nChannels;
				int nSamplesPerSec = wBuf.getInt();
				sampleRate = nSamplesPerSec;
				int nAvgBytesPerSec = wBuf.getInt(); //ignore
				short nBlockAlign = wBuf.getShort(); 
				short wBitsPerSample = wBuf.getShort();
				bitRate = wBitsPerSample;
				short cbSize = -1;
				short wValidBitsPerSample = -1;
				int dwChannelMask = -1;
				byte[] subFormat = new byte[16];
				if (cksize >= 18)
				{
					cbSize = wBuf.getShort();
				}
				if (cksize >= 40)
				{
					wValidBitsPerSample = wBuf.getShort();
					dwChannelMask = wBuf.getInt();
					wBuf.get(subFormat);
				}
				switch (wFormatTag) {
				case 1: //WAVE_FORMAT_PCM
					String secTag;
					byte[] secTagDat;
					secTagDat = new byte[4];
					wBuf.get(secTagDat);
					secTag = new String(secTagDat);
					if (secTag.equals("LIST"))
					{
						int listLen = wBuf.getInt();
						byte[] listDat = new byte[listLen];
						wBuf.get(listDat); //ignore
						//read the next tag
						wBuf.get(secTagDat);
						secTag = new String(secTagDat);
					}
					if (secTag.equals("fact"))
					{
						int factLen = wBuf.getInt();
						byte[] factDat = new byte[factLen];
						wBuf.get(factDat);
						//lazy ass assuming fact is exactly 4 bytes
						nSample = byteArray2Int(factDat);
						//read the next tag
						wBuf.get(secTagDat);
						secTag = new String(secTagDat);
					}
					if (secTag.equals("data"))
					{
						//this should be the last chunk		
						//if (nSample == 0) //we were missing a fact chunk, so it should be just right there
						//{
							nSample = wBuf.getInt(); //or maybe it's always there???
						//}
						sampleArray = new short[nSample];
						if (wBitsPerSample == 8)
						{
							for (int i = 0; i < nSample; i++)
								sampleArray[i] = (short) (wBuf.get() & 0xFF);
						} else if (wBitsPerSample == 16) {
							for (int i = 0; i < nSample; i++)
								sampleArray[i] = wBuf.getShort();
						}
					}
					break;
				case 3: //WAVE_FORMAT_IEEE_FLOAT
					//ignore
					break;
				case 6: //WAVE_FORMAT_ALAW
					//ignore
					break;
				case 7: //WAVE_FORMAT_MULAW
					//ignore
					break;
				case (short) 0xFFFE: //WAVE_FORMAT_EXTENSIBLE
					break;
				default:
					throw new IOException("Invalid WAV format specifier " + cksize);
				}
				len = 24 + (bitRate/8 * nSample);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes(), 0, 8);
			retVal.putInt(len);
			retVal.putShort((short) 0);
			retVal.put((byte) 0);
			retVal.put((byte) basicKey);
			retVal.putInt(voiceFlags);
			retVal.putShort(nChannel);
			retVal.putShort(bitRate);
			retVal.putInt(sampleRate);
			retVal.putInt(keyCorrect);
			retVal.putInt(nSample);
			for (int i = 0; i < nSample; i++)
			{
				if (bitRate == 8)
				{
					retVal.put((byte)sampleArray[i]);
				} else {
					retVal.putShort(sampleArray[i]);
				}
			}
			retVal.flip();
			return retVal;
		}
	}
	protected class AssiWOICBlock extends FileBlock {
		int voiceNum;
		String voiceName;
		AssiWOICBlock(String name, int num)
		{
			tag = "assiWOIC";
			len = 20; //16 + 4
			voiceNum = num;
			voiceName = name;
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(len + 12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes(), 0, 8);
			retVal.putInt(len);
			retVal.putInt(voiceNum);
			int nByte = 0x10;
			if (voiceName.length() < nByte)
				nByte = voiceName.length();
			retVal.put(voiceName.getBytes(), 0, nByte);
			for (int i = nByte; i < 0x10; i++)
				retVal.put((byte)0);
			retVal.flip();
			return retVal;
		}
	}
	protected class NumUnitBlock extends FileBlock {
		int nUnit;
		NumUnitBlock(int n)
		{
			tag = "num UNIT";
			len = 4;
			nUnit = n;
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(16);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes(), 0, 8);
			retVal.putInt(len);
			retVal.putInt(nUnit);
			retVal.flip();
			return retVal;
		}
	}
	protected class AssiUNITBlock extends FileBlock {
		int unitNum;
		String unitName;
		AssiUNITBlock(String name, int num)
		{
			unitName = name;
			unitNum = num;
			tag = "assiUNIT";
			len = 20;
		}
		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(0x20);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes(), 0, 8);
			retVal.putInt(len);
			retVal.putInt(unitNum);
			int nByte = 0x10;
			if (unitName.length() < nByte)
				nByte = unitName.length();
			retVal.put(unitName.getBytes(), 0, nByte);
			for (int i = nByte; i < 0x10; i++)
				retVal.put((byte)0);
			retVal.flip();
			return retVal;
		}
	}
	protected class PxtoneNDBlock extends FileBlock {
		PxtoneNDBlock() 
		{
			tag = "pxtoneND";
			len = 0;
		}

		ByteBuffer toBuf() 
		{
			ByteBuffer retVal = ByteBuffer.allocate(12);
			retVal.order(ByteOrder.LITTLE_ENDIAN);
			retVal.put(tag.getBytes(), 0, 8);
			retVal.putInt(len);
			retVal.flip();
			return retVal;
		}
	}
	
	public int byteArray2Int(byte[] array)
	{
		int result = 0;
		int len = array.length;
		if (len > 4)
			len = 4;
		for (int i = 0; i < len; i++)
		{
			result += ((int)array[i] & 0xFF) << (i * 8);
		}
		return result;
	}
	
	public int getNumEvent()
	{
		Iterator it = blockVec.iterator();
		while (it.hasNext())
		{
			FileBlock nextBlock = (FileBlock) it.next();
			if (nextBlock.tag.equals("Event V5"))
			{
				return ((EventV5Block) nextBlock).nEvent;
			}
		}
		return -1;
	}
}
