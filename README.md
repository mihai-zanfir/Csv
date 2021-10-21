CSV Statistics created for Adverity

Author Mihai Zanfir

https://www.linkedin.com/in/mihai-zanfir-44671194/


This code is a web application for an API for importing a Csv file and Querying statistics.

1. First operation that needs to be done is to upload a CSV file.
2. After that, the database can be queried using multiple parameters.

The CSV format is the one Adverity provided as in the following example:
Datasource,Campaign,Daily,Clicks,Impressions
Google Ads,Adventmarkt Touristik,11/12/19,7,22425
Google Ads,Adventmarkt Touristik,11/13/19,16,45452
The test CSV has 23198 different records. You can find this test Csv file on the root on GitHub (same folder with this README file).

I also did a UI interface for uploading the Csv. Click on the following link:

http://zanfir-mihai.go.ro:8080

The uploaded Csv file is processed and its records are stored in the database.
After uploading you should receive a message like the following: Successful saved in database: 23198
23198 is the number of records it saved in the database.

The database is H2 which is an embeddable database and it runs on your PC memory.
At restart of this application, the database will also be removed so you will have to upload the Csv file one more time.
We could set it a file database so that its data won't be lost when the app is restarted.

How do you query the database?

First I'll give 3 links for the requested queries:

a.Total Clicks for a given Datasource for a given Date range:

http://zanfir-mihai.go.ro:8080/api/search?display=clicks:sum&condition=datasource:Google Ads,daily>01-01-2020,daily<01-31-2020

The associated SQL query is:

SELECT SUM(CLICKS) FROM STATISTIC WHERE DATASOURCE='Google Ads' AND DAILY>'2020-01-01' AND DAILY<'2020-01-31'

b.Click-Through Rate (CTR) per Datasource and Campaign:

http://zanfir-mihai.go.ro:8080/api/search?display=clicks:sum,datasource,campaign&groupBy=datasource,campaign&orderBy=clicks:sum:desc

The associated SQL query is:

SELECT SUM(CLICKS),DATASOURCE,CAMPAIGN FROM STATISTIC GROUP BY DATASOURCE,CAMPAIGN ORDER BY 1 DESC

c.Impressions over time (daily):

http://zanfir-mihai.go.ro:8080/api/search?display=daily,impressions:sum&groupBy=daily&orderBy=daily:desc

The associated SQL query is:

SELECT DAILY,SUM(IMPRESSIONS) FROM STATISTIC GROUP BY DAILY ORDER BY 1 DESC

Additional help for queries:

What parameters can you use?
	 @param display The columns we want to be displayed on results (comma separated).Can also be the name of one columnn followed by :sum (For a SUM on that column values) or a comma separated list of columns
	  		If this is not filled than it will display all the columns.
	  		Ex: datasource
	  		Ex: impressions:sum
			Ex: daily,impressions:sum
	  
	 @param condition If this is used it will apply a condition used as WHERE (or HAVING) in the SQL query
	  		Ex: campaign:'Adventmarkt Touristik'
	  		Ex: clicks>10
	  		Ex: daily>'2020-02-14'
	  
	 @param groupBy If this is used than will GROUP BY after columns specified in this parameter. Can be a comma separated list of columns
	       Ex: datasource,campaign
	       
	 @param orderBy If this is used than it will order the results based on this parameter.
	  		This can be used for ascending or descending order using asc and desc words after the column names 
	  		Ex: campaign:asc or campaign:desc
			If you displayed a SUM on a column then this columns should also contain :sum in it
			Ex: clicks:sum:desc
	  
	 @param offset If this is set than it will show the records starting from this offset. It needs to be a number.
	  		If is not set this will be 0. Using this parameter, Pagination could be easily created.
	  		Ex: 100 - it will display the records starting from this offset
	  
	 @param limit If this is set than it will limit the records that is showing.
	  		TO DO Maybe! If is not set than for security reason it should limit the records to a configured limit which is 1000. This value can be changed in the application.
	  		Using this parameter, Pagination could be easily created.
	  		Ex: 10000 - it will limit the returned records to 10000
	  		Ex: If is not set then it will return all the records (be careful with this as if there are many records it will slow your browser)
	  
	 @param showSQL This is not working anymore for now. If set to true, it would show the SQL generated on the top of records and the number of records found

What column names could you use?

The column name that could be used here are: datasource, campaign, daily, clicks, impressions, id.
If we need to display multiple columns then, the column definitions need to be separated by comma.
In case we need to show a sum column (see SQL SUM), it needs to have the following form (Ex: clicks:sum).

What operators could you use?
: - equality (Ex: datasource:Google Ads) - this is for records that are equal with Google Ads
! - negation (Ex: datasource!Google Ads) - this is for records that are not equal with Google Ads
> - greater than (works fine for numbers or dates) (Ex: daily>01-01-2020)
< - lower than  (works fine for numbers or dates) (Ex: daily<01-31-2020)
~ - like (Ex: datasource~Google Ads) - this is for records that are like Google Ads (see SQL LIKE)
* - if used at the begining of the word it will search for words starting with (Ex: datasource:*Ads)
* - if used at the ending of the word it will search for words ending with (Ex: datasource:Google*)
* - if used at the begining of the word and at the ending of the word it will search for words containing this word (Ex: datasource:*oogle*)

These are concatenating these conditions using AND but we can also use OR by putting ' in front of the individual condition:
Ex: datasource:Google Ads,daily>01-01-2020,daily<01-31-2020 - This is for getting the records that have datasource equal with "Google Ads" AND the daily date is in the range we need.
Ex: datasource:Google Ads,'daily>01-01-2020 - This is for getting the records that have datasource equal with "Google Ads" OR the daily date is greater than 01-01-2020.

These parameters offer a generic custom way to implement a huge variety of queries.
They are combined and in the end the app is creating a SQL query that is executed in the database.
The results will be sent to the requester.
Take a look at limit parameter in case you would like to change the limit of the displayed records.
Using offset and limit parameters, Pagination could be easily created so that to bring small pages of data.
This is useful when there are a lot of records and you would like to go through them in bulks (pages).
Angular for instance has a Material Table UI component that has pagination already implemented and it only need these 2 parameters to be able to have a working Pagination.

How to query the database?

This app contains a H2 console that can be used to execute SQL queries over the STATISTIC table that is created and used by this application.
In order to access it you could go to the following url:
http://zanfir-mihai.go.ro:8080/h2-console/
Use the following login parameters:
JDBC URL: jdbc:h2:mem:testdb
Username: sa
There is no password for now
There is a screenshot for this database login screen.

Swagger Open API UI:

There is also available a user interface for testing the Csv API through the Swagger Open API:
http://zanfir-mihai.go.ro:8080/swagger-ui.html

Last version of the app prevents SQL Injection!

Improvements that can be done:

1. Create a nice UI Frontend interface:
For Upload I would create a component having a progress bar so that to see the time when the CSV is uploaded (or processed). 
For Queries I would create a UI with some WYSWG components (ex: column names, *, COUNT, SUM, etc.) so that to create these queries easier.

2. Save the data from the CSV in database in bulks (small groups) using batches and transaction

3. Better Error Handling and response messaging

4. Using a relational database as: Oracle, Microsoft SQL Server, MySQL, etc so that the processed inserted data is not lost.

5. Writing Unit Tests using JUnit

6. Writing functional tests using Groovy

7. I would create some indexes on the database table so that the queries will run faster. The inserts will take longer but the read and searches in the database will be faster than before.

8. Since DATASOURCE CAMPAIGN contain a limited number of values, I would normalize the table and put these on their own tables so in the end it will be 3 database tables
At processing CSV time, we will check these values with the existing values and if it does not found the value in these table, it will insert them
This will be replaced in the main table with 2 ID columns that will be linked with the 2 tables via some foreign keys.

9. Cache the database entitities and requests to the backend so that similar future requets will not be executed in the database but brought from the cache

10. Other improvements and refactoring of code

11. 


If you have more questions, free free to ask me.

Thanks,

Mihai Zanfir

z_mihai_c@yahoo.com

https://www.linkedin.com/in/mihai-zanfir-44671194/
