package com.adverity.csv.model;

import com.opencsv.bean.CsvBindByName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticCsv {

	@CsvBindByName(column = "Datasource")
	private String datasource;
	
	@CsvBindByName(column = "Campaign")
	private String campaign;
	
	@CsvBindByName(column = "Daily")
	private String daily;
	
	@CsvBindByName(column = "Clicks")
	private int clicks;
	
	@CsvBindByName(column = "Impressions")
	private int impressions;	
}
