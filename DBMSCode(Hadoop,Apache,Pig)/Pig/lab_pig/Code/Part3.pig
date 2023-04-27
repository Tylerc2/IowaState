set default_parallel 10;


data_3 = LOAD 'ip_trace.txt' USING PigStorage(' ') AS (
time:chararray,
connection_id:chararray,
source:chararray,
s:chararray,
destination:chararray,
protocol:chararray,
data:chararray);

data_4 = LOAD 'raw_block.txt' USING PigStorage(' ') AS (
connection_id:chararray,
action:chararray);

data_31 = FOREACH data_3 GENERATE time, connection_id, source, destination;

data_41 = FILTER data_4 BY action == 'Blocked';

joined = JOIN data_31 BY connection_id, data_41 BY connection_id;

wall = FOREACH joined GENERATE data_31::time as time, data_31::connection_id as connection_id, data_31::source as source, data_31::destination as destination, data_41::action as action;

STORE wall INTO '/user/cpre419/lab4/exp3/firewall';



blocked = FOREACH wall GENERATE connection_id, source;


grouped = GROUP blocked by source;

sources = FOREACH grouped {
unique_times = DISTINCT blocked.connection_id;
GENERATE group as source, COUNT(unique_times) as blockedCount;
}

daSort = ORDER sources BY blockedCount DESC;

STORE daSort INTO '/user/cpre419/lab4/exp3/output';


