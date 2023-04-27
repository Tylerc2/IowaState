import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

public class Part1 {
	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Use: <input> <Output>");
			System.exit(1);
		}

		SparkConf config = new SparkConf().setAppName("part1");

		JavaSparkContext context = new JavaSparkContext(config);

		JavaRDD<String> in = context.textFile(args[0]);

		JavaPairRDD<String, String> github = in.mapToPair(new PairFunction<String, String, String>() {
			public Tuple2<String, String> call(String s) {
				String[] st = s.split(",");
				return new Tuple2<String, String>(st[1], s);
			}
		});

		JavaPairRDD<String, Iterable<String>> sort = github.groupByKey();

		JavaPairRDD<Long, String> output = sort
				.mapToPair(new PairFunction<Tuple2<String, Iterable<String>>, Long, String>() {
					public Tuple2<Long, String> call(Tuple2<String, Iterable<String>> hold) {
						String language = "";
						long num_repo = 0;
						long stars = 0;
						String name_of_repo_highest_star = "";

						for (String s : hold._2) {
							String[] st = s.split(",");
							num_repo++;
							if (Long.valueOf(st[12]) > stars) {
								stars = Long.valueOf(st[12]);
								language = st[1];
								name_of_repo_highest_star = st[0];
							}
						}

						return new Tuple2<Long, String>(num_repo, language + ": " + "Number of Repos: " + num_repo + " "
								+ "Name of Repo With Most Stars: " + name_of_repo_highest_star + " " + stars);
					}
				}

				);

		JavaPairRDD<Long, String> sort1 = output.sortByKey(false);
		sort1.values().saveAsTextFile(args[1]);
		context.stop();
		context.close();
	}
}
