set default_parallel 10;

data_2 = LOAD 'network_trace.txt' USING PigStorage(' ') AS (
time:chararray,
ip:chararray,
source:chararray,
s:chararray,
destination:chararray,
protocol:chararray,
data:chararray);

tcp = FILTER data_2 BY protocol == 'tcp';

corrected = FOREACH tcp GENERATE REPLACE(source, '(.([0-9]+|[0-9]+:))$', '') AS source, REPLACE(destination,'(.([0-9]+|[0-9]+:))$', '') AS destination;

sources1 = GROUP corrected by source;

counts = FOREACH sources1 {
uniques = DISTINCT corrected.destination;
GENERATE group as source, COUNT(uniques) as destCount;
}

sorted = ORDER counts BY destCount DESC;
top = LIMIT sorted 10;
STORE top INTO '/user/cpre419/lab4/exp2/output/';
