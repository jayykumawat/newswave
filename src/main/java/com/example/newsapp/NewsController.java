package com.example.newsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller //@Controller marks this class as a Spring MVC Controller
public class NewsController {

    @Value("${newsapi.key}")
private String NEWS_API_KEY;

@Value("${newsdata.key}")
private String NEWSDATA_KEY;
    // ── NewsAPI fetch ──────────────────────────────────────────
    private List<Map<String, String>> fetchNewsAPI(String urlString) {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            reader.close();
            /*
             * URL url = new URL(urlString)
             * → Creates a URL object pointing to NewsAPI
             * 
             * conn = url.openConnection()
             * → Opens a connection to that URL (like dialing a phone number)
             * 
             * conn.setRequestMethod("GET")
             * → Tells the API: "I want to READ data" (not send/delete)
             * 
             * conn.setRequestProperty("User-Agent", "Mozilla/5.0")
             * → Pretends to be a browser so the API doesn't block the request
             * 
             * InputStreamReader → reads raw bytes from the internet
             * BufferedReader → makes it efficient (reads line by line)
             * StringBuilder → collects all lines into one big JSON string
             */
            ObjectMapper mapper = new ObjectMapper();
            JsonNode articles = mapper.readTree(sb.toString()).get("articles");
            /*
            sb.toString()        → our raw JSON string
mapper.readTree()    → converts it into a tree structure Java can navigate
root.get("articles") → goes inside JSON and gets the "articles" array
            */
            int n = 1;
            for (JsonNode a : articles) {
                String title = val(a, "title");
                if (title.isEmpty() || title.equals("[Removed]"))
                    continue;
                Map<String, String> m = new HashMap<>();
                m.put("title", title);
                m.put("description", val(a, "description"));
                m.put("url", val(a, "url"));
                m.put("image", val(a, "urlToImage"));
                m.put("source", a.has("source") ? a.get("source").path("name").asText("Unknown") : "Unknown");
                m.put("number", String.valueOf(n++));
                // NEW CODE FOR DATE, TIME & COUNTRY (NewsAPI)
                // NEW SMART CODE FOR DATE, TIME & COUNTRY (NewsAPI)
                String pubAt = val(a, "publishedAt");
                try {

                    ZonedDateTime zdt = ZonedDateTime.parse(pubAt);

                    m.put("date", zdt.format(DateTimeFormatter.ofPattern("d MMMM yyyy")));

                    m.put("time", zdt.format(DateTimeFormatter.ofPattern("hh:mm a")));
                } catch (Exception e) {
                    m.put("date", "Recent");
                    m.put("time", "");
                }
                m.put("country", "GLOBAL");
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── NewsData.io fetch ──────────────────────────────────────
    private List<Map<String, String>> fetchNewsData(String urlString) {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            reader.close();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode results = mapper.readTree(sb.toString()).get("results");
            if (results == null || results.isNull())
                return list;
            int n = 1;
            for (JsonNode a : results) {
                String title = a.path("title").asText("");
                if (title.isEmpty())
                    continue;
                Map<String, String> m = new HashMap<>();
                m.put("title", title);
                m.put("description", a.path("description").asText(""));
                m.put("url", a.path("link").asText("#"));
                m.put("image", a.path("image_url").asText(""));
                m.put("source", a.path("source_name").asText(a.path("source_id").asText("Unknown")));
                m.put("number", String.valueOf(n++));

                String pubDate = a.path("pubDate").asText("");
                try {

                    LocalDateTime ldt = LocalDateTime.parse(pubDate,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    m.put("date", ldt.format(DateTimeFormatter.ofPattern("d MMMM yyyy")));

                    m.put("time", ldt.format(DateTimeFormatter.ofPattern("hh:mm a")));
                } catch (Exception e) {
                    // Agar parse fail ho jaye toh default
                    m.put("date", pubDate.length() >= 10 ? pubDate.substring(0, 10) : "Recent");
                    m.put("time", "");
                }
                // Country logic
                String country = a.has("country") && a.get("country").isArray() && a.get("country").size() > 0
                        ? a.get("country").get(0).asText().toUpperCase()
                        : "INDIA";
                m.put("country", country);
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String val(JsonNode n, String key) {
        return n.has(key) && !n.get(key).isNull() ? n.get(key).asText() : "";
    }

    // ── FALLBACK HELPER METHOD ─────────────────────────────────
    private List<Map<String, String>> fetchWithFallback(String newsDataUrl, String newsApiUrl) {
        // Pehle NewsData se try karo
        List<Map<String, String>> list = fetchNewsData(newsDataUrl);

        if (list == null || list.isEmpty()) {
            System.out.println("NewsData failed or empty. Falling back to NewsAPI...");
            list = fetchNewsAPI(newsApiUrl);
        }
        return list;
    }

    // ── Main Controller ────────────────────────────────────────
    @GetMapping("/")
    //@GetMapping("/") maps HTTP GET requests for the root URL to the getNews() method. So whenever the homepage is opened, this method executes automatically.
    public String getNews(
            @RequestParam(defaultValue = "national") String category,
            @RequestParam(defaultValue = "") String q,
            Model model) {
        List<Map<String, String>> articles = new ArrayList<>();
        List<Map<String, String>> trending = new ArrayList<>();
        List<Map<String, String>> entertainment = new ArrayList<>();
        List<Map<String, String>> sportsPreview = new ArrayList<>();
        List<Map<String, String>> businessPreview = new ArrayList<>();
        boolean showTrending = false;
        boolean isHomePage = false;

        if (!q.isEmpty()) {
            articles = fetchNewsAPI(
                    "https://newsapi.org/v2/everything?q=" + q
                            + "&language=en&pageSize=30&sortBy=publishedAt&apiKey=" + NEWS_API_KEY);

        } else {
            switch (category) {
                case "national":
                    isHomePage = true;

                    // Call 1 & 2 — first try NewsData
                    List<Map<String, String>> national1 = fetchNewsData(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en"
                                    + "&category=top,politics,sports,technology"
                                    + "&removeduplicate=1&size=10");
                    List<Map<String, String>> national2 = fetchNewsData(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en"
                                    + "&category=breaking,business,entertainment"
                                    + "&removeduplicate=1&size=10");

                    articles = new ArrayList<>();
                    articles.addAll(national1);
                    articles.addAll(national2);

                    // FALLBACK FOR NATIONAL: use newsapi , if newsdata is empty
                    if (articles.isEmpty()) {
                        System.out.println("NewsData limits reached for National. Using NewsAPI...");
                        articles = fetchNewsAPI(
                                "https://newsapi.org/v2/everything?q=India&language=en&sortBy=publishedAt&pageSize=30&apiKey="
                                        + NEWS_API_KEY);
                    }
                    trending = fetchNewsData(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en"
                                    + "&category=breaking,top,politics"
                                    + "&removeduplicate=1&size=10");
                    showTrending = !trending.isEmpty();

                    // Trending — first 10 from list
                    // trending = new ArrayList<>(articles.subList(0, Math.min(10,
                    // articles.size())));
                    // showTrending = !trending.isEmpty();

                    // Bottom sections with Fallback
                    entertainment = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en&category=entertainment&size=5",
                            "https://newsapi.org/v2/top-headlines?country=in&category=entertainment&pageSize=5&apiKey="
                                    + NEWS_API_KEY);
                    sportsPreview = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en&category=sports&size=5",
                            "https://newsapi.org/v2/top-headlines?country=in&category=sports&pageSize=5&apiKey="
                                    + NEWS_API_KEY);
                    break;

                case "politics":
                    articles = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en&category=politics&size=10",
                            "https://newsapi.org/v2/everything?q=politics+india+OR+election+OR+government&language=en&sortBy=publishedAt&pageSize=30&apiKey="
                                    + NEWS_API_KEY);
                    trending = new ArrayList<>(articles.subList(0, Math.min(10, articles.size())));
                    showTrending = true;
                    break;

                case "international":
                    // Ye sirf NewsAPI pe rehta hai
                    articles = fetchNewsAPI(
                            "https://newsapi.org/v2/top-headlines?language=en&pageSize=30&apiKey=" + NEWS_API_KEY);
                    break;

                case "technology":
                    articles = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&language=en&category=technology&size=10",

                            "https://newsapi.org/v2/top-headlines?category=technology&language=en&apiKey="
                                    + NEWS_API_KEY);
                    break;

                case "sports":

                    articles = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&category=sports&size=10",
                            "https://newsapi.org/v2/top-headlines?country=in&category=sports&apiKey=" + NEWS_API_KEY);

                    if (articles.isEmpty()) {
                        articles = fetchNewsAPI(
                                "https://newsapi.org/v2/top-headlines?category=sports&language=en&apiKey="
                                        + NEWS_API_KEY);
                    }
                    break;

                case "business":
                    articles = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&category=business&size=10",
                            "https://newsapi.org/v2/top-headlines?country=in&category=business&apiKey=" + NEWS_API_KEY);

                    if (articles.isEmpty()) {
                        articles = fetchNewsAPI(
                                "https://newsapi.org/v2/top-headlines?category=business&language=en&apiKey="
                                        + NEWS_API_KEY);
                    }
                    break;

                case "entertainment":
                    articles = fetchWithFallback(
                            "https://newsdata.io/api/1/latest?apikey=" + NEWSDATA_KEY
                                    + "&country=in&category=entertainment&language=en&size=10",
                            "https://newsapi.org/v2/top-headlines?country=in&category=entertainment&language=en&apiKey="
                                    + NEWS_API_KEY);

                    if (articles.isEmpty()) {
                        articles = fetchNewsAPI(
                                "https://newsapi.org/v2/top-headlines?category=entertainment&language=en&apiKey="
                                        + NEWS_API_KEY);
                    }
                    break;

                default:
                    articles = fetchNewsAPI(
                            "https://newsapi.org/v2/everything?q=india&language=en&pageSize=30&sortBy=publishedAt&apiKey="
                                    + NEWS_API_KEY);
                    break;

            }
        }

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String dayName = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE"));

        model.addAttribute("articles", articles);
        model.addAttribute("trending", trending);
        model.addAttribute("entertainment", entertainment);
        model.addAttribute("sportsPreview", sportsPreview);
        model.addAttribute("showTrending", showTrending);
        model.addAttribute("isHomePage", isHomePage);
        model.addAttribute("category", category);
        model.addAttribute("searchQuery", q);
        model.addAttribute("todayDate", todayDate);
        model.addAttribute("dayName", dayName);
        return "index";
        /*
        Model is a Spring MVC interface that acts as a bridge between the Controller and the View. added news list to the model with the key articles. Thymeleaf then accesses this data using ${articles} in the HTML template.
        */
    }
}