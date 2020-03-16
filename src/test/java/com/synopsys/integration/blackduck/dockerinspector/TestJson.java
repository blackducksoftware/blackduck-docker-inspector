package com.synopsys.integration.blackduck.dockerinspector;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestJson {
    public void test() throws IOException, JSONException {
        File jsonFile = new File("/Users/ekerwin/source/blackduck-docker-inspector/src/test/resources/bdio/blackducksoftware_hub-webapp_4.0.0_APK_bdio.jsonld");
        String json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
        final JSONArray expected = (JSONArray) JSONParser.parseJSON(json);

        File json2File = new File("/Users/ekerwin/source/blackduck-docker-inspector/src/test/resources/bdio/blackducksoftware_hub-webapp_4.0.0_APK_bdio_actual.jsonld");
        String json2 = FileUtils.readFileToString(json2File, StandardCharsets.UTF_8);
        final JSONArray actual = (JSONArray) JSONParser.parseJSON(json2);
        JSONAssert.assertEquals(expected, actual, false);
    }
}
