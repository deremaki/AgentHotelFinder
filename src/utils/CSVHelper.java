package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
        } catch (Exception ee) {

        }
        return records;
    }

    public static void WriteCsc(List<List<String>> data, String path) {
        File csvOutputFile = new File(path);
        try (PrintWriter writer = new PrintWriter(csvOutputFile)) {
            data.stream().map((i) -> convertToCSV(i)).forEach(writer::println);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void AppendCsv(List<List<String>> data, String path, String city) {
        if (data.size() == 0) {
            return;
        }
        List<String> bestResult = data.get(1);
        /*for(List<String> record : data) {
            if (record.size() < 3) {
                continue;
            }
            if (record.get(2))
        }*/
        LinkedList<String> list = new LinkedList<>(bestResult);
        list.add(city);
        try (FileWriter writer = new FileWriter(path, true)) {
            var toAppend = convertToCSV(list);
            writer.append(toAppend);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String convertToCSV(List<String> data) {
        return String.join(",", data);
    }
}
