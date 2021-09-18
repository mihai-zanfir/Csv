package com.adverity.csv.model;

import java.io.Serializable;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "STATISTIC")
public class Statistic implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "DATASOURCE", nullable = false)
	private String datasource;
	
	@Column(name = "CAMPAIGN", nullable = false)
	private String campaign;
	
	@Column(name = "DAILY", nullable = false)
	private Date daily;
	
	@Column(name = "CLICKS", nullable = false)
	private int clicks;
	
	@Column(name = "IMPRESSIONS", nullable = false)
	private int impressions;
}
