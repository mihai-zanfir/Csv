package com.adverity.csv.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.adverity.csv.service.CsvService;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Api(tags = "Csv Controller coded by Mihai Zanfir")
@Log4j2
@AllArgsConstructor
@RequestMapping("/api")
/**
 * Csv Controller. This is the main class for this code. It handles the requests to this API"
 * 
 * @author Mihai Zanfir - https://www.linkedin.com/in/mihai-zanfir-44671194/
 * 
 * Improvements that can be done:
 * a. Create a nice UI Frontend interface
 * b. Write some code to prevent SQL Injection
 * c. Save the data from the CSV in database in small groups using batches and transactions
 * d. Better Error Handling and response messaging
 * e. Using a relational database as: Oracle, Microsoft SQL Server, MySQL, etc.
 * f. Writing Unit Tests using JUnit
 * g. Writing functional tests
 */
public class CsvController {

	private final CsvService csvService;
	
	/**
	 * Basic test method
	 * @return A response string with the status of the operation
	 */
	@GetMapping("/test")
	public ResponseEntity<String> testGet() {
		log.info("Test()");
		return ResponseEntity.ok("Success");
	}

	/**
	 * Uploads a CSV file, parse it and save its data in the database
	 * 
	 * @param file A csv file to upload
	 * @return A response text with the status of the operation which could be Success or Error
	 */
	@PostMapping("/upload-csv-file")
        public ResponseEntity<String> uploadCSVFile(@RequestParam("file") MultipartFile file) {
		log.info("uploadCSVFile() -- " + file.getName());
                return csvService.uploadCSVFile(file);
        }
	
	/**
	 * Search the database and get a list of Statistic results based on the input query parameters
	 * This will only create and execute a SELECT query
	 * The COLUMN NAMES that could be used here are: DATASOURCE, CAMPAIGN, DAILY, CLICKS, IMPRESSIONS, ID
	 * We could use one column name or more column names separated by commas
	 * We could also use other SQL variables as: COUNT(*) or SUM(CLICKS), etc.
	 * No input parameter are required
	 * If you do not fill any parameter than it will return all the records but by default there is a 1000 record limits. 
	 * You could also display all the records if needed See @param limit
	 * 
	 * @param display The columns we want to be displayed on results. Can also be COUNT(*) or a comma separated list of columns
	 * 		If this is not filled than it will display all the columns
	 * 		Ex: COUNT(DATASOURCE), DATASOURCE
	 * 
	 * @param condition If this is used it will apply a condition used as WHERE or HAVING in the SQL query
	 * 		Ex: CAMPAIGN='Adventmarkt Touristik'
	 * 		Ex: CLICKS>10
	 * 		Ex: DAILY>'2020-02-14'
	 * 
	 * @param groupBy If this is used than will GROUP BY after columns specified in this parameter. Can be a comma separated list of columns.
	 *      Ex: DATASOURCE,CAMPAIGN
	 *      
	 * @param orderBy If this is used than it will order the results based on this parameter.
	 * 		This can be used for ascending or descending order using ASC and DESC words after the column names 
	 * 		Ex: CAMPAIGN ASC or CAMPAIGN DESC
	 * 
	 * @param offset If this is set than it will show the records starting from this offset. It needs to be a number.
	 * 		If is not set this will be 0. Using this parameter, Pagination could be easily created.
	 * 		Ex: 100 - it will display the records starting from this offset
	 * 
	 * @param limit If this is set than it will limit the records that is showing.
	 * 		If is not set than for security reason it will limit the records to a configured limit which is 1000.
	 *      Using this parameter, Pagination could be easily created.
	 * 		Ex: 10000 - it will limit the returned records to 10000
	 * 		Ex: no -it will return all the records (be careful with this)
	 * 
	 * @param showSQL If set to true, it will show the SQL generated on the top of records and the number of records found
	 * 
	 * @return a List of records or a text that will indicate the result of this operation (No results or ... Error)
	 */
	@GetMapping("/search")
	public ResponseEntity<?> searchStatistics(
			@RequestParam(name = "display", required=false) String display,
			@RequestParam(name = "condition", required=false) String condition, 
			@RequestParam(name = "groupBy", required=false) String groupBy,
			@RequestParam(name = "orderBy", required=false) String orderBy,
			@RequestParam(name = "offset", required=false) String offset,
			@RequestParam(name = "limit", required=false) String limit,
			@RequestParam(name = "showSQL", required=false) String showSQL) {
		log.info("searchStatistics() -- display:" + display + " condition:" + condition + " groupBy:" + groupBy +
				" orderBy:" + orderBy + " offset:" + offset + " limit:" + limit + " showSQL:" + showSQL);
		return csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL);
	}
}
