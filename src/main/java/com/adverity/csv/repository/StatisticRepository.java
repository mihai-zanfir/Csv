package com.adverity.csv.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.adverity.csv.model.Statistic;

public interface StatisticRepository extends JpaRepository<Statistic, Integer> {

	Optional<List<Statistic>> findByCampaign(String query);

}
