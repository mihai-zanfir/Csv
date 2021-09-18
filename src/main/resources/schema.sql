drop table if exists STATISTIC;

CREATE TABLE IF NOT EXISTS STATISTIC
(
	ID              integer  NOT NULL  IDENTITY ( 1,1 ),
	DATASOURCE      varchar(50) NOT NULL,
	CAMPAIGN        varchar(250) NOT NULL,
	DAILY           date NOT NULL,
	CLICKS          integer NOT NULL,
	IMPRESSIONS     integer  NOT NULL
);
