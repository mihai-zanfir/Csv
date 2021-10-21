package com.adverity.csv.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisplayColumn {
	private String column;
	private String function;
	
	public DisplayColumn(String allColumn) {
		String[] parts = allColumn.split(":");
		if (parts.length == 2) {
			function = parts[1];
		} else {
			function = "";
		}
		column = parts[0];
	}
}