# Yaml Parser Benchmarks

### How to run 

To execute the benchmark, run the following commands in the root of the repository:
 - `./gradlew jmhJar`
 - `java -jar yamlify-bench/build/libs/yamlify-bench-jmh.jar -i 10 -wi 4 -f 1 -r 2 -w 2 -tu ms`


### Yaml Parser Benchmarks


The Cojen and Reflect parsers are generic parsers that can be used to parse any domain class.

The Baseline parsers are specific to the domain class they are parsing. 

The following benchmarks show the average results for 10 iterations of 2 second runs for each parser implementation. The benchmark was run on a machine with a Ryzen 5 5600 CPU and 16GB 3200mhz RAM.

### Savings Account Benchmark

#### Savings Account Class Structure
 
The Savings Account class has the following structure:

```kotlin
class SavingsAccount(
    val accountCode: Short,
    val holderName: String,
    val balance: Long,
    val isActive: Boolean,
    val interestRate: Double,
    val transactionLimit: Int,
    val withdrawLimit: Int,
)
```

#### Benchmark Results and Analysis

| Parser   | Operations per millisecond | Relative Performance |
|----------|----------------------------|----------------------|
| Baseline | 521.963 ± 1.463            | 1.000                |
| Cojen    | 514.603 ± 1.589            | 0.986                |
| Reflect  | 436.399 ± 0.837            | 0.836                |

It can be observed that the Cojen parser's performance is within the margin of error of the baseline parser.

This is due to the fact that the Cojen parser dynamically generates code to instantiate the domain class at runtime
during its initial invocation. Once this code is generated, subsequent calls to the parser benefit from utilizing
the pre-generated code, eliminating the overhead of reflection. 

The Reflect parser, on the other hand, shows slower performance compared to both the baseline and Cojen parsers.

Unlike the Cojen parser, the Reflect parser relies on reflection to instantiate the domain class every time it is called 
through the `callBy()` method, which is used to invoke the constructor of the domain class with the parsed values.
This repeated use of reflection results in a performance overhead, leading to the observed results.

### Student Benchmark

##### Student Class Structure
The Student class is a more complex class compared to the Savings Account class. It has the following structure:
```kotlin
class Student (
    val name: String,
    val nr: Int,
    val from: String,
    val address: Address? = null,
    val grades: List<Grade> = emptyList()
)
class Grade(
    val subject: String,
    val classification: Int
)
class Address(
    val street: String, 
    val number: Int, 
    val city: String
)
```
##### Benchmark Results and Analysis
| Parser   | Operations per millisecond | Relative Performance |
|----------|----------------------------|----------------------|
| Baseline | 306.275 ± 1.646            | 1.000                |
| Cojen    | 301.828 ± 0.678            | 0.985                |
| Reflect  | 231.036 ± 0.485            | 0.754                |

Once again, the Cojen parser's performance is close to the baseline parser's performance.

The Reflect parser's performance is again, slower than the baseline and Cojen parsers, with a slight decrease in relative performance compared to the Savings Account benchmark. 

This slight decrease in relative performance for the Reflect parser can be attributed to the increased complexity of the Student class compared to the Savings Account class. 
The Student class has a nested Address class property and a list of instances of the Grade class, which requires additional parser calls and instantiation steps.

These additional steps result in a slight decrease in performance for the reflect parser, which relies on reflection for instantiation through the constructor `callBy()` method.

### Conclusion

The benchmarks show that the Cojen parser is a viable alternative to the Baseline parser for both of these class structures, offering similar performance without the overhead of reflection or 
the need to implement a specific parser for each domain class.

The Reflect parser, on the other hand, shows slightly slower performance due to its reliance on reflection for instantiation. However,
it is still an alternative to handle the parsing of domain classes without the need to implement a specific parser for each domain class, 
though it is not as performant as the Cojen parser.

