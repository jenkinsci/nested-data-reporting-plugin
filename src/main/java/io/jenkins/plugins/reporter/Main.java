package io.jenkins.plugins.reporter;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws ProcessingException, URISyntaxException, IOException {
        String json = "{\"items\":[{\"id\":\"Aktie\",\"name\":\"Aktie\",\"items\":[{\"id\":\"Aktien1\",\"name\":\"Aktien1\",\"result\":{\"incorrect\":3541,\"manually\":58,\"accurate\":6399}},{\"id\":\"Aktie2\",\"name\":\"Aktie2\",\"result\":{\"incorrect\":4488,\"manually\":55,\"accurate\":5456}},{\"id\":\"Aktie3\",\"name\":\"Aktie3\",\"result\":{\"incorrect\":2973,\"manually\":72,\"accurate\":6954}}]},{\"id\":\"Not_Found\",\"name\":\"Not_Found\",\"items\":[{\"id\":\"Not_Found\",\"name\":\"Not_Found\",\"result\":{\"incorrect\":8701,\"manually\":0,\"accurate\":1298}}]},{\"id\":\"Renten\",\"name\":\"Renten\",\"items\":[{\"id\":\"Rente1\",\"name\":\"Rente1\",\"result\":{\"incorrect\":5762,\"manually\":49,\"accurate\":4187}},{\"id\":\"Rente2\",\"name\":\"Rente2\",\"result\":{\"incorrect\":2271,\"manually\":79,\"accurate\":7648}}]},{\"id\":\"Derivat\",\"name\":\"Derivat\",\"result\":{\"incorrect\":2271,\"manually\":79,\"accurate\":7648}}],\"colors\":{\"incorrect\":\"#EF9A9A\",\"manually\":\"#FFF59D\",\"accurate\":\"#A5D6A7\"}}";
        ProcessingReport validate = JsonSchemaFactory.byDefault().getJsonSchema("resource:/report.json").validate(JsonLoader.fromString(json));
        System.out.println(validate.isSuccess());
    }
}
