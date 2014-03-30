/**
 * 
 */
package com.andrewhebert.hebertmedia

import groovy.io.FileType

import java.text.SimpleDateFormat
import java.util.regex.Pattern
import java.util.Date;

import net.sourceforge.filebot.mediainfo.MediaInfo
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind

import org.apache.sanselan.Sanselan
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter
import org.apache.sanselan.formats.tiff.TiffField
import org.apache.sanselan.formats.tiff.TiffImageMetadata
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants
import org.apache.sanselan.formats.tiff.constants.TiffConstants
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory
import org.apache.sanselan.formats.tiff.write.TiffOutputField
import org.apache.sanselan.formats.tiff.write.TiffOutputSet

/**
 * @author Andy
 *
 */
class HebertMediaTransfer {
	
	enum SUP_F_EXT{avi,mov,jpg,jpeg,mpg,mpeg};
	
	enum DupStrategyEnum{INC,DEL};
	
	static DupStrategyEnum DUP_STRATEGY = DupStrategyEnum.DEL; 
	
	static File logFile = new File("U:\\logs\\HebertMediaTransfer.log");
	static{
		logFile.createNewFile();
	}
	static File issuesDir = new File("U:\\HebertMediaDropoffIssueFiles")
	//hour and a half
	static long procWait = 540000L;
	
	static main(args) {  
		def inputpath = args[0];
		
		def inputdirectory = new File(inputpath);
		
		def videooutputpath = args[1];
		
		def videooutputdirectory = new File(videooutputpath);
		
		def imageoutputpath = args[2];
		
		def imageoutputdirectory = new File(imageoutputpath);
		
		
		inputdirectory.eachFile(FileType.FILES) {
			def pathConvert = it.canonicalPath.toLowerCase();
			println("Processing: ${pathConvert}");
			Date now = new Date();
			String orgFileName = it.name;
			File orgFile = new File(it.canonicalPath);
			logFile << "BEGIN: ${now} for ${orgFileName}\n";
			File newFile = null;
			try{	
				if(pathConvert.endsWith(".avi")){
					newFile = mkAndGetAndyStylePath(it, videooutputdirectory, "mpg");
					convertAviVideo(it, newFile);				
					setLastModified(newFile, it);
					//it.delete();
					
				}else if(pathConvert.endsWith(".mov")){
					Date takenDate = getVideoFileDateTaken(it);
					newFile =  mkAndGetAndyStylePath(it, takenDate, videooutputdirectory, "mov");
					convertMovVideo(it,newFile);
					setLastModified(newFile, takenDate);
					// File newFile2 = mkAndGetAndyStylePathForRotatedVideos(it, takenDate, videooutputdirectory, "mpg");
					// convertMovVideoIfRotated(it, newFile2);
					// setLastModified(newFile2, takenDate);
				}
				else if(pathConvert.endsWith(".mp4")){
					Date takenDate = getVideoFileDateTaken(it);
					newFile = mkAndGetAndyStylePath(it, takenDate, videooutputdirectory, "mp4");
					convertMp4Video(it, newFile);
					setLastModified(newFile, takenDate);
				}
				else if(pathConvert.endsWith(".mpg")){
					//there is no date taken for mpeg metadata
					newFile = mkAndGetAndyStylePath(it, videooutputdirectory, "mpg");
					convertMpgVideo(it, newFile);
					setLastModified(newFile, it);
					//it.delete();
				}else if(pathConvert.endsWith(".jpg") || pathConvert.endsWith(".jpeg")){
					Date lastTakenDate = getJpgDateTaken(it);
					newFile = mkAndGetAndyStylePathForJpg(it, lastTakenDate, imageoutputdirectory, "jpg");
					processJpg(it, newFile);
					setLastModified(newFile, lastTakenDate)
					
				}else{
					logFile << "Unidentified File: ${pathConvert}\n";
					logFile << "END: ${orgFileName}\n\n";
					return;
				}
				
				println("Move to: ${newFile}");
				//it would be suspect if we didn't have a file greater than 1k that was created
				if(newFile == null || !newFile.exists() || newFile.size() < 1024){
					logFile << "There might have been an issue with ${it}.  Not > 1K. or newly created (${newFile}) doesn't exist: \n";
					
				}else{
					String delMsg = "Deleting ${it}: ";
					boolean worked = it.delete();
					delMsg = "${delMsg} ${worked}\n";
					logFile << delMsg;
				}
				
				
				
			}catch(Exception e){
				File issuesPlace = new File(issuesDir, orgFile.name);
				boolean worked = orgFile.renameTo(issuesPlace);
				logFile << "There was an issue with ${orgFile}, potential new place is ${newFile}, moving to issues dir: ${issuesPlace}, worked: ${worked}\n";
				logFile << "Exception: " + e.getMessage() + "\n";
				
				
				
			}
			logFile << "END: ${orgFileName}\n\n";
			

			
		}
	}
	
	static File mkAndGetAndyStylePath(File originalFile, Date dateTaken, File outputdirectory, String ext){
		File newDirectory = createPlace(originalFile, dateTaken, outputdirectory);
		
		File newFile = changeFileName(originalFile, newDirectory, ext);
		
		return newFile;
		
	}
	
	static File mkAndGetAndyStylePath(File originalFile, File outputdirectory, String ext){
		
		return mkAndGetAndyStylePath(originalFile, new Date(originalFile.lastModified()), outputdirectory, ext);
		
	}
	
	static File mkAndGetAndyStylePathForRotatedVideos(File originalFile, Date dateToUseToSetLastModified, File outputdirectory, String ext){
		File newDirectory = createPlaceForRotatedVideos(originalFile, dateToUseToSetLastModified, outputdirectory);
		
		File newFile = changeFileNameForRotatedVideo(originalFile, newDirectory, ext);
		
		return newFile;
		
	}
	
	static File mkAndGetAndyStylePathForJpg(File originalFile, Date dateToUseToSetLastModified, File outputdirectory, String ext){
		File newDirectory = createPlaceForJpg(originalFile, dateToUseToSetLastModified, outputdirectory);
		
		File newFile = changeFileName(originalFile, newDirectory, ext);
		
		return newFile;
		
	
	}
	
	
	static void setLastModifiedVideoSmart(File file, File originalFile){
		MediaInfo info = new MediaInfo();
		
		info.open(originalFile);
		
		String rotationStr = info.get(StreamKind.Video, 0, "Rotation",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		
	}
	
	
	static void setLastModified(File file, File originalFile){
		file.setLastModified(originalFile.lastModified());
	}
	
	static void setLastModified(File file, Date date){
		file.setLastModified(date.getTime());
	}
	
	
	
	static File changeFileName(File originalFile, File newDirectory, String ext){
		String newFileName = originalFile.name.replaceFirst(~/\.[^\.]+$/, ".${ext}");
		
		String newFilenamePath = newDirectory.canonicalPath + "/${newFileName}";
		
		File newFile = new File(newFilenamePath);
		
		//deal with file already being there
		if(newFile.isFile()){
			if(DUP_STRATEGY.equals(DupStrategyEnum.INC)){
				Pattern firstPartPattern = ~/.+\./;
				
				String firstPart = newFile.name.find(firstPartPattern);
				firstPart = firstPart.substring(0, firstPart.length() -1);		
				
				for(int i = 1; newFile.isFile(); i++){
					String firstNameInc = firstPart + " (${i}).${ext}";
					String pathInc = newDirectory.canonicalPath + "/${firstNameInc}";
					newFile = new File(pathInc);
					
				}
			}else if(DUP_STRATEGY.equals(DupStrategyEnum.DEL)){
				newFile.delete();
			}
						
		}
		
		return newFile;
	}
	
	static File changeFileNameForRotatedVideo(File originalFile, File newDirectory, String ext){
		File firstChangeFileNameFile = changeFileName(originalFile, newDirectory, ext);
		
		String newFileName = firstChangeFileNameFile.name.replaceFirst(~/\.[^\.]+$/, "-rotated.${ext}");
		
		String newFilenamePath = newDirectory.canonicalPath + "/${newFileName}";

		File newFile = new File(newFilenamePath);
		
		return newFile;
		
	}
	

	
	static File createPlace(File fileToConvert, Date dateTaken, File outputdirectory){
		logFile << "creating place for: ${fileToConvert}, last modified date is: ${dateTaken}\n";
		String year = dateTaken.format("yyyy");
		String month = dateTaken.format("MMMMM");
        String numericMonth = dateTaken.format("MM");
		
		String basePath = outputdirectory.canonicalPath;
		String newDirectoryPath = basePath + "/${year}/${numericMonth}-${month}";

		File newDirectory = new File(newDirectoryPath);
		newDirectory.mkdirs();
		logFile << "making dirs if needed: ${newDirectory}\n";
		
		return newDirectory

	}
	
	static File createPlaceForRotatedVideos(File fileToConvert, Date dateToUseForLastModified, File outputdirectory){

		logFile << "creating place for: ${fileToConvert}, last modified date is: ${dateToUseForLastModified}\n";
		String year = dateToUseForLastModified.format("yyyy");
		String month = dateToUseForLastModified.format("MMMMM");
		String numericMonth = dateToUseForLastModified.format("MM");
		
		String basePath = outputdirectory.canonicalPath;
		String newDirectoryPath = basePath + "/${year}/${numericMonth}-${month}";

		File newDirectory = new File(newDirectoryPath);
		newDirectory.mkdirs();
		logFile << "making dirs if needed: ${newDirectory}\n";
		
		return newDirectory

	}
	
	static File createPlaceForJpg(File fileToConvert, Date dateToUseForLastModified, File outputdirectory){
		Date aviOrgDate = new Date(dateToUseForLastModified.getTime());
		logFile << "creating place for: ${fileToConvert}, last modified date is: ${aviOrgDate}\n";
		String year = aviOrgDate.format("yyyy");
		String month = aviOrgDate.format("MMMMM");
		String numericMonth = aviOrgDate.format("MM");
		
		String basePath = outputdirectory.canonicalPath;
		String newDirectoryPath = basePath + "/${year}/${numericMonth}-${month}";

		File newDirectory = new File(newDirectoryPath);
		newDirectory.mkdirs();
		logFile << "making dirs if needed: ${newDirectory}\n";
		
		return newDirectory
		

	}
	
	
	static Date getVideoFileDateTaken(File fileToProcess){
		MediaInfo info = new MediaInfo();
		
		info.open(fileToProcess);
		//String format = "UTC 2012-09-20 17:19:31";
		String fmt = "zzz yyyy-MM-dd HH:mm:ss";  
		String dateStr = null;
		SimpleDateFormat sdf = new SimpleDateFormat(fmt);
		dateStr = info.get(StreamKind.General, 0, "Tagged date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		dateStr = info.get(StreamKind.General, 0, "Encoded date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		dateStr = info.get(StreamKind.Video, 0, "Tagged date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		dateStr = info.get(StreamKind.Video, 2, "Encoded date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		dateStr = info.get(StreamKind.Audio,2, "Tagged date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		dateStr = info.get(StreamKind.Audio, 0, "Encoded date",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name_Text);
		if(dateStr != null && !dateStr.isEmpty()){
			return sdf.parse(dateStr);
		}
		return new Date(fileToProcess.lastModified());
		
		
		
	}
	
	static Date getJpgDateTaken(File fileToProcess){
		
		JpegImageMetadata metadata = null;
		try{
			metadata = (JpegImageMetadata)Sanselan.getMetadata(fileToProcess);
		}
		catch(Exception e){
			logFile << "had an issue getting metadata: ${e} Will use modified date\n";
		}
		String dateStr;

			
		boolean isNotSet = (metadata == null ||
			metadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL) == null ||
			metadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getStringValue() == null ||
			metadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getStringValue().isEmpty());
		
		long modifiedTime = fileToProcess.lastModified();
		
		
				
		if(!isNotSet){
			String dateStrFromPic = metadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getStringValue();
			Date dateFromPic = Date.parse("yyyy:MM:dd HH:mm:ss", dateStrFromPic);
			isNotSet = dateFromPic.before(new Date("01/01/1990"));
			if(isNotSet){
				logFile << "found date of pic earlier than 1990. Assuming this is for a camera. Will use modified date\n";
			}
		}
		
		
		if(isNotSet){
			
				String dateTakenFormatStr = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date(modifiedTime));
				
				
				TiffOutputSet outputSet;
				if(metadata == null){
					outputSet = new TiffOutputSet();
				}else{
					TiffImageMetadata exif = metadata.getExif();
					if(exif == null){
						outputSet = new TiffOutputSet();
					}else{
						outputSet = metadata.getExif().getOutputSet();
					}
					
					
				}
				TiffOutputField dateTaken = new TiffOutputField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, TiffFieldTypeConstants.FIELD_TYPE_ASCII, dateTakenFormatStr.length(), dateTakenFormatStr.getBytes());
				TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
				// make sure to remove old value if present (this method will
				// not fail if the tag does not exist).
				exifDirectory.removeField(TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
				exifDirectory.add(dateTaken);
				byte [] saveFile = fileToProcess.readBytes();
				fileToProcess.delete();
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(fileToProcess));
				
				new ExifRewriter().updateExifMetadataLossless(saveFile, os, outputSet);
				
				fileToProcess.setLastModified(modifiedTime);
				
				os.close();
				dateStr = dateTakenFormatStr;
			}
			else{
				TiffField tiffField1 = metadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
				dateStr = tiffField1.getStringValue();
			}

		
		Date date = Date.parse("yyyy:MM:dd HH:mm:ss", dateStr);
		logFile << "process jpg exif org str - parsed date is ${date} \n";
		
		return date;
	}
	
	static void processJpg(File fileToProcess, File fileNewPath){

		boolean good = fileToProcess.renameTo(fileNewPath);
		
		if(!good){
			throw new RuntimeException("${fileNewPath} already exists");
		}
		logFile << "process jpg: status: ${good}, moving ${fileToProcess} to ${fileNewPath}\n";
	
		
		
		
	}
	
	static void convertAviVideo(File fileToConvert, File fileNewPath){
			
		 def mencoder = "mencoder -oac lavc -ovc lavc -of mpeg -mpegopts format=DVD:tsaf -vf scale=720:480,harddup -srate 48000 -af lavcresample=48000 -lavcopts vcodec=mpeg2video:vrc_buf_size=1835:vrc_maxrate=9800:vbitrate=9800:keyint=18:vstrict=0:acodec=ac3:abitrate=192:aspect=16/9 -ofps 30000/1001 -o \"${fileNewPath.canonicalPath}\" \"${fileToConvert.canonicalPath}\"";
		 
		 def mencoderProc = mencoder.execute();
		 mencoderProc.consumeProcessOutput();
		 mencoderProc.waitForOrKill(procWait);
		 
		 logFile << "process avi: converting ${fileToConvert} to ${fileNewPath}\n";
		 

	}
	
	/**
	 * This is turning out to be a huge pain
	 * Rotating to mpeg dimensions was a big pain in the butt, after iphone 3gs that went beyond 720x480
	 * 
	 * @param fileToConvert
	 * @param fileNewPath
	 */
	static void convertMovVideoIfRotated(File fileToConvert, File fileNewPath){
	
		MediaInfo info = new MediaInfo();
		
		info.open(fileToConvert);
		
		String rotationStr = info.get(StreamKind.Video, 0, "Rotation",  MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		boolean isANum = true;
		try{
			Double.parseDouble(rotationStr);
		}
		catch(NumberFormatException nfe){
			isANum = false;
		}catch(NullPointerException npe){
			isANum = false;
		}
		
		def encodercmd = null;
		if(rotationStr != null && !rotationStr.isEmpty() && isANum && Double.valueOf(rotationStr) > 0){
			Double rotateInDegrees = Double.valueOf(rotationStr);
			//don't care about losing precision
			Integer mencoderRotates = rotateInDegrees / 90;
			//TODO this doesn't work right stretch stuff out
			encodercmd = "mencoder -oac lavc -ovc lavc -of mpeg -mpegopts format=DVD:tsaf -vf rotate=${mencoderRotates},harddup,scale=720:480 -srate 48000 -af lavcresample=48000 -lavcopts vcodec=mpeg2video:vrc_buf_size=1835:vrc_maxrate=9800:vbitrate=5000:keyint=18:vstrict=0:acodec=ac3:abitrate=192 -ofps 30000/1001 -o \"${fileNewPath.canonicalPath}\" \"${fileToConvert.canonicalPath}\"";
			def encodercmdproc = encodercmd.execute();
			encodercmdproc.consumeProcessOutput();
			encodercmdproc.waitForOrKill(procWait);
			
			logFile << "process rotation mov: converting ${fileToConvert} to ${fileNewPath}\n";
			
		}		
		

		
		
		
		
	}
	
	static void convertMovVideo(File fileToConvert, File fileNewPath){
		fileToConvert.withInputStream { is ->
			fileNewPath << is
		  }
		  
					  
		  logFile << "process mov: moved ${fileToConvert} to ${fileNewPath}\n";
	}
	
	
	static void convertMp4Video(File fileToConvert, File fileNewPath){
		
			fileToConvert.withInputStream { is -> 
			  fileNewPath << is 
			}
			
						
			logFile << "process mp4: moved ${fileToConvert} to ${fileNewPath}\n";
				
	}
	
	static void convertMpgVideo(File fileToConvert, File fileNewPath){
		
			fileToConvert.withInputStream { is -> 
			  fileNewPath << is 
			}
			
						
			logFile << "process mpg: moved ${fileToConvert} to ${fileNewPath}\n";
				
	}

}
