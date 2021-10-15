package com.adverity.csv.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.containsAnyIgnoreCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.adverity.csv.mapper.StatisticMapper;
import com.adverity.csv.model.Statistic;
import com.adverity.csv.model.StatisticCsv;
import com.adverity.csv.repository.StatisticRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@AllArgsConstructor
@Log4j2
/**
 * Csv Service - This service methods are called by the Csv Controller"
 * 
 * @author Mihai Zanfir - https://www.linkedin.com/in/mihai-zanfir-44671194/
 */
public class CsvService {
	// Limits the displayed records to 1000
	private final String RECORDS_DEFAULT_LIMIT = "1000";
	// An array of illegal words. If the SQL contains any of these than it will throw a 500 error
	private final String[] illegalWords = {"INSERT", "DELETE", "UPDATE"};

	private final StatisticRepository statisticRepository;
	private final StatisticMapper statisticMapper;
	private final EntityManager entityManager;

	/**
	 * Uploads a CSV file, parse it and save its data in the database
	 * 
	 * @param file A csv file to upload
	 * @return A response text with the status of the operation which could be
	 *         Success or Error
	 */
	public String uploadCSVFile(MultipartFile file, Model model) {
		// validate file
		String msg = "";
		boolean status = false;
		if (file.isEmpty()) {
			msg = "Please select a CSV file to upload. ";
			log.error(msg + file.getName());
		} else {
			// parse CSV file to create a list of `StatisticsCsv` objects
			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
				// create csv bean reader
				CsvToBean<StatisticCsv> csvToBean = new CsvToBeanBuilder(reader)
						.withType(StatisticCsv.class)
						.withIgnoreLeadingWhiteSpace(true).build();
				// convert `CsvToBean` object to list of StatisticCsv and map it to list of
				// Statistic entities
				List<Statistic> statistics = statisticMapper.mapListEntityCsvToListEntity(csvToBean.parse());
				// Save statistics in DB
				statisticRepository.saveAll(statistics);
				List<Statistic> statisticsSaved = statisticRepository.findAll();
				msg = "Successful saved in database: " + statisticsSaved.size() + " records";
				log.info(msg);
				status = true;
			} catch (IOException ex) {
				msg = "Errors: " + ex.getMessage();
				log.error(msg);
			}
		}
		model.addAttribute("message", msg);
        model.addAttribute("status", status);
		return "file-upload-status";
	}

	/**
	 * Search the database and get a list of Statistic results based on the input
	 * query parameters This will only create and execute a SELECT query The COLUMN
	 * NAMES that could be used here are: DATASOURCE, CAMPAIGN, DAILY, CLICKS,
	 * IMPRESSIONS, ID We could use one column name or more column names separated
	 * by commas We could also use other SQL variables as: COUNT(*) or SUM(CLICKS),
	 * etc. No input parameter are required If you do not fill any parameter than it
	 * will return all the records but by default there is a 1000 record limits. You
	 * could also display all the records if needed See @param limit
	 * 
	 * @param display   The columns we want to be displayed on results. Can also be
	 *                  COUNT(*) or a comma separated list of columns If this is not
	 *                  filled than it will display all the columns 
	 *                  Ex: COUNT(DATASOURCE), DATASOURCE
	 * 
	 * @param condition If this is used it will apply a condition used as WHERE or
	 *                  HAVING in the SQL query 
	 *                  Ex: CAMPAIGN='Adventmarkt Touristik'
	 *                  Ex: CLICKS>10 
	 *                  Ex: DAILY>'2020-02-14'
	 * 
	 * @param groupBy   If this is used than will GROUP BY after columns specified
	 *                  in this parameter. Can be a comma separated list of columns.
	 *                  Ex: DATASOURCE,CAMPAIGN
	 * 
	 * @param orderBy   If this is used than it will order the results based on this
	 *                  parameter. This can be used for ascending or descending
	 *                  order using ASC and DESC words after the column names 
	 *                  Ex: CAMPAIGN ASC or CAMPAIGN DESC
	 * 
	 * @param offset    If this is set than it will show the records starting from
	 *                  this offset. It needs to be a number. If is not set this
	 *                  will be 0. Using this parameter, Pagination could be easily
	 *                  created. 
	 *                  Ex: 100 - it will display the records starting from this offset
	 * 
	 * @param limit     If this is set than it will limit the records that is
	 *                  showing. If is not set than for security reason it will
	 *                  limit the records to a configured limit which is 1000. Using
	 *                  this parameter, Pagination could be easily created. 
	 *                  Ex: 10000 - it will limit the returned records to 10000 
	 *                  Ex: no -it will return all the records (be careful with this)
	 * 
	 * @param showSQL   If set to true, it will show the SQL generated on the top of
	 *                  records and the number of records found
	 * 
	 * @return a List of records or a text that will indicate the result of this
	 *         operation (No results or ... Error)
	 */
	public String searchStatistics(String display, String condition, String groupBy, String orderBy,
			String offset, String limit, String showSQL, Model model) {
		String sql = createSQL(display, condition, groupBy, orderBy, offset, limit);
		checkIllegalWords(sql);
		Query query = entityManager.createNativeQuery(sql);
		List<Object> statistics = query.getResultList();
		if (statistics.size() > 0) {
			log.info("Total records: " + String.valueOf(statistics.size()));
		} else {
			log.info("There are no records in the database!");
		}
		model.addAttribute("statistics", statistics);
		if (isNotBlank(showSQL)) {
			model.addAttribute("sql", sql);
		}
		return "query-results";
	}

	/**
	 * Creates a SQL based on the input parameters 
	 * Ex: SELECT COUNT(*) FROM STATISTIC where CAMPAIGN LIKE ('SN_OnlineSchn%') 
	 * Ex: SELECT COUNT(DATASOURCE), DATASOURCE FROM STATISTIC GROUP BY DATASOURCE HAVING CLICKS>10
	 * 
	 * @param display   The columns we want to be displayed on results. Can also be
	 *                  COUNT(*) or a comma separated list of columns If this is not
	 *                  filled than it will display all the columns 
	 *                  Ex: COUNT(DATASOURCE), DATASOURCE
	 * 
	 * @param condition If this is used it will apply a condition used as WHERE or
	 *                  HAVING in the SQL query 
	 *                  Ex: CAMPAIGN='Adventmarkt Touristik'
	 *                  Ex: CLICKS>10 Ex: DAILY>'2020-02-14'
	 * 
	 * @param groupBy   If this is used than will GROUP BY after columns specified
	 *                  in this parameter. Can be a comma separated list of columns.
	 *                  Ex: DATASOURCE,CAMPAIGN
	 * 
	 * @param orderBy   If this is used than it will order the results based on this
	 *                  parameter. This can be used for ascending or descending
	 *                  order using ASC and DESC words after the column names 
	 *                  Ex: CAMPAIGN ASC or CAMPAIGN DESC
	 * 
	 * @param offset    If this is set than it will show the records starting from
	 *                  this offset. It needs to be a number. If is not set this
	 *                  will be 0. Using this parameter, Pagination could be easily
	 *                  created. 
	 *                  Ex: 100 - it will display the records starting from
	 *                  this offset
	 * 
	 * @param limit     If this is set than it will limit the records that is
	 *                  showing. If is not set than for security reason it will
	 *                  limit the records to a configured limit which is 1000. Using
	 *                  this parameter, Pagination could be easily created. 
	 *                  Ex: 10000 - it will limit the returned records to 10000 
	 *                  Ex: no -it will return all the records (be careful with this)
	 * 
	 * @param showSQL   If set to true, it will show the SQL generated on the top of
	 *                  records and the number of records found
	 * 
	 * @return A SELECT SQL query created using the input parameters
	 */
	private String createSQL(String display, String condition, String groupBy, String orderBy, String offset, String limit) {
		if (isBlank(display)) {
			display = "*";
		}
		String sql = "SELECT " + display + " FROM STATISTIC";
		if (isBlank(groupBy)) {
			if (isNotBlank(condition)) {
				sql += " WHERE " + condition;
			}
		} else {
			sql += " GROUP BY " + groupBy;
			if (isNotBlank(condition)) {
				sql += " HAVING " + condition;
			}
		}
		if (isNotBlank(orderBy)) {
			sql += " ORDER BY " + orderBy;
		}
		if (isNotBlank(offset)) {
			sql += " OFFSET " + offset;
		}
		if (isBlank(limit)) {
			sql += " LIMIT " + RECORDS_DEFAULT_LIMIT;
		} else if (!"no".equalsIgnoreCase(limit)) {
			sql += " LIMIT " + limit;
		}
		log.info("SQL: " + sql);
		return sql;
	}
	
	/**
	 * Checks if the SQL given as parameter contains any illegal words.
	 * The illegal words for the moment are: INSERT, DELETE, UPDATE
	 * If an illegal word is encountered then it will throw a 500 Error
	 * 
	 * @param sql input SQLquery string
	 */
	private void checkIllegalWords(String sql) {
		if (containsAnyIgnoreCase(sql, illegalWords)) {
			throw new ResponseStatusException(
		           HttpStatus.INTERNAL_SERVER_ERROR, "Illegal query!");
		}
	}
}
