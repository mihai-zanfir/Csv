package com.adverity.csv.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.adverity.csv.service.CsvService;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@Api(tags = "Csv Controller coded by Mihai Zanfir")
@Log4j2
@AllArgsConstructor
@RequestMapping("/api")
/**
 * Csv Controller. This is the main class for this code. It handles the requests
 * to this API"
 * 
 * @author Mihai Zanfir - https://www.linkedin.com/in/mihai-zanfir-44671194/
 * 
 *         Improvements that can be done to this code:
 *         1. Create a nice UI
 *         Frontend interface: For Upload I would create a component having a
 *         progress bar so that to see the time when the CSV is uploaded (or
 *         processed). 
 *         For Queries I would create a UI with some WYSWG
 *         components (ex: column names, *, COUNT, SUM, etc.) so that to create
 *         these queries easier. 
 *         2. Write some code to prevent SQL Injection 
 *         3. Save the data from the CSV in database in bulks (small groups) using
 *         batches and transaction
 *         4. Better Error Handling and response
 *         messaging 
 *         5. Using a relational database as: Oracle, Microsoft SQL
 *         Server, MySQL, etc so that the processed inserted data is not lost.
 *         6. Writing Unit Tests using JUnit 
 *         7. Writing functional tests using Groovy
 *         8. I would create some indexes on the database table so that
 *         the queries will run faster. The inserts will take longer but the
 *         read and searches in the database will be faster than before. 
 *         9. Since DATASOURCE CAMPAIGN contain a limited number of values, I would
 *         normalize the table and put these on their own tables so in the end
 *         it will be 3 database tables At processing CSV time, we will check
 *         these values with the existing values and if it does not found the
 *         value in these table, it will insert them This will be replaced in
 *         the main table with 2 ID columns that will be linked with the 2
 *         tables via some foreign keys. 
 *         10. Cache the database entitities and
 *         requests to the backend so that similar future requets will not be
 *         executed in the database but brought from the cache 11. etc.
 */
public class CsvController {

	private final CsvService csvService;

	/**
	 * Homepage
	 */
	@Operation(summary = "Get the initial page")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Get the initial page", content = @Content)})
	@GetMapping("/")
	public String index() {
		return "index";
	}

	/**
	 * Uploads a CSV file, parse it and save its data in the database
	 * 
	 * @param file A csv file to upload
	 * @return A response text with the status of the operation which could be
	 *         Success or Error
	 */
	@Operation(summary = "Uploads a CSV file, parse it and save its data in the database")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "return A response text with the status of the operation which could be Success or Error",
			content = @Content)})
	@PostMapping("/upload-csv-file")
	public String uploadCSVFile(@RequestParam("file") MultipartFile file, Model model) {
		log.info("uploadCSVFile() -- " + file.getName());
		return csvService.uploadCSVFile(file, model);
	}

	/**
	 * Search the database and get a list of Statistic results based on the input query parameters.
	 * 
	 * This will create and execute a SELECT query.
	 * The COLUMN NAMES that could be used here are: datasource, campaign, daily, clicks, impressions, id.
	 * We could use one column definitions or more column names separated by commas.
	 * We could also use other SQL variables as: clicks:sum. 
	 * No input parameter are required. 
	 * If you do not fill any parameter than it will return all the records 
	 * 
	 * What operators could you use?
	 * : - equality (Ex: datasource:Google Ads) - this is for records that are equal with Google Ads
	 * ! - negation (Ex: datasource!Google Ads) - this is for records that are not equal with Google Ads
	 * > - greater than (works fine for numbers or dates) (Ex: daily>01-01-2020)
	 * < - lower than  (works fine for numbers or dates) (Ex: daily<01-31-2020)
	 * ~ - like (Ex: datasource~Google Ads) - this is for records that are like Google Ads (see SQL LIKE)
	 * * - if used at the begining of the word it will search for words starting with (Ex: datasource:*Ads)
	 * * - if used at the ending of the word it will search for words ending with (Ex: datasource:Google*)
	 * * - if used at the begining of the word and at the ending of the word it will search for words containing this word (Ex: datasource:*oogle*)
	 * 
	 * These are concatenating these conditions using AND but we can also use OR by putting ' in front of the individual condition:
	 * Ex: datasource:Google Ads,daily>01-01-2020,daily<01-31-2020 - This is for getting the records that have datasource equal with "Google Ads" AND the daily date is in the range we need.
	 * Ex: datasource:Google Ads,'daily>01-01-2020 - This is for getting the records that have datasource equal with "Google Ads" OR the daily date is greater than 01-01-2020.
	 * 
	 * @param display The columns we want to be displayed on results (comma separated). 
	 * 			Can also be the name of one columnn followed by :sum (For a SUM on that column values) or a comma separated list of columns
	 *  		If this is not filled than it will display all the columns.
	 *  		Ex: datasource
	 *  		Ex: impressions:sum
	 * 			Ex: daily,impressions:sum
	 *  
	 * @param condition If this is used it will apply a condition used as WHERE (or HAVING) in the SQL query
	 *  		Ex: campaign:'Adventmarkt Touristik'
	 *  		Ex: clicks>10
	 *  		Ex: daily>'2020-02-14'
	 *  
	 * @param groupBy If this is used than will GROUP BY after columns specified in this parameter. Can be a comma separated list of columns
	 *       	Ex: datasource,campaign
	 *       
	 * @param orderBy If this is used than it will order the results based on this parameter.
	 *  		This can be used for ascending or descending order using asc and desc words after the column names 
	 *  		Ex: campaign:asc or campaign:desc
	 * 			If you displayed a SUM on a column then this columns should also contain :sum in it
	 * 			Ex: clicks:sum:desc
	 *  
	 * @param offset If this is set than it will show the records starting from this offset. It needs to be a number.
	 *  		If is not set this will be 0. Using this parameter, Pagination could be easily created.
	 *  		Ex: 100 - it will display the records starting from this offset
	 *  
	 * @param limit If this is set than it will limit the records that is showing.
	 *  		TO DO Maybe! If is not set than for security reason it should limit the records to a configured limit which is 1000. This value can be changed in the application.
	 *  		Using this parameter, Pagination could be easily created.
	 *  		Ex: 10000 - it will limit the returned records to 10000
	 *  		Ex: If is not set then it will return all the records (be careful with this as if there are many records it will slow your browser)
	 *  
	 * @param showSQL This is not working anymore for now. If set to true, it would show the SQL generated on the top of records and the number of records found
	 * 
	 * @return a List of records or a text that will indicate the result of this operation (No results or ... Error)
	 */
	@Operation(summary = "Search the database and get a list of Statistic results based on the input query parameters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Return a List of records or a text that will indicate the result of this operation", 
						 content = @Content),
			@ApiResponse(responseCode = "500", description = "Illegal query words were used", 
			    		 content = @Content)})
	@GetMapping("/search")
	public String searchStatistics(
			@Parameter(description = "The columns we want to be displayed on results. "
					+ "The COLUMN NAMES that could be used here are: datasource, campaign, daily, clicks, impressions, id.") 
				@RequestParam(name = "display", required = false) String display,
			@Parameter(description = "If this is used it will apply a condition used as WHERE or HAVING in the SQL query")
				@RequestParam(name = "condition", required = false) String condition,
			@Parameter(description = "If this is used than will GROUP BY after columns specified in this parameter")
				@RequestParam(name = "groupBy", required = false) String groupBy,
			@Parameter(description = "If this is used than it will order the results based on this parameter")
				@RequestParam(name = "orderBy", required = false) String orderBy,
			@Parameter(description = "If this is set than it will show the records starting from this ofset")
				@RequestParam(name = "offset", required = false) String offset,
			@Parameter(description = "If this is set than it will limit the records that is showing")
				@RequestParam(name = "limit", required = false) String limit,
			@Parameter(description = "If set to true, it will show the SQL generated on the top of records and the number of records found")
				@RequestParam(name = "showSQL", required = false) String showSQL,
			Model model, HttpServletResponse response) {
		log.info("searchStatistics() -- display:" + display + " condition:" + condition + " groupBy:" + groupBy
				+ " orderBy:" + orderBy + " offset:" + offset + " limit:" + limit + " showSQL:" + showSQL);
		return csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL, model);
	}
	
	/**
	 * Basic test method
	 * 
	 * @return A response string with the status of the operation
	 */
	@Operation(summary = "Get a test page")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Get a test page", content = @Content)})
	@GetMapping("/test")
	public ResponseEntity<String> testGet() {
		log.info("Test()");
		return ResponseEntity.ok("Success");
	}
}
