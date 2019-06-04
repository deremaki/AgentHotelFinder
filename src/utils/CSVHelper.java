package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVHelper {

    public static List<List<String>> ReadCsv(String path) {
        StringBuilder stringBuilder = new StringBuilder();
        List<List<String>> records = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                records.add(Arrays.asList(values));
            }
            br.close();
        } catch (Exception ee) {
        }
        return records;
    }

    public static void WriteCsc(List<List<String>> data, String path) {
        File csvOutputFile = new File(path);
        try (PrintWriter writer = new PrintWriter(csvOutputFile)) {

            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                data.stream().map((i) -> convertToCSV(i)).forEach(writer::println);
            }

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String convertToCSV(List<String> data) {
        return String.join(",", data);
    }
}
