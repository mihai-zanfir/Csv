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
The test CSV has 23198 different records.

If there is no upload UI than, postman app can be used for uploading the CSV file via the following link:
http://localhost:8080/api/upload-csv-file?file=
You need to set a "Body" parameter for "form-data" named "file" and is a File type.

The uploaded Csv file is processed and its records are stored in the database.
After uploading you should receive a message like the following: Successful saved in database: 23198
23198 is the number of records it saved in the database.

The database is H2 which is an embeddable database and it runs on your PC memory.
At restart of this application, the database will also be removed so you will have to upload the Csv file one more time.
We could set it a file database so that its data won't be lost when the app is restarted.

How do you query the database?

First I'll give 3 links for the requested queries:

a.Total Clicks for a given Datasource for a given Date range:

http://localhost:8080/api/search?display=SUM(CLICKS)&condition=DATASOURCE='Google Ads' AND DAILY>'2020-01-01' AND DAILY<'2020-01-31'&showSQL=true

The associated SQL query is:

SELECT SUM(CLICKS) FROM STATISTIC WHERE DATASOURCE='Google Ads' AND DAILY>'2020-01-01' AND DAILY<'2020-01-31'

b.Click-Through Rate (CTR) per Datasource and Campaign:

http://localhost:8080/api/search?display=SUM(CLICKS),DATASOURCE,CAMPAIGN&groupBy=DATASOURCE,CAMPAIGN&orderBy=1 DESC&showSQL=true

The associated SQL query is:

SELECT SUM(CLICKS),DATASOURCE,CAMPAIGN FROM STATISTIC GROUP BY DATASOURCE,CAMPAIGN ORDER BY 1 DESC

c.Impressions over time (daily):

http://localhost:8080/api/search?display=DAILY,SUM(IMPRESSIONS)&groupBy=DAILY&orderBy=1 DESC&showSQL=true

The associated SQL query is:

SELECT DAILY,SUM(IMPRESSIONS) FROM STATISTIC GROUP BY DAILY ORDER BY 1 DESC

Additional help for queries:

If you add the showSQL=true parameter, you will get the SQL that is executed in the database for your query
and the number of records you got on execution.
The values of the parameters are similar with the ones you would use for a SQL query so keep in mind that H2 database is used so that, if you need something more complex, you need to look for the H2 SQL commands.

What parameters can you use?
	 @param display The columns we want to be displayed on results. Can also be COUNT(*), SUM() or a comma separated list of columns
	  		If this is not filled than it will display all the columns.
	  		Ex: COUNT(DATASOURCE)
	  		Ex: DATASOURCE
	  		Ex: SUM(IMPRESSIONS)
	  
	 @param condition If this is used it will apply a condition used as WHERE or HAVING in the SQL query
	  		Ex: CAMPAIGN='Adventmarkt Touristik'
	  		Ex: CLICKS>10
	  		Ex: DAILY>'2020-02-14'
	  
	 @param groupBy If this is used than will GROUP BY after columns specified in this parameter. Can be a comma separated list of columns
	       Ex: DATASOURCE,CAMPAIGN
	       
	 @param orderBy If this is used than it will order the results based on this parameter.
	  		This can be used for ascending or descending order using ASC and DESC words after the column names 
	  		Ex: CAMPAIGN ASC or CAMPAIGN DESC
	  
	 @param offset If this is set than it will show the records starting from this offset. It needs to be a number.
	  		If is not set this will be 0. Using this parameter, Pagination could be easily created.
	  		Ex: 100 - it will display the records starting from this offset
	  
	 @param limit If this is set than it will limit the records that is showing.
	  		If is not set than for security reason it will limit the records to a configured limit which is 1000. This value can be changed in the application.
	  		Using this parameter, Pagination could be easily created.
	  		Ex: 10000 - it will limit the returned records to 10000
	  		Ex: no -it will return all the records (be careful with this as if there are many records it will slow your browser)
	  
	 @param showSQL If set to true, it will show the SQL generated on the top of records and the number of records found

What column names could you use?

The column name that could be used here are: DATASOURCE, CAMPAIGN, DAILY, CLICKS, IMPRESSIONS, ID

These parameters offer a generic custom way to implement a huge variety of queries.
They are combined and in the end the app is creating a SQL query that is executed in the database.
The results will be sent to the requester.
Remember that I put a limit of 1000 for the displayed records.
Take a look at limit parameter in case you would like to change the displayed records.
Using offset and limit parameters, Pagination could be easily created so that to bring small pages of data.
This is useful when there are a lot of records and you would like to go through them in bulks (pages).
Angular for instance has a Material Table UI component that has pagination already implemented and it only need these 2 parameters to be able to have a working Pagination.

How to query the database?

This app contains a H2 console that can be used to execute SQL queries over the STATISTIC table that is created and used by this application.
In order to access it you could go to the following url:
http://localhost:8080/h2-console/
Use the following login parameters:
JDBC URL: jdbc:h2:mem:testdb
Username: sa
There is no password for now

Swagger Open API UI:

There is also available a user interface for testing the Csv API through the Swagger Open API:
http://localhost:8080/swagger-ui.html
or
http://localhost:8080/swagger-ui/index.html?configUrl=/csv-openapi/swagger-config

Improvements that can be done:

1. Create a nice UI Frontend interface:
For Upload I would create a component having a progress bar so that to see the time when the CSV is uploaded (or processed). 
For Queries I would create a UI with some WYSWG components (ex: column names, *, COUNT, SUM, etc.) so that to create these queries easier.

2. Write some code to prevent SQL Injection

3. Save the data from the CSV in database in bulks (small groups) using batches and transaction

4. Better Error Handling and response messaging

5. Using a relational database as: Oracle, Microsoft SQL Server, MySQL, etc so that the processed inserted data is not lost.

6. Writing Unit Tests using JUnit

7. Writing functional tests using Groovy

8. I would create some indexes on the database table so that the queries will run faster. The inserts will take longer but the read and searches in the database will be faster than before.

9. Since DATASOURCE CAMPAIGN contain a limited number of values, I would normalize the table and put these on their own tables so in the end it will be 3 database tables
At processing CSV time, we will check these values with the existing values and if it does not found the value in these table, it will insert them
This will be replaced in the main table with 2 ID columns that will be linked with the 2 tables via some foreign keys.

10. Cache the database entitities and requests to the backend so that similar future requets will not be executed in the database but brought from the cache

11. 


If you have more questions, free free to ask me.

Thanks,

Mihai Zanfir

z_mihai_c@yahoo.com

https://www.linkedin.com/in/mihai-zanfir-44671194/
