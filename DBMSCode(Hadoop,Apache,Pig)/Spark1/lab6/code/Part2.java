import java.util.Arrays;
import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import java.util.*;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;
import scala.Tuple3;

public class Part2 {

	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage <input> <output>");
			System.exit(1);
		}
		SparkConf sparkConf = new SparkConf().setAppName("part 2");
		JavaSparkContext context = new JavaSparkContext(sparkConf);

		JavaRDD<String> file = context.textFile(args[0]);
		JavaRDD<String> cleaned = file.filter(new Function<String, Boolean>() {
			public Boolean call(String s) {
				return !s.isEmpty();
			}
		});

		JavaPairRDD<Long, Long> edges = cleaned.flatMapToPair(new PairFlatMapFunction<String, Long, Long>() {
			@SuppressWarnings("unchecked")
			public Iterator<Tuple2<Long, Long>> call(String s) {
				String[] st = s.trim().replaceAll("\\s+", " ").split(" ");
				return Arrays.asList(new Tuple2<Long, Long>(Long.parseLong(st[0]), Long.parseLong(st[1])),
						new Tuple2<Long, Long>(Long.parseLong(st[1]), Long.parseLong(st[0]))).iterator();
			}
		});

		JavaPairRDD<Long, Iterable<Long>> tres = edges.groupByKey();

		JavaPairRDD<Long, Tuple2<Long, Iterable<Long>>> hold = tres.flatMapToPair(
				new PairFlatMapFunction<Tuple2<Long, Iterable<Long>>, Long, Tuple2<Long, Iterable<Long>>>() {
					public Iterator<Tuple2<Long, Tuple2<Long, Iterable<Long>>>> call(Tuple2<Long, Iterable<Long>> s) {
						Iterable<Long> values = s._2;
						List<Tuple2<Long, Tuple2<Long, Iterable<Long>>>> output = new ArrayList<Tuple2<Long, Tuple2<Long, Iterable<Long>>>>();

						for (Long value : values) {
							output.add(new Tuple2<Long, Tuple2<Long, Iterable<Long>>>(value, s));
						}

						return output.iterator();
					}
				});

		JavaPairRDD<Long, Iterable<Tuple2<Long, Iterable<Long>>>> grouped = hold.groupByKey();

		JavaRDD<Tuple3<Long, Long, Long>> hold2 = grouped.flatMap(
				new FlatMapFunction<Tuple2<Long, Iterable<Tuple2<Long, Iterable<Long>>>>, Tuple3<Long, Long, Long>>() {
					public Iterator<Tuple3<Long, Long, Long>> call(
							Tuple2<Long, Iterable<Tuple2<Long, Iterable<Long>>>> s) {

						long key = s._1;
						Iterable<Tuple2<Long, Iterable<Long>>> val = s._2;
						HashSet<Long> set = new HashSet<Long>();
						for (Tuple2<Long, Iterable<Long>> value : val) {
							set.add(value._1);
						}

						List<Tuple3<Long, Long, Long>> output = new ArrayList<Tuple3<Long, Long, Long>>();
						for (Tuple2<Long, Iterable<Long>> value : val) {
							for (Long temp : value._2) {
								if (set.contains(temp)) {
									Long[] tres = { key, value._1, temp };
									Arrays.sort(tres);
									output.add(new Tuple3<Long, Long, Long>(tres[0], tres[1], tres[2]));
								}
							}
						}
						return output.iterator();
					}
				});

		JavaRDD<Tuple3<Long, Long, Long>> hold3 = hold2.distinct();

		List<Long> list = new ArrayList<Long>();
		list.add(hold3.count());
		JavaRDD<Long> result = context.parallelize(list);
		System.out.println(result);
		result.saveAsTextFile(args[1]);
		context.stop();
		context.close();
	}
}