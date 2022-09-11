package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.hm.hafner.echarts.JacksonFacade;
import io.jenkins.cli.shaded.org.apache.commons.io.FileUtils;
import io.jenkins.cli.shaded.org.apache.commons.io.FilenameUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;

public class ResultParser {
    
    public Optional<Result> parseResult(File file) throws IOException {
        
        String extension =  FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT);
        ObjectMapper jsonWriter = new ObjectMapper();
        String json;

        switch (extension) {
            case "yaml":
            case "yml": {
                Result result = new ObjectMapper(new YAMLFactory()).readerFor(Result.class).readValue(file);
                json = jsonWriter.writeValueAsString(result);
                break;
            }
            case "xml": {
                Result result = new ObjectMapper(new XmlFactory()).readerFor(Result.class).readValue(file);
                json = jsonWriter.writeValueAsString(result);
                break;
            }
            case "json":
                json = FileUtils.readFileToString(file, "UTF-8");
                break;
            case "csv":
                CsvMapper mapper = new CsvMapper();
                CsvSchema csvSchema = mapper.typedSchemaFor(Map.class).withHeader();

                MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                        .with(csvSchema.withColumnSeparator(','))
                        .readValues(file);

                Result result = new Result();
                result.setId(String.valueOf(file.getName().hashCode()));
                result.setName(FilenameUtils.removeExtension(file.getName()));
                List<Item> items = new ArrayList<>();

                while (it.hasNextValue()) {
                    Map<String, String> row = it.nextValue();
                    Item item = new Item();
                    item.setId(row.get("id"));
                    item.setName(row.get("name"));
                    LinkedHashMap<String, Integer> res = row.keySet()
                            .stream().filter(key -> !key.equals("id") && !key.equals("name"))
                            .collect(
                                    LinkedHashMap::new,
                                    (map, key) -> map.put(key, Integer.parseInt(row.get(key))),
                                    Map::putAll);
                    item.setResult(res);
                    items.add(item);
                }

                result.setItems(items);
                json = jsonWriter.writeValueAsString(result);
            default:
                return Optional.empty();
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/report.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            SchemaLoader schemaLoader = SchemaLoader.builder()
                    .schemaClient(SchemaClient.classPathAwareClient())
                    .schemaJson(rawSchema)
                    .resolutionScope("classpath:/")
                    .build();
            Schema schema = schemaLoader.load().build();
            schema.validate(new JSONObject(json));

            JacksonFacade jackson = new JacksonFacade();
            Result result =  jackson.fromJson(json, Result.class);
            
            return Optional.of(result);
        
        } catch (ValidationException e) {
            return Optional.empty();
        }
    }
}
