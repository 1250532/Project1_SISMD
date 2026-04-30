error id: file://<WORKSPACE>/src/ApplyFilters.java:_empty_/Filters#HistogramFilter#
file://<WORKSPACE>/src/ApplyFilters.java
empty definition using pc, found symbol in pc: _empty_/Filters#HistogramFilter#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 445
uri: file://<WORKSPACE>/src/ApplyFilters.java
text:
```scala
import java.io.IOException;
import java.util.Scanner;

public class ApplyFilters {
 
    public static void main(String[] args) throws IOException {
		
		Scanner input = new Scanner(System.in);        
		String filePath = "";
		System.out.println("Insert the name of the file path you would like to use.");
        filePath = input.nextLine();
        input.close();
        Filters filters = new Filters(filePath);
        filters.@@HistogramFilter("output.jpg",128);
    }

} 

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/Filters#HistogramFilter#