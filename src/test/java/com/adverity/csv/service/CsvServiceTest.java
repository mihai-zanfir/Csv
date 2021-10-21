package com.adverity.csv.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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
	@Spy
	private EntityManager entityManager;
	@Spy
	Model model;
	@Mock
	CriteriaBuilder builder;
	@Spy
	CriteriaQuery<Object> query;
	@Spy
	Root stat;
	@Mock
	TypedQuery<Object> typedQuery;
	
	private CsvService csvService;
	
	@BeforeEach
	public void init() {
		MockitoAnnotations.openMocks(this);
		//builder = entityManager.getCriteriaBuilder();
		//query = builder.createQuery(Object.class);
		//stat = query.from(Statistic.class);
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
	 * Test handle Display when display parameter is set
	 */
	@Test
	void testHandleDisplay() {
		String display = "campaign,daily";
		csvService.handleDisplay(display, builder, query, stat);
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(query, times(1)).multiselect(any(), any());
	}
	
	/**
	 * Test handle Display when display parameter is set and contain a sum function
	 */
	@Test
	void testHandleDisplaySum() {
		String display = "campaign:sum,daily";
		csvService.handleDisplay(display, builder, query, stat);
		verify(builder, times(1)).sum(any());
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(query, times(1)).multiselect(any(), any());
	}
	
	/**
	 * Test handle Display when display parameter is null or empty
	 */
	@Test
	void testHandleDisplayNull() {
		String display = null;
		csvService.handleDisplay(display, builder, query, stat);
		verify(query, times(1)).multiselect(stat.get("datasource"), stat.get("campaign"), stat.get("daily"),
				stat.get("clicks"), stat.get("impressions"));
	}
	
	/**
	 * Test handle GroupBy when groupBy parameter is set
	 */
	@Test
	void testHandleGroupBy() {
		String groupBy = "campaign,daily";
		csvService.handleGroupBy(groupBy, builder, query, stat);
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(query, times(1)).groupBy(any(), any());
	}
	
	/**
	 * Test handle GroupBy when groupBy parameter is null or empty
	 */
	@Test
	void testHandleGroupByNull() {
		String groupBy = null;
		csvService.handleGroupBy(groupBy, builder, query, stat);
		verifyNoInteractions(stat);
		verifyNoInteractions(query);
	}
	
	/**
	 * Test handle OrderBy when orderBy parameter is set with 2 column names and without direction
	 */
	@Test
	void testHandleOrderBy() {
		String orderBy = "campaign,daily";
		csvService.handleOrderBy(orderBy, builder, query, stat);
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(builder, times(2)).asc(any());
		List<Order> orders = new ArrayList<Order>();
		orders.add(null);
		orders.add(null);
		verify(query, times(1)).orderBy(orders);
	}
	
	/**
	 * Test handle OrderBy when orderBy parameter is set with 2 column names and direction
	 */
	@Test
	void testHandleOrderByDesc() {
		String orderBy = "campaign:desc,daily";
		csvService.handleOrderBy(orderBy, builder, query, stat);
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(builder, times(1)).desc(any());
		verify(builder, times(1)).asc(any());
		List<Order> orders = new ArrayList<Order>();
		orders.add(null);
		orders.add(null);
		verify(query, times(1)).orderBy(orders);
	}
	
	/**
	 * Test handle OrderBy when orderBy parameter is set with 2 column names and without direction. One column is a SUM.
	 */
	@Test
	void testHandleOrderBySum() {
		String orderBy = "campaign:sum:asc,daily";
		csvService.handleOrderBy(orderBy, builder, query, stat);
		verify(stat, times(1)).get("campaign");
		verify(stat, times(1)).get("daily");
		verify(builder, times(2)).asc(any());
		verify(builder, times(1)).sum(any());
		List<Order> orders = new ArrayList<Order>();
		orders.add(null);
		orders.add(null);
		verify(query, times(1)).orderBy(orders);
	}
	
	/**
	 * Test handle OrderBy when orderBy parameter is null or empty
	 */
	@Test
	void testHandleOrderByNull() {
		String orderBy = null;
		csvService.handleOrderBy(orderBy, builder, query, stat);
		verifyNoInteractions(builder);
		verifyNoInteractions(stat);
		verifyNoInteractions(query);
	}
	
	/**
	 * Test handle Condition when condition parameter is set
	 */
	@Test
	void testHandleCondition() {
		String condition = "datasource:Google Ads,daily>01-01-2020";
		csvService.handleCondition(condition, builder, query, stat);
		verify(stat, times(1)).get("datasource");
		verify(stat, times(1)).get("daily");
		verify(builder, times(1)).equal(null, "Google Ads");
		verify(builder, times(2)).and(any(), any());
		LocalDate daily = LocalDate.parse("01-01-2020", DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		verify(builder, times(1)).greaterThan(null, daily);
		Predicate predicate = null;
		verify(query, times(1)).where(predicate);
	}
	
	/**
	 * Test handle Condition when condition parameter is set and first parameter is combined with the second parameter using OR instead of AND
	 */
	@Test
	void testHandleConditionOr() {
		String condition = "datasource:Google Ads,'daily>01-01-2020";
		csvService.handleCondition(condition, builder, query, stat);
		verify(stat, times(1)).get("datasource");
		verify(stat, times(1)).get("daily");
		verify(builder, times(1)).equal(null, "Google Ads");
		verify(builder, times(1)).and(any(), any());
		verify(builder, times(1)).or(any(), any());
		LocalDate daily = LocalDate.parse("01-01-2020", DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		verify(builder, times(1)).greaterThan(null, daily);
		Predicate predicate = null;
		verify(query, times(1)).where(predicate);
	}
	
	/**
	 * Test handle Condition when condition parameter is null or empty
	 */
	@Test
	void testHandleConditionNull() {
		String condition = null;
		csvService.handleCondition(condition, builder, query, stat);
		verifyNoInteractions(builder);
		verifyNoInteractions(stat);
		verifyNoInteractions(query);
	}
	
	/**
	 * Test Create Query when offset and limit parameters are null or empty
	 */
	@Test
	void testCreateQuery() {
		String offset = null;
		String limit = null;
		Mockito.doReturn(typedQuery).when(entityManager).createQuery(query);
		Mockito.doReturn(typedQuery).when(typedQuery).setFirstResult(0);
		csvService.createQuery(query, offset, limit);
		verify(entityManager, times(1)).createQuery(query);
		verify(typedQuery, times(1)).setFirstResult(0);
		verify(typedQuery, times(1)).getResultList();
	}
	
	/**
	 * Test Create Query when offset parameter is set and limit parameters is null or empty
	 */
	@Test
	void testCreateQueryOffset() {
		String offset = "100";
		String limit = null;
		Mockito.doReturn(typedQuery).when(entityManager).createQuery(query);
		Mockito.doReturn(typedQuery).when(typedQuery).setFirstResult(100);
		csvService.createQuery(query, offset, limit);
		verify(entityManager, times(1)).createQuery(query);
		verify(typedQuery, times(1)).setFirstResult(100);
		verify(typedQuery, times(1)).getResultList();
	}
	
	/**
	 * Test Create Query when offset parameter is null or empty and limit parameters is set
	 */
	@Test
	void testCreateQueryLimit() {
		String offset = null;
		String limit = "100";
		Mockito.doReturn(typedQuery).when(entityManager).createQuery(query);
		Mockito.doReturn(typedQuery).when(typedQuery).setFirstResult(0);
		Mockito.doReturn(typedQuery).when(typedQuery).setMaxResults(100);
		csvService.createQuery(query, offset, limit);
		verify(entityManager, times(1)).createQuery(query);
		verify(typedQuery, times(1)).setFirstResult(0);
		verify(typedQuery, times(1)).setMaxResults(100);
		verify(typedQuery, times(1)).getResultList();
	}
	
	/**
	 * Test searching the statistics when all the parameters are set.
	 * Is checking that all the methods are executed: handleDisplay, handleGroupBy, handleCondition, handleOrderBy, createQuery
	 */
	@Test
	void testSearchStatistics() {
		String display = "clicks:sum,datasource,campaign"; 
		String condition = "clicks>0";
		String groupBy = "datasource,campaign";
		String orderBy = "clicks:sum:desc";
		String offset = "100";
		String limit = "200";
		String showSQL = null;
		Mockito.doReturn(builder).when(entityManager).getCriteriaBuilder();
		Mockito.doReturn(query).when(builder).createQuery(Object.class);
		Mockito.doReturn(stat).when(query).from(Statistic.class);
		Mockito.doReturn(typedQuery).when(entityManager).createQuery(query);
		Mockito.doReturn(typedQuery).when(typedQuery).setFirstResult(100);
		Mockito.doReturn(typedQuery).when(typedQuery).setMaxResults(200);
		ArrayList<String> statistics = new ArrayList<String>();
		statistics.add("Result");
		Mockito.doReturn(statistics).when(typedQuery).getResultList();
		String template = csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL, model);
		assertEquals(template, "query-results");
		Mockito.verify(model).addAttribute("statistics", statistics);
		// We have an any() for each column to be displayed, groupedBy, orderBy
		verify(query, times(1)).multiselect(any(), any(), any());
		verify(query, times(1)).groupBy(any(), any());
		List<Order> orders = new ArrayList<Order>();
		orders.add(null);
		verify(query, times(1)).orderBy(orders);
		Predicate predicate = null;
		verify(query, times(1)).where(predicate);
		verify(entityManager, times(1)).createQuery(query);
	}
	
	/**
	 * Test searching the statistics when all the parameters are null or not set
	 * It only focuses on returning the correct template and that is setting correctly the statistics model attribute
	 */
	@Test
	void testSearchStatisticsNull() {
		String display = null; 
		String condition = null;
		String groupBy = null;
		String orderBy = null;
		String offset = null;
		String limit = null;
		String showSQL = null;
		Mockito.doReturn(builder).when(entityManager).getCriteriaBuilder();
		Mockito.doReturn(query).when(builder).createQuery(Object.class);
		Mockito.doReturn(stat).when(query).from(Statistic.class);
		Mockito.doReturn(typedQuery).when(entityManager).createQuery(query);
		Mockito.doReturn(typedQuery).when(typedQuery).setFirstResult(0);
		Mockito.doReturn(typedQuery).when(typedQuery).setMaxResults(100);
		ArrayList<String> statistics = new ArrayList<String>();
		statistics.add("Result");
		Mockito.doReturn(statistics).when(typedQuery).getResultList();
		String template = csvService.searchStatistics(display, condition, groupBy, orderBy, offset, limit, showSQL, model);
		assertEquals(template, "query-results");
		Mockito.verify(model).addAttribute("statistics", statistics);
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
