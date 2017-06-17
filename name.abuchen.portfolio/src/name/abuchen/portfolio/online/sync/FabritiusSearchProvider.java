package name.abuchen.portfolio.online.sync;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.online.SecuritySearchProvider;

public class FabritiusSearchProvider implements SecuritySearchProvider
{
    private static final String SEARCH_URL = "http://testapi.fabritius.org/securities/search/{0}"; //$NON-NLS-1$

    @Override
    public String getName()
    {
        return "Fabritius Search Provider";
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        return runSearch(query).stream().map(OnlineItem::toResultItem).collect(Collectors.toList());
    }

    public List<OnlineItem> runSearch(String query) throws IOException
    {
        String searchUrl = MessageFormat.format(SEARCH_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

        List<OnlineItem> answer = new ArrayList<>();

        try (Scanner scanner = new Scanner(new URL(searchUrl).openStream(), StandardCharsets.UTF_8.name()))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONArray response = (JSONArray) JSONValue.parse(html);
            if (response != null)
            {
                for (int ii = 0; ii < response.size(); ii++)
                    answer.add(OnlineItem.from((JSONObject) response.get(ii)));
            }
        }

        return answer;
    }
}
