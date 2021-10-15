package com.adverity.csv.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.adverity.csv.model.Statistic;
import com.adverity.csv.model.StatisticCsv;

/**
 * Class for mapping different objects
 * For now this is used to converts a list of StatisticCsv into a list of Statistic entities
 * 
 * @author Mihai Zanfir
 */
@Mapper(componentModel = "spring")
public abstract class StatisticMapper {

	/**
	 * Converts a list of StatisticCsv into a list of Statistic entities
	 * 
	 * @param source A list of StatisticCsv to be converted
	 * @return A list of Statistic entities
	 */
	@Mapping(target = "daily", source = "source")
    public abstract List<Statistic> mapListEntityCsvToListEntity(List<StatisticCsv> source);

	/**
	 * Maps a Daily string that is coming from CSV into a daily Date used on Statistic entity
	 * 
	 * @param date a Daily string that is coming from CSV in the following format "01/31/19"
	 * @return a converted Date value of form "2019-01-31"
	 */
	LocalDate mapDaily(String date) {
		return LocalDate.parse(date, DateTimeFormatter.ofPattern("MM/dd/yy"));
    }
}
