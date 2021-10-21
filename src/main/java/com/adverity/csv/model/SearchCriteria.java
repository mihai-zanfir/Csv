package com.adverity.csv.model;

import com.adverity.csv.util.SearchOperation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchCriteria {
	private String key;
	private SearchOperation operation;
	private Object value;
	private boolean orPredicate;
	
    public SearchCriteria(final String key, final SearchOperation operation, final Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }
    
    public SearchCriteria(final String orPredicate, final String key, final SearchOperation operation, final Object value) {
        this.orPredicate = SearchOperation.OR_PREDICATE_FLAG.equals(orPredicate);
        this.key = key;
        this.operation = operation;
        this.value = value;
    }
    
    public SearchCriteria(final String orPredicate, final String key, final String operation, 
    					  final String prefix, final String value, final String suffix) {
        SearchOperation op = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (op != null) {
            if (op == SearchOperation.EQUALITY) { // the operation may be complex operation
                final boolean startWithAsterisk = prefix != null && prefix.contains(SearchOperation.ZERO_OR_MORE_REGEX);
                final boolean endWithAsterisk = suffix != null && suffix.contains(SearchOperation.ZERO_OR_MORE_REGEX);
                if (startWithAsterisk && endWithAsterisk) {
                    op = SearchOperation.CONTAINS;
                } else if (startWithAsterisk) {
                    op = SearchOperation.ENDS_WITH;
                } else if (endWithAsterisk) {
                    op = SearchOperation.STARTS_WITH;
                }
            }
        }
        this.orPredicate = SearchOperation.OR_PREDICATE_FLAG.equals(orPredicate);
        this.key = key;
        this.operation = op;
        this.value = value;
    }
}