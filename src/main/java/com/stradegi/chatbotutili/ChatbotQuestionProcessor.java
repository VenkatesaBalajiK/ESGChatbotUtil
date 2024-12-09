package com.stradegi.chatbotutili;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class ChatbotQuestionProcessor {

	private static final String EXCEL_FILE_PATH = "D:\\3.Automation\\3. Web Automation\\Eclipse Local Workspace\\ESGChatbotUtili\\src\\test\\resources\\Excel Files\\Question Prepared For Testing.xlsx";
	private static final String API_REQUEST_URL = "http://3.227.191.227:8080/api/chatbot/sql/request";
	private static final String API_PROCESS_STATUS_URL = "http://3.227.191.227:8080/api/chatbot/sql/process-status";
	private static final String API_RESPONSE_URL = "http://3.227.191.227:8080/api/chatbot/sql/response";

	public static void main(String[] args) {
		try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH); Workbook workbook = new XSSFWorkbook(file)) {

			// Reading Excel file and sending API requests
			processExcelAndSendRequests(workbook);

		} catch (IOException e) {
			System.err.println("Error reading the Excel file: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Method to process Excel file and send API requests
	private static void processExcelAndSendRequests(Workbook workbook) {
		Sheet sheet = workbook.getSheetAt(0); // Assume data is in the first sheet
		Iterator<Row> rowIterator = sheet.iterator();

		// Get index of the "Question" column
		int questionColumnIndex = getQuestionColumnIndex(rowIterator);
		if (questionColumnIndex == -1) {
			throw new RuntimeException("Column 'Question' not found in the Excel sheet.");
		}

		// Iterate through rows and send API requests
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			String question = getCellValue(row, questionColumnIndex);

			if (question != null && !question.isEmpty()) {
				sendApiRequests(question);
			}
		}
	}

	// Method to find the "Question" column index
	private static int getQuestionColumnIndex(Iterator<Row> rowIterator) {
		Row headerRow = rowIterator.next(); // Assuming the first row contains headers
		for (Cell cell : headerRow) {
			if (cell.getStringCellValue().equalsIgnoreCase("Question")) {
				return cell.getColumnIndex();
			}
		}
		return -1; // Column not found
	}

	// Method to get cell value from a specific column
	private static String getCellValue(Row row, int columnIndex) {
		Cell cell = row.getCell(columnIndex);
		return cell != null ? cell.getStringCellValue() : null;
	}

	// Method to send API requests based on the question
	private static void sendApiRequests(String question) {
		String requestBody1 = buildRequestBody(question);
		System.out.println("Question: " + question);

		// Send first API request
		Response request = sendPostRequest(API_REQUEST_URL, requestBody1);
//		System.out.println("Response from API 1: " + request.prettyPrint());

		// Send first API request
		String requestStatus = "";
		Response processStatus;
		do {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			processStatus = sendPostRequest(API_PROCESS_STATUS_URL, request.asPrettyString());
			// Parse the JSON string
			JSONObject jsonObject = new JSONObject(processStatus.asPrettyString());

			// Fetch the request_status
			requestStatus = jsonObject.getString("request_status");

			System.out.println("Process Status: " + requestStatus);
		} while (!requestStatus.equalsIgnoreCase("success"));

		// Send second API request using the response of the first one
		Response apiResponse2 = sendPostRequest(API_RESPONSE_URL, processStatus.asPrettyString());

		// Parse the JSON string
		JSONObject jsonObject = new JSONObject(apiResponse2.prettyPrint());

		// Extract the chatbot_response array
		JSONArray chatbotResponseArray = jsonObject.getJSONArray("chatbot_response");

		// Print the result
		System.out.println("Chatbot Response :" + chatbotResponseArray);
	}

	// Method to build request body for the first API request
	private static String buildRequestBody(String question) {
		return "{\n" + "    \"previous_request_ids\": [],\n" + "    \"question\": \"" + question + "\"\n" + "}";
	}

	// Method to send a POST request to a given URL with a JSON body
	private static Response sendPostRequest(String url, String requestBody) {
		return RestAssured.given().baseUri(url).contentType(ContentType.JSON).body(requestBody).post();
	}
}
