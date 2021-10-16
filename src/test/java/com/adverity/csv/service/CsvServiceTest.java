package com.adverity.csv.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.adverity.csv.mapper.StatisticMapper;
import com.adverity.csv.model.Statistic;
import com.adverity.csv.model.StatisticCsv;
import com.adverity.csv.repository.StatisticRepository;

class CsvServiceTest {
	
	@Spy
	private StatisticRepository statisticRepository;
	@Mock
	private StatisticMapper statisticMapper;
	@Mock
	private EntityManager entityManager;
	@Mock
	Query query;
	@Spy
	Model model;
	
	private CsvService csvService;
	
	@BeforeEach
	public void init() {
		MockitoAnnotations.openMocks(this);
		csvService = new CsvService(statisticRepository, statisticMapper, entityManager);
	}
	
	/**
	 * Test uploading a null CSV file
	 */
	@Test
	void testuploadCSVFileNull() {
		String template = csvService.uploadCSVFile(null, model);
		assertEquals(template, "file-upload-status");
		Mockito.verify(model).addAttribute("message", "Please select a CSV file to upload. ");
		Mockito.verify(model).addAttribute("status", false);
	}
	
	/**
	 * Test uploading a CSV file
	 */
	@Test
	void testuploadCSVFile() {
		try {
			MultipartFile file = new MockMultipartFile ("CsvTest.csv", new FileInputStream(new File("src/test/data/CsvTest.csv")));
			List<Statistic> statistics = new ArrayList<Statistic>();
			statistics.add(new Statistic(1, "Google Ads", "Adventmarkt Touristik", LocalDate.now(), 5 , 7705));
			statistics.add(new Statistic(1, "Google Ads", "GDN_Retargeting", LocalDate.now(), 33 , 29954));
			Mockito.doReturn(statistics).when(statisticMapper).mapListEntityCsvToListEntity(any());
			Mockito.doReturn(statistics).when(statisticRepository).findAll();
			String template = csvService.uploadCSVFile(file, model);
			Mockito.verify(statisticRepository).saveAll(statistics);
			Mockito.verify(statisticRepository).findAll();
			assertEquals(template, "file-upload-status");
			Mockito.verify(model).addAttribute("message", "Successful saved in database: 2 records");
			Mockito.verify(model).addAttribute("status", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test parsing a CSV file
	 */
	@Test
	void testParseCsvFile() {
		try {
			MultipartFile file = new MockMultipartFile ("CsvTest.csv", new FileInputStream(new File("src/test/data/CsvTest.csv")));
			List<StatisticCsv> statistics = csvService.parseCsvFile(file);
			StatisticCsv statistic = (StatisticCsv)statistics.get(0);
			assertEquals(statistics.size(), 2);
			assertEquals(statistic.getDatasource(), "Google Ads");
			assertEquals(statistic.getCampaign(), "Adventmarkt Touristik");
			assertEquals(statistic.getDaily(), "12/24/19");
			assertEquals(statistic.getClicks(), 5);
			assertEquals(statistic.getImpressions(), 7705);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test searching the statistics
	 */
	@Test
	void testSearchStatistics() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String showSQL = null;
		//ModelMock model = new ModelMock();
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		Mockito.doReturn(query).when(entityManager).createNativeQuery(sql);
		ArrayList<String> statistics = new ArrayList<String>();
		statistics.add("Result");
		Mockito.doReturn(statistics).when(query).getResultList();
		String template = csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL, model);
		assertEquals(template, "query-results");
		Mockito.verify(model).addAttribute("statistics", statistics);
	}
	
	
	/**
	 * Test searching the statistics when showSQL parameter is true
	 */
	@Test
	void testSearchStatisticsShowSQL() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String showSQL = "true";
		//ModelMock model = new ModelMock();
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		Mockito.doReturn(query).when(entityManager).createNativeQuery(sql);
		ArrayList<String> statistics = new ArrayList<String>();
		statistics.add("Result");
		Mockito.doReturn(statistics).when(query).getResultList();
		String template = csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL, model);
		assertEquals(template, "query-results");
		Mockito.verify(model).addAttribute("statistics", statistics);
		Mockito.verify(model).addAttribute("sql", sql);
	}
	
	/**
	 * Test the creation of SQL when no parameter is set
	 */
	@Test
	void testCreateSQLOnlyNullParameters() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when display parameter is set
	 */
	@Test
	void testCreateSQLDisplaySet() {
		String display = "CAMPAIGN"; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT CAMPAIGN FROM STATISTIC LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when condition parameter is set
	 */
	@Test
	void testCreateSQLConditionSet() {
		String display = null; 
		String condition = "CAMPAIGN == 'Adventmarkt Touristik'";
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC WHERE CAMPAIGN == 'Adventmarkt Touristik' LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when groupBy parameter is set
	 */
	@Test
	void testCreateSQLGroupBySet() {
		String display = null; 
		String condition = null;
		String groupBy = "CAMPAIGN";
		String orderBy = null;
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC GROUP BY CAMPAIGN LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when groupBy and condition parameters are set
	 */
	@Test
	void testCreateSQLGroupByConditionSet() {
		String display = null; 
		String condition = "CAMPAIGN == 'Adventmarkt Touristik'";
		String groupBy = "CAMPAIGN";
		String orderBy = null;
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC GROUP BY CAMPAIGN HAVING CAMPAIGN == 'Adventmarkt Touristik' LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when orderBy parameter is set
	 */
	@Test
	void testCreateSQLOrderBySet() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = "CAMPAIGN ASC";
		String offset = null;
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC ORDER BY CAMPAIGN ASC LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when offset parameter is set
	 */
	@Test
	void testCreateSQLOffsetSet() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = "10";
		String limit = null;
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC OFFSET 10 LIMIT 1000");
	}
	
	/**
	 * Test the creation of SQL when limit parameter is set
	 */
	@Test
	void testCreateSQLLimitSet() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = "200";
		String sql = csvService.createSQL(display, condition, groupBy, orderBy, offset, limit);
		assertEquals(sql, "SELECT * FROM STATISTIC LIMIT 200");
	}
	
	/**
	 * This checks that if the SQL does not contain any illegal words, it won't throw any Errors
	 */
	@Test
	void testCheckIllegalWords() {
		String sql = "SELECT INTO STATISTIC";
		assertDoesNotThrow(() -> csvService.checkIllegalWords(sql));
	}

	/**
	 * This checks that if the SQL contains an illegal word, it will throw 500 Error
	 */
	@Test
	void testCheckIllegalWordsThrows500Error() {
		String sql = "SELECT INTO STATISTIC WHERE insert test";
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> csvService.checkIllegalWords(sql));
		assertEquals("500 INTERNAL_SERVER_ERROR \"Illegal query!\"", thrown.getMessage());
	}	
}
