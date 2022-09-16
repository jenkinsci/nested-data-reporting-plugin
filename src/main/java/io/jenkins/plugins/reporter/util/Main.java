package io.jenkins.plugins.reporter.util;

import java.util.regex.Pattern;

public class Main {

        public static void main(String[] args) {
            String SLASH = "/";
            String match = "/5/report-JSON+Report";
            System.out.println(Pattern.compile(match).pattern());
            String viewUrl = "http://localhost:8080/jenkins/job/new-api-v2-2/5/report-JSON+Report/";
            int selectedBuildNumber = 3;
            System.out.println(escapeRE(match));
            String resultId = "report-JSON+Report";
            String url = viewUrl.replaceFirst(
                    match, SLASH + selectedBuildNumber + SLASH + resultId);

            System.out.println(url);

        }

    public static String escapeRE(String str) {
        Pattern escaper = Pattern.compile("([^a-zA-z0-9])");
        return escaper.matcher(str).replaceAll("\\\\$1");
    }
}
