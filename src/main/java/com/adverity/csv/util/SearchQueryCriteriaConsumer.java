package com.adverity.csv.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.adverity.csv.model.SearchCriteria;

public class SearchQueryCriteriaConsumer implements Consumer<SearchCriteria> {
	private Predicate predicate;
	private CriteriaBuilder builder;
	private Root stat;

	public SearchQueryCriteriaConsumer(Predicate predicate, CriteriaBuilder builder, Root stat) {
        super();
        this.predicate = predicate;
        this.builder = builder;
        this.stat= stat;
    }
	
	@Override
	public void accept(SearchCriteria param) {
		switch (param.getOperation()) {
		case EQUALITY:
			if ("daily".equalsIgnoreCase(param.getKey())) {
				predicate = andOr(param, builder.equal(stat.get(param.getKey()), mapDaily(param.getValue().toString())));
			} else {
				predicate = andOr(param, builder.equal(stat.get(param.getKey()), param.getValue()));
			}
			break;
		case NEGATION:
			predicate = andOr(param, builder.notEqual(stat.get(param.getKey()), param.getValue()));
			break;
		case GREATER_THAN:
			if ("daily".equalsIgnoreCase(param.getKey())) {
				predicate = andOr(param, builder.greaterThan(stat.get(param.getKey()), mapDaily(param.getValue().toString())));
			} else {
				predicate = andOr(param, builder.greaterThan(stat.<String>get(param.getKey()), param.getValue().toString()));
			}
			break;
		case LESS_THAN:
			if ("daily".equalsIgnoreCase(param.getKey())) {
				predicate = andOr(param, builder.lessThan(stat.get(param.getKey()), mapDaily(param.getValue().toString())));
			} else {
				predicate = andOr(param, builder.lessThan(stat.<String>get(param.getKey()), param.getValue().toString()));
			}
			break;
		case LIKE:
			predicate = andOr(param, builder.like(stat.<String>get(param.getKey()), param.getValue().toString()));
			break;
		case STARTS_WITH:
			predicate = andOr(param, builder.like(stat.<String>get(param.getKey()), param.getValue() + "%"));
			break;
		case ENDS_WITH:
			predicate = andOr(param, builder.like(stat.<String>get(param.getKey()), "%" + param.getValue()));
			break;
		case CONTAINS:
			predicate = andOr(param, builder.like(stat.<String>get(param.getKey()), "%" + param.getValue() + "%"));
			break;
		default:
		}
	}

	public Predicate andOr(SearchCriteria param, Predicate newPredicate) {
		if (param.isOrPredicate()) {
			return builder.or(predicate, newPredicate);
		}
		return builder.and(predicate, newPredicate);
	}
	
	LocalDate mapDaily(String date) {
		return LocalDate.parse(date, DateTimeFormatter.ofPattern("MM-dd-yyyy"));
	}
	
    public Predicate getPredicate() {
        return predicate;
    }
}