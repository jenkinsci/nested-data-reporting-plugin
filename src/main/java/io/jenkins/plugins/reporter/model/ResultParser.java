package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

public class ResultParser {
    
    public Optional<Result> parseResult(File file) throws IOException {
        
        String extension =  FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT);

        String json;

        switch (extension) {
            case "yaml":
            case "yml": {
                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                Object obj = yamlReader.readValue(file, Object.class);
                ObjectMapper jsonWriter = new ObjectMapper();
                json = jsonWriter.writeValueAsString(obj);
                break;
            }
            case "json":
                json = FileUtils.readFileToString(file, "UTF-8");
                break;
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
