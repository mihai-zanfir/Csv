package com.adverity.csv.service;

import static org.apache.commons.lang3.StringUtils.containsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.adverity.csv.mapper.StatisticMapper;
import com.adverity.csv.model.Column;
import com.adverity.csv.model.DisplayColumn;
import com.adverity.csv.model.OrderColumn;
import com.adverity.csv.model.SearchCriteria;
import com.adverity.csv.model.Statistic;
import com.adverity.csv.model.StatisticCsv;
import com.adverity.csv.repository.StatisticRepository;
import com.adverity.csv.util.SearchOperation;
import com.adverity.csv.util.SearchQueryCriteriaConsumer;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
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
	private final int RECORDS_DEFAULT_LIMIT = 1000;
	// An array of illegal words. If the SQL contains any of these than it will throw a 500 error
	private final String[] illegalWords = {"INSERT", "DELETE", "UPDATE", "DROP", "TABLE", "CREATE"};

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
		String msg = "";
		boolean status = false;
		if (!(file == null || file.isEmpty())) {
			// parse Csv file and converts the list of StatisticCsv to a list of Statistic entities
			List<Statistic> statistics = statisticMapper.mapListEntityCsvToListEntity(
					parseCsvFile(file));
			if (!statistics.isEmpty()) {
				// Save statistics in DB
				statisticRepository.saveAll(statistics);
				List<Statistic> statisticsSaved = statisticRepository.findAll();
				msg = "Successful saved in database: " + statisticsSaved.size() + " records";
				status = true;
				log.info(msg);
			} else {
				msg = "There were errors on parsing the Csv file!";
				log.error(msg);
			}
		} else {
			msg = "Please select a CSV file to upload. ";
			log.error(msg);
		}
		model.addAttribute("message", msg);
        model.addAttribute("status", status);
		return "file-upload-status";
	}

	/**
	 * Parse CSV file to create a list of Statistics entities
	 * 
	 * @param file A csv file to upload
	 * @return A list of Statistic entities
	 */
	public List<StatisticCsv> parseCsvFile(MultipartFile file) {
		try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			// create csv bean reader
			CsvToBean<StatisticCsv> csvToBean = new CsvToBeanBuilder(reader)
					.withType(StatisticCsv.class)
					.withIgnoreLeadingWhiteSpace(true).build();
			return csvToBean.parse();
		} catch (IOException ex) {
			log.error("Parsing error: " + ex.getMessage());
		}
		return Collections.emptyList();
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
	public String searchStatistics(String display, String condition, String groupBy, String orderBy,
			String offset, String limit, String showSQL, Model model) {
        
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = builder.createQuery(Object.class);
        Root stat = query.from(Statistic.class);
        
        handleDisplay(display, builder, query, stat);
        handleGroupBy(groupBy, builder, query, stat);
        handleCondition(condition, builder, query, stat);
	    handleOrderBy(orderBy, builder, query, stat);
	    List<Object> statistics = createQuery(query, offset, limit);
		
	    if (statistics.size() > 0) {
			log.info("Total records: " + String.valueOf(statistics.size()));
		} else {
			log.info("There are no records in the database!");
		}
		model.addAttribute("statistics", statistics);
		/*if ("true".equalsIgnoreCase(showSQL)) {
			model.addAttribute("sql", sql);
		}*/
		return "query-results";
	}
	
	/**
	 * Handle display parameter and prepare the SQL columns to be displayed
	 * 
	 * @param display display parameter that is received from request 
	 * @param builder	CriteriaBuilder object
	 * @param query	CriteriaQuery object
	 * @param stat Root object
	 */
	public void handleDisplay(String display, CriteriaBuilder builder, CriteriaQuery<Object> query, Root stat) {
		if (isNotBlank(display)) {
			List<DisplayColumn> columns = new ArrayList<DisplayColumn>();
		    Pattern pattern = Pattern.compile("([\\w:]+?),");
		    Matcher matcher = pattern.matcher(display + ",");
		    while (matcher.find()) {
		    	columns.add(new DisplayColumn(matcher.group(1)));
		    }
		    
		    Selection<?>[] selections = new Selection<?>[columns.size()];
		    for (int i=0; i < columns.size(); i++) {
		    	if ("sum".equalsIgnoreCase(columns.get(i).getFunction())) {
		    		selections[i] = builder.sum(stat.get(columns.get(i).getColumn()));
		    	} else {
		    		selections[i] = stat.get(columns.get(i).getColumn());
		    	}
		    }
		    query.multiselect(selections);
		} else {
			query.multiselect(stat.get("datasource"), stat.get("campaign"), stat.get("daily"),
					stat.get("clicks"), stat.get("impressions"));
		}
	}
	
	/**
	 * Handle groupBy parameter and prepare the SQL GROUP BY
	 * 
	 * @param groupBy groupBy parameter that is received from request 
	 * @param builder	CriteriaBuilder object
	 * @param query	CriteriaQuery object
	 * @param stat Root object
	 */
	public void handleGroupBy(String groupBy, CriteriaBuilder builder, CriteriaQuery<Object> query, Root stat) {
		if (isNotBlank(groupBy)) {
			List<Column> gbColumns = new ArrayList<Column>();
		    Pattern pattern = Pattern.compile("([\\w:]+?),");
		    Matcher matcher = pattern.matcher(groupBy + ",");
		    while (matcher.find()) {
		    	gbColumns.add(new Column(matcher.group(1)));
		    }
		    
		    Expression<?>[] gbSelections = new Expression<?>[gbColumns.size()];
		    for (int i=0; i < gbColumns.size(); i++) {
		    	gbSelections[i] = stat.get(gbColumns.get(i).getColumn());
		    }
		    query.groupBy(gbSelections);
		}
	}
	
	/**
	 * Handle condition parameter and prepare the SQL WHERE condition
	 * 
	 * @param condition condition parameter that is received from request 
	 * @param builder	CriteriaBuilder object
	 * @param query	CriteriaQuery object
	 * @param stat Root object
	 */
	public void handleCondition(String condition, CriteriaBuilder builder, CriteriaQuery<Object> query, Root stat) {
		if (isNotBlank(condition)) {
			List<SearchCriteria> params = new ArrayList<SearchCriteria>();
		    String operationSetExper = Joiner.on("|").join(SearchOperation.SIMPLE_OPERATION_SET);
		    Pattern pattern = Pattern.compile("(\\w+?)(" + operationSetExper + ")(\\p{Punct}?)([\\w\\s\\p{Punct}]+?)(\\p{Punct}?),");
		    Matcher matcher = pattern.matcher(condition + ",");
		    while (matcher.find()) {
		    	params.add(new SearchCriteria(
		          matcher.group(1),
		          matcher.group(2),
		          matcher.group(3),
		          matcher.group(4),
		          matcher.group(5)));
		    }
		    
	        Predicate predicate = builder.conjunction();
	        SearchQueryCriteriaConsumer searchConsumer = new SearchQueryCriteriaConsumer(predicate, builder, stat);
	        params.stream().forEach(searchConsumer);
	        predicate = searchConsumer.getPredicate();
	        query.where(predicate);
		}
	}
	
	/**
	 * Handle orderBy parameter and prepare the SQL ORDER BY condition
	 * 
	 * @param orderBy orderBy parameter that is received from request 
	 * @param builder	CriteriaBuilder object
	 * @param query	CriteriaQuery object
	 * @param stat Root object
	 */
	public void handleOrderBy(String orderBy, CriteriaBuilder builder, CriteriaQuery<Object> query, Root stat) {
		if (isNotBlank(orderBy)) {
			List<OrderColumn> oColumns = new ArrayList<OrderColumn>();
		    Pattern pattern = Pattern.compile("([\\w:]+?),");
		    Matcher matcher = pattern.matcher(orderBy + ",");
		    while (matcher.find()) {
		    	oColumns.add(new OrderColumn(matcher.group(1)));
		    }
		    
		    List<Order> orders = new ArrayList<Order>();
		    for (OrderColumn order : oColumns) {
		    	if ("sum".equalsIgnoreCase(order.getFunction())) {
		    		if ("desc".equalsIgnoreCase(order.getDirection())) {
		    			orders.add(builder.desc(builder.sum(stat.get(order.getColumn()))));
		    		} else {
		    			orders.add(builder.asc(builder.sum(stat.get(order.getColumn()))));
		    		}
		    	} else {
		    		if ("desc".equalsIgnoreCase(order.getDirection())) {
		    			orders.add(builder.desc(stat.get(order.getColumn())));
		    		} else {
		    			orders.add(builder.asc(stat.get(order.getColumn())));
		    		}
		    	}
		    }
		    query.orderBy(orders);
		}
	}
	
	/**
	 * Creates, execute SQL query and return the resulted records after quering the database. 
	 * 
	 * @param query	CriteriaQuery object
	 * @param offset offset parameter received from request
	 * @param limit limit parameter received from request
	 * @return a List of Objects (database records found in the database)
	 */
    public List<Object> createQuery(CriteriaQuery<Object> query, String offset, String limit) {
	    int offsetNr = 0;
		if (isNotBlank(offset)) {
			offsetNr = Optional.ofNullable(offset).map(Ints::tryParse).orElse(0);
		}
		int limitNr = RECORDS_DEFAULT_LIMIT;
		if (isNotBlank(limit)) {
			limitNr = Optional.ofNullable(limit).map(Ints::tryParse).orElse(RECORDS_DEFAULT_LIMIT);
			return entityManager.createQuery(query).setFirstResult(offsetNr).setMaxResults(limitNr).getResultList();
		}
		return entityManager.createQuery(query).setFirstResult(offsetNr).getResultList();
    }
	
	/**
	 * Checks if the SQL given as parameter contains any illegal words.
	 * The illegal words for the moment are: INSERT, DELETE, UPDATE
	 * This means it will only allow SELECT queries.
	 * If an illegal word is encountered then it will throw a 500 Error
	 * 
	 * This a simple SQL Injection prevention. Anyway, a most robust solution needs to be researched. 
	 * 
	 * @param sql input SQL query that will be checked
	 */
	public void checkIllegalWords(String sql) {
		if (containsAnyIgnoreCase(sql, illegalWords)) {
			throw new ResponseStatusException(
		           HttpStatus.INTERNAL_SERVER_ERROR, "Illegal query!");
		}
	}
}
