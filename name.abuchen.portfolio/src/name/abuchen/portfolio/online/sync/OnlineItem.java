package name.abuchen.portfolio.online.sync;

import org.json.simple.JSONObject;

import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

public class OnlineItem
{
    private String id;
    private String name;
    private String isin;
    private String wkn;
    private String ticker;

    public static OnlineItem from(JSONObject json)
    {
        OnlineItem vehicle = new OnlineItem();
        vehicle.id = (String) json.get("uuid"); //$NON-NLS-1$
        vehicle.name = (String) json.get("name"); //$NON-NLS-1$
        vehicle.isin = (String) json.get("isin"); //$NON-NLS-1$
        vehicle.wkn = (String) json.get("wkn"); //$NON-NLS-1$
        vehicle.ticker = (String) json.get("ticker"); //$NON-NLS-1$
        return vehicle;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getIsin()
    {
        return isin;
    }

    public String getWkn()
    {
        return wkn;
    }

    public String getTicker()
    {
        return ticker;
    }

    public ResultItem toResultItem()
    {
        ResultItem resultItem = new ResultItem();

        resultItem.setName(name);
        resultItem.setIsin(isin);
        resultItem.setSymbol(ticker);
        resultItem.setType("Wertpapier");

        return resultItem;
    }
}
