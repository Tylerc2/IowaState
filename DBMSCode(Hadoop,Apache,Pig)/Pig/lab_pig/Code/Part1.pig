data_land = LOAD 'gaz_tracts_national.txt' USING PigStorage('\t') AS (
USPS:chararray,
GEOID:int,
POP10:int,
HU10:int,
ALAND:long,
AWATER:long,
ALAND_SQMI:double,
AWATER_SQMI:double,
INTPTLAT:double,
INTPTLONG:double);

grouped = GROUP data_land BY USPS;

temp = FOREACH grouped GENERATE group, SUM(data_land.ALAND) AS sum;

ordered = ORDER temp BY sum DESC;

head = LIMIT ordered 10;

STORE head INTO 'lab4P1';
