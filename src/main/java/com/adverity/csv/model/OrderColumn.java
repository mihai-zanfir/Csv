package com.adverity.csv.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderColumn {
	private String column;
	private String function;
	private String direction;
	
	public OrderColumn(String allColumn) {
		String[] parts = allColumn.split(":");
		if (parts.length == 3) {
			function = parts[1];
			if ("desc".equalsIgnoreCase(parts[2])) {
				direction = "desc";
			} else {
				direction = "asc";
			}
		} else if (parts.length == 2) {
			function = "";
			if ("desc".equalsIgnoreCase(parts[1])) {
				direction = "desc";
			} else {
				direction = "asc";
			}
		} else {
			function = "";
			direction = "asc";
		}
		column = parts[0];
	}
}