package com.example.CoronaVirusTracker.service;

import com.example.CoronaVirusTracker.model.LocationStats;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CoronaVirusService {

    private static final String URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";

    private List<LocationStats> allStats = new ArrayList<>();

    public List<LocationStats> getAllStats() {
        return allStats;
    }

    @PostConstruct
    @Scheduled(cron = "0 0 * * * *") //fetch data every hour
    public void fetchData() throws IOException, InterruptedException {
        List<LocationStats> newStats = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            StringReader reader = new StringReader(httpResponse.body());
            CSVParser csvParser = new CSVParser(reader,CSVFormat.RFC4180);
            for (CSVRecord record : csvParser) {
                LocationStats locationStat = new LocationStats();
                locationStat.setState(record.get("Province/State"));
                locationStat.setCountry(record.get("Country/Region"));
                try {
                    int latestCases = Integer.parseInt(record.get(record.size() - 1));
                    int prevDayCases = Integer.parseInt(record.get(record.size() - 2));
                    locationStat.setLatestTotalCases(latestCases);
                    locationStat.setDiffFromPrevDay(latestCases - prevDayCases);
                } catch (NumberFormatException e) {
                    log.error("Error while parsing data..."+e.getMessage());
                    continue;
                }
                newStats.add(locationStat);
            }
            this.allStats = newStats;
        } catch (Exception e) {
            log.error("Error while fetching data from CSV..."+e.getMessage());
        }
    }
}
