package com.adverity.csv.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.adverity.csv.model.Statistic;

public interface StatisticRepository extends JpaRepository<Statistic, Integer>, JpaSpecificationExecutor<Statistic> {

	Optional<List<Statistic>> findByCampaign(String query);

}
