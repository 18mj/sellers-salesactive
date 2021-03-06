package com.uni.sellers.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("commonFileUtils")
public class CommonFileUtils {
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Value("#{config['path.file']}")
	private String filePath;
	@Value("#{config['path.sampleFile']}")
	private String sampleFilePath;
	
	@Value("#{config['path.resultFile']}")
	private String resultFilePath;

	@Value("#{config['path.companyBizResultFile']}")
	private String companyBizResultFile;

	@Value("#{config['path.clientBizResultFile']}")
	private String clientBizResultFile;
	
	@Value("#{config['path.projectBizResultFile']}")
	private String projectBizResultFile;
	
	@Value("#{config['path.partnerCooperationFile']}")
	private String partnerCooperationFile;
	
	@Value("#{config['path.uploadFile']}")
	private String uploadFilePath;
	
	@Value("#{config['path.image']}")
	private String ImageFilePath;
	
	@Value("#{config['path.calUserInfo']}")
	private String calUserInfo;
	
	private final String serverCalFilePath = "/dbms/file/.calendar/";
	private String localOutlookInfoPath = "C:/sellers/calendar/";

	private static Object lockObj = new Object(); 
	
	public CommonFileUtils() {
	}
	
	public String getDirectoryName(String tableName){
		if(tableName == "BIZ_FILE_STORE"){
			tableName = "BIZ_FILE_STORE/";
		}else if(tableName == "CLIENT_COMPANY_INFO_FILE_STORE"){
			tableName = "CLIENT_COMPANY_INFO_FILE_STORE/";
		}else if(tableName == "CLIENT_EVENT_FILE_STORE"){
			tableName = "CLIENT_EVENT_FILE_STORE/";
		}else if(tableName == "OPPORTUNITY_FILE_STORE"){
			tableName = "OPPORTUNITY_FILE_STORE/";
		}else if(tableName == "CLIENT_ISSUE_FILE_STORE"){
			tableName = "CLIENT_ISSUE_FILE_STORE/";
		}else if(tableName == "CLIENT_PUNCHING_FILE_STORE"){
			tableName = "CLIENT_PUNCHING_FILE_STORE/";
		}else if(tableName == "PARTNER_SALES_LINAKGE_FILE_STORE"){
			tableName = "PARTNER_SALES_LINAKGE_FILE_STORE/";
		}else if(tableName == "RIVAL_COMPANY_FILE_STORE"){
			tableName = "RIVAL_COMPANY_FILE_STORE/";
		}else if(tableName == "PROPOSAL_FILE_STORE"){
			tableName = "PROPOSAL_FILE_STORE/";
		}else if(tableName == "BIZ_PROJECT_PLAN_FILE_STORE"){
			tableName = "BIZ_PROJECT_PLAN_FILE_STORE/";
		}
		return tableName;
	}

	public List<Map<String, Object>> insertFile(Map<String, Object> map, HttpServletRequest request,String fileTableName, String fileColumnName) throws Exception {
		String directoryName = getDirectoryName(fileTableName);
		
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		List<MultipartFile> multipartFileList = multipartHttpServletRequest.getFiles("fileModalUploadFile[]");
		String originalFileName = null;
		String originalFileExtension = null;

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> listMap = null;
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<HashMap<String, Object>> fileDatalist = new ArrayList<HashMap<String,Object>>(); 
		String jsonData = (map.get("fileData")).toString();		 
		fileDatalist = mapper.readValue(jsonData, new TypeReference<ArrayList<HashMap<String, String>>>() {});

		int filePK = Integer.parseInt(map.get("filePK").toString());
		String creatorId = (String) map.get("hiddenModalCreatorId");

		/*
		 * ????????? ?????? ??? ?????? ?????? ex) 201605
		 */
		String thismonth = CommonUtils.currentDate("yyyyMM")+"/";
		String filePathThisMonth = filePath + thismonth + directoryName;

		File file = new File(filePathThisMonth);
		if (file.exists() == false) {
			// ????????? ??????
			file.mkdirs();
		}
		int fileIndex = 0;
		for (MultipartFile multipartFile : multipartFileList) {
			//if (multipartFile.isEmpty() == false) {
			if (multipartFile.isEmpty() == false && !"false".equals(fileDatalist.get(fileIndex).get("useYN").toString())) {
				originalFileName = multipartFile.getOriginalFilename();
				originalFileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

				int i = 1; // ????????? ????????? ???????????? ?????? ??????
				int idx = originalFileName.lastIndexOf("."); // ???????????? *??????
				originalFileName = originalFileName.substring(0, idx); // originalFileName?????????
																		// ???????????????
																		// ??? ??????
																		// ????????????
																		// ??????.
				file = new File(filePathThisMonth + originalFileName + originalFileExtension);
				
				synchronized(lockObj){

					while (file.isFile()) { // ??????????????? ????????? ????????? true ????????? false
						i++;
						file = new File(filePathThisMonth + originalFileName + "(" + i + ")" + originalFileExtension);
					}
					if (i == 1) { // 1????????? ????????????.?????????
						file = new File(filePathThisMonth + originalFileName + originalFileExtension);
					} else if (i >= 2) { // 2????????? ?????? ????????????(i).?????????
						originalFileName = originalFileName + "(" + i + ")";
						
						file = new File(filePathThisMonth + originalFileName + originalFileExtension);
						FileUtils.touch(file);
					}
				}
				multipartFile.transferTo(file);

				listMap = new HashMap<String, Object>();
				listMap.put("filePK", filePK);
				listMap.put("originalFileName", originalFileName + originalFileExtension);
				listMap.put("originalFileExtension", originalFileExtension);
//				listMap.put("filePath", thismonth);
				listMap.put("filePath", thismonth + directoryName);
				listMap.put("creatorId", creatorId);
				listMap.put("fileTableName", fileTableName);
				listMap.put("fileColumnName", fileColumnName);

				list.add(listMap);
			}
			fileIndex = fileIndex + 1;
		}
		return list;
	}

	public void deleteFile(Map<String, Object> map, HttpServletRequest request) {
		log.debug("deleteFile call. ");
		
		File file = new File(filePath + map.get("FILE_PATH") + map.get("FILE_NAME"));
		if (file.exists()) {
			file.delete();
		}
	}

	public void deleteFileAll(List<Map<String, Object>> list, HttpServletRequest request) {
		for (int i = 0; i < list.size(); i++) {
			File file = new File(filePath + list.get(i).get("FILE_PATH") + list.get(i).get("FILE_NAME"));
			if (file.exists()) {
				file.delete();
			}
		}
	}

	public String downloadFile(Map<String, Object> map, HttpServletResponse response, HttpServletRequest request)throws Exception {
		String errorFlag = "0";
		try{
			
			byte fileByte[];
			if(map.get("FILE_PATH").toString().contains("organizationChart/") || map.get("FILE_PATH").toString().contains("photo/")){
				fileByte = FileUtils
						.readFileToByteArray(new File(ImageFilePath + map.get("FILE_PATH") + map.get("FILE_NAME")));
			}else{
				fileByte = FileUtils
						.readFileToByteArray(new File(filePath + map.get("FILE_PATH") + map.get("FILE_NAME")));
			}
	
			boolean ie = (request.getHeader("User-Agent").indexOf("MSIE") > -1)
					|| (request.getHeader("User-Agent").indexOf("Trident") > -1);
			String name;
			if (ie) {
				name = URLEncoder.encode((String) map.get("FILE_NAME"), "UTF-8").replaceAll("\\+", "%20");
			} else {
				name = new String(((String) map.get("FILE_NAME")).getBytes("UTF-8"), "ISO-8859-1");
			}
			response.setContentType("application/octet-stream");
			response.setContentLength(fileByte.length);
			response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
			response.setHeader("Content-Transfer-Encoding", "binary");
			response.getOutputStream().write(fileByte);
	
			response.getOutputStream().flush();
			response.getOutputStream().close();
		}catch(FileNotFoundException f){
			errorFlag = "-1";
			return errorFlag;
		}
		
		return errorFlag;
		
	}

	public int sampleDownloadFile(Map<String, Object> map, HttpServletResponse response, HttpServletRequest request)throws Exception {
		int result=0;
		String filePath = "";
		int delCnt =0;
		try{
			
			if(map.get("menuFlag").equals("companyBizFile")){		//??????/?????????
				filePath = companyBizResultFile;
			}else if(map.get("menuFlag").equals("clientBizFile")){	//???????????????
				filePath = clientBizResultFile;
			}else if(map.get("menuFlag").equals("projectBizFile")){	//??????????????????
				filePath = projectBizResultFile;
			}else if(map.get("menuFlag").equals("excelFile")){		//????????? ????????????
				delCnt = 1;
				filePath = resultFilePath;
			}else if(map.get("menuFlag").equals("partnerCooperationSalesFile")){	//?????????????????????_???????????????
				filePath = partnerCooperationFile;
			}else{
				delCnt = 1;
				filePath = resultFilePath;
			}
			 
//			byte fileByte[] = FileUtils.readFileToByteArray(new File(resultFilePath + map.get("sampleFileName")));
			byte fileByte[] = FileUtils.readFileToByteArray(new File(filePath + map.get("sampleFileName")));
			
			boolean ie = (request.getHeader("User-Agent").indexOf("MSIE") > -1)
					|| (request.getHeader("User-Agent").indexOf("Trident") > -1);
			String name;
			if (ie) {
				name = URLEncoder.encode((String) map.get("sampleFileName"), "UTF-8").replaceAll("\\+", "%20");
			} else {
				name = new String(((String) map.get("sampleFileName")).getBytes("UTF-8"), "ISO-8859-1");
			}
			response.setContentType("application/octet-stream");
			response.setContentLength(fileByte.length);
			response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
			response.setHeader("Content-Transfer-Encoding", "binary");
			response.getOutputStream().write(fileByte);

			response.getOutputStream().flush();
			response.getOutputStream().close();
		}catch(FileNotFoundException f){
			result = -1;
			return result;
		}
//		deleteExel(resultFilePath + map.get("sampleFileName").toString());
		
		if(delCnt == 1)
		{	// ?????? ????????? ?????? ??????????????? ?????? ??????
			deleteExel(filePath + map.get("sampleFileName").toString());
		}
		
		return result;
	}
	
	public void deleteExel(String filePath) {
		File delFile = new File(filePath);
		log.debug("====delete====");
	    if(delFile.exists()){
	    	boolean deleteFlag = delFile.delete();
	    	if(deleteFlag){
	    		log.debug("???????????? ??????.");
	    	}else{
	    		log.debug("?????? ?????? ??????.");
	    	}
	    }else{
	    	log.debug("????????? ???????????? ????????????.");
	    }
	}
	
	public void sampleExcelDownloadFile(Map<String, Object> map, HttpServletResponse response, HttpServletRequest request)throws Exception {
		byte fileByte[] = FileUtils.readFileToByteArray(new File(sampleFilePath + map.get("sampleFileName")));
		boolean ie = (request.getHeader("User-Agent").indexOf("MSIE") > -1)
				|| (request.getHeader("User-Agent").indexOf("Trident") > -1);
		String name;
		if (ie) {
			name = URLEncoder.encode((String) map.get("sampleFileName"), "UTF-8").replaceAll("\\+", "%20");
		} else {
			name = new String(((String) map.get("sampleFileName")).getBytes("UTF-8"), "ISO-8859-1");
		}
		response.setContentType("application/octet-stream");
		response.setContentLength(fileByte.length);
		response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
		response.setHeader("Content-Transfer-Encoding", "binary");
		response.getOutputStream().write(fileByte);
		
		response.getOutputStream().flush();
		response.getOutputStream().close();
		
		//?????? ?????? ????????? ?????? ??????
		deleteExel(sampleFilePath + map.get("sampleFileName").toString());
	}

	public Map<String, Object> insertPhoto(Map<String, Object> map, HttpServletRequest request, String fileTableName,String fileColumnName) throws Exception {
		String companyId = "";
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		List<MultipartFile> multipartFile = multipartHttpServletRequest.getFiles("fileModalUploadPhoto");
		
		if(fileTableName.equals("CLIENT_INDIVIDUAL_PHOTO_STORE")){
			companyId = (String) map.get("imageType") + "/client/" + (String) map.get("textCommonSearchCompanyId") + "/";
			multipartFile = multipartHttpServletRequest.getFiles((String) map.get("fileModalUploadType"));
		}else if(fileTableName.equals("PARTNER_INDIVIDUAL_PHOTO_STORE")){
			companyId = (String) map.get("imageType") + "/partner/" + (String) map.get("textCommonSearchCompanyId") + "/";
			multipartFile = multipartHttpServletRequest.getFiles((String) map.get("fileModalUploadType"));
		}else if(fileTableName.equals("CLIENT_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/client/";
		}else if(fileTableName.equals("PARTNER_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/partner/";
		}
		
		String originalFileName = null;
		String originalFileExtension = null;
		
		Map<String, Object> listMap = null;
		
		int filePK = Integer.parseInt(map.get("filePK").toString());
		String creatorId = (String) map.get("hiddenModalCreatorId");
		

		/*
		 * ????????? ?????? ??? ?????? ?????? ex) 201605
		 */
		/*
		 * String thismonth = CommonUtils.thisMonth(); String filePathThisMonth
		 * = filePath+thismonth;
		 */
		String filePathThisCompanyId = ImageFilePath + companyId;
		
		File file = new File(filePathThisCompanyId);
		if (file.exists() == false) {
			// ????????? ??????
			file.mkdirs();
		}
		if (multipartFile.isEmpty() == false) {
			originalFileName = multipartFile.get(0).getOriginalFilename();
			originalFileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

			int i = 1; // ????????? ????????? ???????????? ?????? ??????
			// int idx = originalFileName.lastIndexOf("."); // ???????????? *??????
			// originalFileName = originalFileName.substring(0, idx); //
			// originalFileName????????? ??????????????? ??? ?????? ???????????? ??????.
			originalFileName = map.get("filePK").toString();
			file = new File(filePathThisCompanyId + originalFileName + originalFileExtension);

			while (file.isFile()) { // ??????????????? ????????? ????????? true ????????? false
				i++;
				file = new File(filePathThisCompanyId + originalFileName + "(" + i + ")" + originalFileExtension);
			}
			if (i == 1) { // 1????????? ????????????.?????????
				file = new File(filePathThisCompanyId + originalFileName + originalFileExtension);
			} else if (i >= 2) { // 2????????? ?????? ????????????(i).?????????
				originalFileName = originalFileName + "(" + i + ")";
				file = new File(filePathThisCompanyId + originalFileName + originalFileExtension);

			}
			multipartFile.get(0).transferTo(file);

			listMap = new HashMap<String, Object>();
			listMap.put("filePK", filePK);
			listMap.put("originalFileName", originalFileName + originalFileExtension);
			listMap.put("originalFileExtension", originalFileExtension);
			listMap.put("filePath", companyId);
			listMap.put("creatorId", creatorId);
			listMap.put("fileTableName", fileTableName);
			listMap.put("fileColumnName", fileColumnName);

		}
		return listMap;
	}

	public void deletePhoto(Map<String, Object> map, HttpServletRequest request) {
		/*String companyId = "";
		
		if(map.get("fileTableName").equals("CLIENT_INDIVIDUAL_PHOTO_STORE")){
			companyId = "photo/client/" + (String) map.get("textCommonSearchCompanyId") + "/";
		}else if(map.get("fileTableName").equals("PARTNER_INDIVIDUAL_PHOTO_STORE")){
			companyId = "photo/partner/" + (String) map.get("textCommonSearchCompanyId") + "/";
		}else if(map.get("fileTableName").equals("CLIENT_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/client/";
		}else if(map.get("fileTableName").equals("PARTNER_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/partner/";
		}*/
		
		File file = new File(ImageFilePath + (String)map.get("FILE_PATH") + map.get("FILE_NAME"));
		if (file.exists()) {
			file.delete();
		}
	}

	public Map<String, Object> movePhoto(Map<String, Object> photo, Map<String, Object> map, HttpServletRequest request,String fileTableName, String fileColumnName) throws Exception {
		String companyId = "";
		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		List<MultipartFile> multipartFile = multipartHttpServletRequest.getFiles("fileModalUploadPhoto");
		
		if(map.get("fileTableName").equals("CLIENT_INDIVIDUAL_PHOTO_STORE")){
			companyId = (String) map.get("imageType") + "/client/" + (String) map.get("textCommonSearchCompanyId") + "/";
			multipartFile = multipartHttpServletRequest.getFiles((String) map.get("fileModalUploadType"));
		}else if(map.get("fileTableName").equals("PARTNER_INDIVIDUAL_PHOTO_STORE")){
			companyId = (String) map.get("imageType") + "/partner/" + (String) map.get("textCommonSearchCompanyId") + "/";
			multipartFile = multipartHttpServletRequest.getFiles((String) map.get("fileModalUploadType"));
		}else if(map.get("fileTableName").equals("CLIENT_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/client/";
		}else if(map.get("fileTableName").equals("PARTNER_COMPANY_INFO_FILE_STORE")){
			companyId = "organizationChart/partner/";
		}
		
		// ?????? ??????

		String photoFilePath = (String) photo.get("FILE_PATH");
		String photoFileName = (String) photo.get("FILE_NAME");
		File file = new File(ImageFilePath + photoFilePath + photoFileName);

		// end

		String originalFileName = null;
		String originalFileExtension = null;

		Map<String, Object> listMap = null;

		int filePK = Integer.parseInt(map.get("filePK").toString());
		String creatorId = (String) map.get("hiddenModalCreatorId");
		//String companyId = (String) map.get("hiddenModalCompanyId") + "/";

		/*
		 * ????????? ?????? ??? ?????? ?????? ex) 201605
		 */
		/*
		 * String thismonth = CommonUtils.thisMonth(); String filePathThisMonth
		 * = filePath+thismonth;
		 */
		String filePathThisCompanyId = ImageFilePath + companyId;

		File fileToMove = new File(filePathThisCompanyId);
		if (fileToMove.exists() == false) {
			// ????????? ??????
			fileToMove.mkdirs();
		}
		if (multipartFile.isEmpty() == false) {
			originalFileName = multipartFile.get(0).getOriginalFilename();
			originalFileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

			int i = 1; // ????????? ????????? ???????????? ?????? ??????
			// int idx = originalFileName.lastIndexOf("."); // ???????????? *??????
			// originalFileName = originalFileName.substring(0, idx); //
			// originalFileName????????? ??????????????? ??? ?????? ???????????? ??????.
			originalFileName = map.get("filePK").toString();
			fileToMove = new File(filePathThisCompanyId + originalFileName + originalFileExtension);

			while (fileToMove.isFile()) { // ??????????????? ????????? ????????? true ????????? false
				i++;
				fileToMove = new File(filePathThisCompanyId + originalFileName + "(" + i + ")" + originalFileExtension);
			}
			if (i == 1) { // 1????????? ????????????.?????????
				fileToMove = new File(filePathThisCompanyId + originalFileName + originalFileExtension);
			} else if (i >= 2) { // 2????????? ?????? ????????????(i).?????????
				originalFileName = originalFileName + "(" + i + ")";
				fileToMove = new File(filePathThisCompanyId + originalFileName + originalFileExtension);

			}
			// multipartFile.get(0).transferTo(fileToMove);
			boolean isMove = file.renameTo(fileToMove);
			log.debug("?????? ?????? : " + isMove);

			listMap = new HashMap<String, Object>();
			listMap.put("filePK", filePK);
			listMap.put("originalFileName", originalFileName + originalFileExtension);
			listMap.put("originalFileExtension", originalFileExtension);
			listMap.put("filePath", companyId);
			listMap.put("creatorId", creatorId);
			listMap.put("fileTableName", fileTableName);
			listMap.put("fileColumnName", fileColumnName);

		}
		return listMap;
	}

	///// ICS?????? ???????????? ??????
	public void downICS(Map<String, Object> map, HttpServletRequest request) throws Exception {
		
		log.debug("downICS Call.");
		
		// checkFilePath(request);
		// filePath = "C:/dev/workspace/sellers/src/main/webapp/ICS/";

		URL url;
		byte[] buf;

		url = new URL(map.get("downURL").toString());

		String calendar = (String) map.get("calendar");
		// String creatorId = (String)map.get("creatorId")+"/";
		String creatorId = (String) map.get("creatorId");

		// String filePathThisCreatorId = icsFilePath+creatorId; //??????
		String filePathThisCreatorId = calUserInfo + "google/in/"; // ??????

		File file = new File(filePathThisCreatorId);
		if (file.exists() == false) {
			// ????????? ??????
			file.mkdirs();
		}

		FileOutputStream fos = new FileOutputStream(filePathThisCreatorId + "."+ creatorId + ".ics");
		InputStream is = url.openStream();
		buf = new byte[1024];

		double len = 0;

		while ((len = is.read(buf)) > 0) {
			fos.write(buf, 0, (int) len);
		}

		fos.close();
		is.close();
		log.debug("???????????? ??????.");
	}

	/**
	 * google ????????? ???????????? Google ICS ?????? ??????.
	 * 
	 * @param map
	 * @throws Exception
	 */
	public void deleteFile(Map<String, Object> map, String calUserInfoPath) throws Exception {
		log.debug("deleteFile Call.");

		String creatorId 	= (String) map.get("hiddenModalCreatorId");
		String fileDir		= "";
		File file 			= null;
		
		if (map.get("calendarName").equals("????????? ?????????")) { //????????? ????????? ?????? ?????? ??????
			
			fileDir = calUserInfoPath + "outlook/" + "."+ creatorId + ".txt";
			file = new File(fileDir);
			
			if (file.exists()) {
				file.delete();
			}
			
		} else if ( map.get("calendarType").equals("3")) { //?????? ICS ?????? ??????
			
			fileDir = calUserInfoPath + "google/in/" + "."+ creatorId + ".ics";
			file = new File(fileDir);
			if (file.exists()) {
				file.delete();
			}
			
		} else if ( map.get("calendarType").equals("2")) { //Office 365 ????????? ?????? ?????? ??????.
			
			fileDir = calUserInfoPath + "office365/" + "."+ creatorId + ".txt";
			file = new File(fileDir);
			
			if (file.exists()) {
				file.delete();
			}
		}
	}

	/**
	 * ?????? ICS ?????? ????????? ????????? ?????? ????????????.
	 * 
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> getGoogleIcsEventList(Map<String, Object> map, String calUserInfoPath) throws Exception {

		log.debug("getGoogleIcsEventList Call.");

		String creatorId = (String) map.get("hiddenModalCreatorId");
		// ArrayList list = new ArrayList();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> recurList = new ArrayList<Map<String, Object>>();
		String fileDir = calUserInfoPath + "google/in/" + "." + creatorId + ".ics";

		File file = new File(fileDir);

		if (file.exists()) {

			BufferedReader in = new BufferedReader(new FileReader(fileDir));
			Map<String, Object> icsMap = new HashMap<String, Object>();
			
			String line 		= null;
			String str 			= "";
			String location 	= "";
			String description 	= "";
			String summary 		= "";
			String dtStart 		= "";
			String dtEnd 		= "";
			String rrule 		= ""; // ????????????
			String exDate		= ""; // ????????????
			String uid			= ""; // uid ?????????
			String recur_id		= ""; // ???????????????
			
			try {
				while ((line = in.readLine()) != null) {
					str = line;
					
					if (str.matches(".*UID.*")) {
						uid = str;
						uid = uid.substring(uid.indexOf(":")+1);
					}
					
					if (str.matches(".*RECURRENCE-ID.*")) {
						Map<String, Object> recurMap = new HashMap<String, Object>();
						recur_id = str;
						recur_id = recur_id.substring(recur_id.indexOf(":")+1);
						
						SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd");
						SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd");
						
						Date original_date = original_format.parse(recur_id);
						recur_id = new_format.format(original_date);
						
						recurMap.put("uid", uid);
						recurMap.put("recur_id", recur_id);
						recurList.add(recurMap);
					}
					
					if (str.matches(".*DTSTART.*")) {
						dtStart = str;
						dtStart = dtStart.substring(dtStart.indexOf(":") + 1);
						
						//icsMap.put("DTSTART", dtStart);
						
						//????????? - ???
						if(dtStart.length()==8){ //??????O
							icsMap.put("allDay", "1"); //??????O
							icsMap.put("START_DAY", dtStart);
							
							SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd");
							SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd");
							
							Date original_date = original_format.parse(dtStart);
							dtStart = new_format.format(original_date);
							
							icsMap.put("start", dtStart);
						}else{ //??????X
							icsMap.put("allDay", "0"); //??????X
							
							if(dtStart.length()==15){ //????????? ?????? ????????? ????????? ??????
								SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd'T'hhmmss");
								SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
								SimpleDateFormat new_format2 = new SimpleDateFormat("yyyyMMdd");
								TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
								original_format.setTimeZone(timeZone);
								
								Date original_date = original_format.parse(dtStart);
								dtStart = new_format.format(original_date);
								
								icsMap.put("START_DAY", new_format2.format(original_date));
								icsMap.put("start", dtStart);
							}else if(dtStart.length()==16){ //????????? ?????? Z??? ????????? ??????
								SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd'T'hhmmss'Z'");
								SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
								SimpleDateFormat new_format2 = new SimpleDateFormat("yyyyMMdd");
								TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
								original_format.setTimeZone(timeZone);
								
								Date original_date = original_format.parse(dtStart);
								dtStart = new_format.format(original_date);
								
								icsMap.put("START_DAY", new_format2.format(original_date));
								icsMap.put("start", dtStart);
							}
						}
						//?????????  - ??? 
						
						continue;
					}

					if (str.matches(".*DTEND.*")) {
						dtEnd = str;
						dtEnd = dtEnd.substring(dtEnd.indexOf(":") + 1);

						//icsMap.put("DTEND", dtEnd);
						
						//????????? - ???
						if(dtEnd.length()==8){ //??????O
							icsMap.put("allDay", "1"); //??????O
							icsMap.put("END_DAY", dtEnd);
							
							SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd");
							SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd");
							
							Date original_date = original_format.parse(dtEnd);
							dtEnd = new_format.format(original_date);
							
							icsMap.put("end", dtEnd);
						}else{ //??????X
							icsMap.put("allDay", "0"); //??????X
							
							if(dtEnd.length()==15){ //????????? ?????? ????????? ????????? ??????
								SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd'T'hhmmss");
								SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
								SimpleDateFormat new_format2 = new SimpleDateFormat("yyyyMMdd");
								TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
								original_format.setTimeZone(timeZone);
								
								Date original_date = original_format.parse(dtEnd);
								dtEnd = new_format.format(original_date);
								
								icsMap.put("END_DAY", new_format2.format(original_date));
								icsMap.put("end", dtEnd);
							}else if(dtEnd.length()==16){ //????????? ?????? Z??? ????????? ??????
								SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd'T'hhmmss'Z'");
								SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
								SimpleDateFormat new_format2 = new SimpleDateFormat("yyyyMMdd");
								TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
								original_format.setTimeZone(timeZone);
								
								Date original_date = original_format.parse(dtEnd);
								dtEnd = new_format.format(original_date);
								
								icsMap.put("END_DAY", new_format2.format(original_date));
								icsMap.put("end", dtEnd);
							}
						}
						//????????? - ???
						
						continue;
					}

					if (str.matches(".*DESCRIPTION.*")) {
						description = str;
						description = description.substring(description.indexOf(":") + 1);

						//icsMap.put("DESCRIPTION", description);
						icsMap.put("EVENT_DETAIL", description);
						continue;
					}
					if (str.matches(".*LOCATION.*")) {
						location = str;
						location = location.substring(location.indexOf(":") + 1);

						icsMap.put("LOCATION", location);
						continue;
					}
					if (str.matches(".*SUMMARY.*")) {
						summary = str;
						summary = summary.substring(summary.indexOf(":") + 1);

						//icsMap.put("SUMMARY", summary);
						icsMap.put("title", summary);
					}

					if (str.matches(".*RRULE.*")) {
						rrule = str;
						rrule = rrule.substring(rrule.indexOf(":") + 1);
						
						String endRuleType = "";
						
						if(rrule.matches(".*UNTIL=.*")){
							endRuleType = "until";
							
							String endRule = rrule.substring(rrule.indexOf("UNTIL=") + 6);
							endRule = endRule.substring(0, endRule.indexOf(";"));
							
							SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd");
							SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd");
							TimeZone timeZone = TimeZone.getTimeZone("Asia/Seoul");
							original_format.setTimeZone(timeZone);
							
							Date original_date = original_format.parse(endRule);
							endRule = new_format.format(original_date);
							
							icsMap.put("RECURRENCE_END_DATE", endRule);
						}else if(rrule.matches(".*COUNT=.*")){
							endRuleType = "count";

							String endRule = rrule.substring(rrule.indexOf("COUNT=") + 6);
							endRule = endRule.substring(0, endRule.indexOf(";"));
							
							icsMap.put("RECURRENCE_COUNT", endRule);
						}else{
							endRuleType = "loop";
						}
						icsMap.put("END_RULE", endRuleType);
						icsMap.put("RECURRENCE_RULE", rrule);
						icsMap.put("REPEAT_YN", "Y");
					}
					//??? ?????????
					if (str.matches(".*EXDATE.*")) {
						String orDate = str;
						orDate = orDate.substring(orDate.indexOf(":") + 1);
						
						SimpleDateFormat original_format = new SimpleDateFormat("yyyyMMdd");
						SimpleDateFormat new_format = new SimpleDateFormat("yyyy-MM-dd");
						
						Date original_date = original_format.parse(orDate);
						orDate = new_format.format(original_date);
						
						if(exDate != null && exDate != ""){
							exDate += ","+orDate;
						}else{
							exDate = orDate;
						}
						//?????? END:VEVENT?????? ????????? uid??? ????????? ?????? ??? ?????? ??????.
					}

					if (str.matches("END:VEVENT")) {
						
						if(icsMap.get("REPEAT_YN") == null){
							icsMap.put("REPEAT_YN", "N");
						}
						
						if(rrule != null && rrule != ""){
							for(int i=0; i < recurList.size(); i++){
								if(recurList.get(i).get("uid").toString().equals(uid)){
									if(exDate != null && exDate != ""){
										exDate += ","+recurList.get(i).get("recur_id").toString();
									}else{
										exDate = recurList.get(i).get("recur_id").toString();
									}
								}
								recurList.remove(i);
								i = i-1;
							}
							icsMap.put("EX_DATE", exDate);
						}
						
						list.add(icsMap);
						icsMap = new HashMap<String, Object>();
					}
				}

				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/**
	 * ????????? ???????????? ICS ????????? ?????????.
	 * 
	 * @param list
	 * @param map
	 * @throws Exception
	 */
	public void outIcsEventList(List<Map<String, Object>> list, Map<String, Object> map) throws Exception {

		log.debug("outIcsEventList Call.");

		String beginVcal 		= "BEGIN:VCALENDAR\n";
		String prodid 			= "PRODID:-//Unipoint Corporation//sellers MIMEDIR//EN\n";
		String version 			= "VERSION:2.0\n";
		String method 			= "METHOD:PUBLISH\n";

		String beginVtime 		= "BEGIN:VTIMEZONE\n";
		String tzid 			= "TZID:Korea Standard Time\n";

		String beginStandard 	= "BEGIN:STANDARD\n";
		String tzoffSetFrom 	= "TZOFFSETFROM:+0900\n";
		String tzoffSetTo 		= "TZOFFSETTO:+0900\n";
		String endStandard 		= "END:STANDARD\n";
		String endVtimeZone 	= "END:VTIMEZONE\n";

		// body start
		String eventBegin 		= "BEGIN:VEVENT\n";
		String classPublic 		= "CLASS:PUBLIC\n";

		String location 		= "LOCATION:\n";
		String sequence 		= "SEQUENCE:0\n";
		String status 			= "STATUS:CONFIRMED\n";

		String tranSp 			= "TRANSP:QPAQUE\n";
		String eventEnd 		= "END:VEVENT\n";
		// end

		String calEnd 			= "END:VCALENDAR";

		try {

			String creatorId = (String) map.get("creatorId");
			String filePathThisCreatorId = serverCalFilePath + "google/out/" + "." + creatorId + ".ics";
			File icsFile = new File(filePathThisCreatorId);

			if (icsFile.exists() == false) {
				// ????????? ??????
				icsFile.mkdirs();
			}

			// File icsFile = new
			// File("C:/Users/shJang/Desktop/sellers/basic.ics");

			if (icsFile.exists()) {
				icsFile.delete();
			}

			BufferedWriter fw = new BufferedWriter(new FileWriter(icsFile, true));

			fw.write(beginVcal);
			fw.write(prodid);
			fw.write(version);
			fw.write(method);
			fw.write(beginVtime);
			fw.write(tzid);
			fw.write(beginStandard);
			fw.write(tzoffSetFrom);
			fw.write(tzoffSetTo);
			fw.write(endStandard);
			fw.write(endVtimeZone);

			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					String startDate 	= "DTSTART:";
					String endDate 		= "DTEND:";
					String created 		= "CREATED:";
					String description 	= "DESCRIPTION:";
					String lastModified = "LAST-MODIFIED:";
					String summary 		= "SUMMARY:";
					String repeatRule 	= "RRULE:";

					SimpleDateFormat new_format = new SimpleDateFormat("yyyyMMddkkmmss");
					String new_date = new_format.format(list.get(i).get("START_DATETIME"));
					String yymmdd = new_date.substring(0, 8);
					String kkmmss = new_date.substring(8, 14);
					String startDateTime = yymmdd + "T" + kkmmss;

					// startDate += list.get(i).get("START_DATETIME");
					startDate 		+= startDateTime;
					endDate 		+= list.get(i).get("END_DATETIME");
					created 		+= list.get(i).get("SYS_REGISTER_DATE");
					description 	+= list.get(i).get("EVENT_DETAIL");
					lastModified 	+= list.get(i).get("SYS_UPDATE_DATE");
					summary 		+= list.get(i).get("EVENT_SUBJECT");

					fw.write(eventBegin);
					fw.write(classPublic);

					fw.write(startDate + "\n");
					fw.write(endDate + "\n");
					fw.write(created + "\n");
					if (list.get(i).get("REPEAT_YN").equals("Y")) {
						repeatRule += list.get(i).get("RECURRENCE_RULE") + "\n";
						fw.write(repeatRule);
					}
					fw.write(description + "\n");
					fw.write(lastModified + "\n");

					fw.write(location);
					fw.write(sequence);
					fw.write(status);

					fw.write(summary + "\n");

					fw.write(tranSp);
					fw.write(eventEnd);

					fw.flush();

				}
			}

			fw.write(calEnd);
			fw.flush();

			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ??????????????? ?????? ????????????, ID/PASSWORD ??????????????????, ????????? ?????? ??????.
	 * @param filePath
	 */
	public void deleteIncorrectUserLoginInfo(String filePath){
		File file 	= new File(filePath);
		
		if(file.exists()){
			file.delete();
		}
	}
	
	/**
	 * ????????? ????????? ID, PASSWORD??? ?????? ????????? ????????? Check ???????????? ????????? ????????? ??? ?????? ID, PASSWORD
	 * ???????????? ????????????. ID, PASSWORD ?????? ????????????.
	 * 
	 * @param map
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> readOutlookInfoFile(Map<String, Object> map, List<Map<String, Object>> list, String calType, String userFilePath)
			throws Exception {

		log.debug("readOutlookInfoFile Call.");
		
		String serverUrl	= "outlook/";
		String fileDir		= "";
		String fileTmp		= "";
//		String creatorId	= (String) map.get("hiddenModalCreatorId");
//		String creatorId	= (String) map.get("hiddenUserId");
		String creatorId	= (String) map.get("MEMBER_ID_NUM");
//		String fileDir 		= serverCalFilePath + "outlook/" + "." + creatorId + ".txt"; 		// ??????????????????(?????????????????????)
//		String fileTmp 		= serverCalFilePath + "outlook/" + "." + creatorId + "Tmp.txt";		// ????????? ?????? (?????? ??? ??????)
		Map<String, Object> outlookMap = new HashMap<String, Object>();
		
		/*
		if (calType.equals("outlook")) {
			serverUrl = calType+"/";
		}else if (calType.equals("office365")){
			serverUrl = calType+"/";
		}
		*/
		
		serverUrl = calType+"/";
		
		fileDir	= userFilePath + serverUrl + "." + creatorId + ".txt"; 		// ??????????????????(?????????????????????)
		
		outlookMap.put("userLoginInfoFilePath", fileDir);
		
		fileTmp	= userFilePath + serverUrl + "." + creatorId + "Tmp.txt";		// ????????? ?????? (?????? ??? ??????)
		File file 	= new File(fileDir);
		File tmp 	= new File(fileTmp); // ????????? ?????? ?????? ??????
		
		if (file.exists()) {
			// log.debug("===========???????????? ?????? ??????(????????? ?????? ??????)===========");
			log.debug( serverUrl + " File Decryption start" );
			FileReader fr = null;
			FileWriter fw = null;
			try {

				String fileName 	= fileDir; 		// ???????????? ??????(????????? ?????? ??????)
				String fileNameTmp 	= fileTmp; 		// ????????? ?????? ??????

				fr = new FileReader(fileName); 		// ????????? ?????? ?????? ????????? ?????? ??????.
				fw = new FileWriter(fileNameTmp); 	// ????????? ?????? ?????? ?????? ?????? ??????.

				int secret 	= 3;
				int data 	= 0;
				log.debug("::Decryption Start");
				while ((data = fr.read()) != -1) {
					data -= secret; 			// ?????????
					char ch = (char) data;
					//System.out.print(ch); 	// ????????? ??????
					fw.write(data); 			// ???????????? ???????????? ????????????.
				} // while
				log.debug(" ::Decryption End");
				// fw.flush();
				// fw.close();
				// fr.close();
			}
			// catch (FileNotFoundException abc)
			// {
			// abc.printStackTrace();
			// System.out.println("????????? ??????????????? ????????? ????????????.");
			// }
			catch (IOException ee) {
				ee.printStackTrace();
				log.debug("????????? ???????????? ????????? ????????????.");
			} finally {
				fw.close();
				fr.close();
			}

			log.debug(serverUrl + " File Decryption End");

			String line 		= null;
			String str 			= "";
			String outlookId 	= "";
			String outlookPw 	= "";
			String creatorNum 	= "";
			String serverNm		= "";

			// BufferedReader in = new BufferedReader(new FileReader(fileDir));
			BufferedReader in = new BufferedReader(new FileReader(fileTmp));

			while ((line = in.readLine()) != null) {
				str = line;

				if (str.matches(".*outlookId.*")) {
					outlookId = str;
					outlookId = outlookId.substring(outlookId.indexOf(":") + 1);

					outlookMap.put("outlookId", outlookId);
				}

				if (str.matches(".*outlookPw.*")) {
					outlookPw = str;
					outlookPw = outlookPw.substring(outlookPw.indexOf(":") + 1);

					outlookMap.put("outlookPw", outlookPw);
				}

				if (str.matches(".*creatorId.*")) {
					creatorNum = str;
					creatorNum = creatorNum.substring(creatorNum.indexOf(":") + 1);

					outlookMap.put("creatorId", creatorNum);
				}
				
				if (str.matches(".*serverNm.*")) {
					serverNm = str;
					serverNm = serverNm.substring(serverNm.indexOf(":") + 1);
					
					outlookMap.put("serverNm", serverNm);
				}
			}

			list.add(outlookMap);

			in.close();

			if (tmp.exists()) {
				// System.out.println("===========???????????? ?????? ??????(????????? ????????????)===========");
				tmp.delete();
			}
		} else {
			// ?????????????????? ?????????????????? ?????????
			log.debug("Outlook Login Fail");
			outlookMap.put("errStatus", "2");
			
			outlookMap.put("errMsg", "??????????????? ?????? ??????????????? ?????????????????????. \n?????? ????????? ????????????.");
			list.add(outlookMap);
		}
		
		return list;
	}

	/**
	 * ????????? ID, PASSWORD ??? .txt ????????? ????????? ??? ?????? ????????? ????????? ??????.
	 * 
	 * @param map
	 * @param outlookID
	 * @param outlookPW
	 * @throws Exception
	 */
	public void outlookInfoSave(String creatorId, String outlookID, String outlookPW, String flag, String userFilepath) throws Exception {

		log.debug("outlookInfoSave Call. ");

		File infoSaveDir 	= null;
		File infoFile 		= null;
		String outlookId 	= "outlookId:";
		String outlookPw 	= "outlookPw:";
		String creatorNum 	= "creatorId:";
		String serverName	= "serverNm:";
		String exServer		= "";
		if(flag.equals("outlook")){
			exServer = flag+"/";
			serverName += flag;
		}else if(flag.equals("office365")){
			exServer = flag+"/";
			serverName += flag;
		}
//		String tmpFile 		= serverCalFilePath + "outlook/" + "." + creatorId + "tmp.txt";
//		String encrypFile 	= serverCalFilePath + "outlook/" + "." + creatorId + ".txt";
		String tmpFile 		= userFilepath + exServer + "." + creatorId + "tmp.txt";
		String encrypFile 	= userFilepath + exServer + "." + creatorId + ".txt";
		
		
//		infoSaveDir = new File(serverCalFilePath + "outlook/");
		infoSaveDir = new File(userFilepath + exServer);
		infoFile 	= new File(tmpFile);

		//???????????? ????????? ???????????? ????????? ??????
		if (!infoSaveDir.isDirectory()) {
			
			infoSaveDir.mkdirs();
		}

		// ?????? ????????? ???????????? ????????? ?????? ????????? ?????? ?????? ?????????, ?????? ????????? ??????
		if (infoFile.exists()) {
			infoFile.delete();
		}

		try {
			outlookId 	+= outlookID + "\r\n";
			outlookPw 	+= outlookPW + "\r\n";
			creatorNum 	+= creatorId + "\r\n";

			// true ????????? ????????? ?????? ????????? ????????? ??????
			BufferedWriter fw = new BufferedWriter(new FileWriter(infoFile, true));

			// ???????????? ????????? ??????
			fw.write(outlookId);
			fw.write(outlookPw);
			fw.write(creatorNum);
			fw.write(serverName);
			fw.flush();

			// ?????? ??????
			fw.close();
			// System.out.println("===========???????????? ?????? ??????(????????? ????????????)===========");

			//=================== ?????? ?????????===================================
			FileReader fr 			= null;
			FileWriter encryptionFw = null;
			int secret = 3; // ?????????&????????? ???????????? ???
			
			fr 				= new FileReader(tmpFile); // ????????? ????????? ?????? ????????? (????????? ?????? ?????? ??????)
			encryptionFw 	= new FileWriter(encrypFile); // ????????? ?????? ?????? ?????????

			int data = 0;

			log.debug("File Encryption Start");
			while ((data = fr.read()) != -1) {
				data += secret; 			// ?????????
				char ch = (char) data;
				//log.debug(ch); 		// ????????? ??????
				encryptionFw.write(data); 	// ???????????? ???????????? ????????????.
			} // while

			encryptionFw.flush();
			encryptionFw.close();
			fr.close();
			log.debug("File Encryption End");
			// ???????????? ??????
			if (infoFile.exists()) {
				// System.out.println("===========???????????? ?????? ??????(????????? ?????? ??????)===========");
				infoFile.delete();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}