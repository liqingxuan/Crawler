package com.qingxuan.crawler;


import com.qingxuan.ad.Ad;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by qingxuan on 6/30/17.
 */
public class AmazonCrawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36";
    private final String authUser = "bittiger";
    private final String authPassword = "cs504";
    private final String port = "60099";
    private static final String AMAZON_QUERY_URL = "https://www.amazon.com/s/ref=nb_sb_noss?field-keywords=";

    private int roundRobinCnt = 0;
    private final int totalIPNum = 30;
    private String[] IPAddr = new String[totalIPNum];

    private long adID = 0;
    private double relevanceScore = 0.0;
    private double pClick = 0.0;
    private double rankScore = 0.0;
    private double qualityScore = 0.0;
    private double costPerClick = 0.0;
    private int position = 0;
    private String description = null;



    HashSet<String> existProd = new HashSet<>();
    StopWords stopWords = new StopWords();

    //constructor
    public AmazonCrawler(String proxyFile){

        // initialize proxy.
        try(BufferedReader br = new BufferedReader(new FileReader(proxyFile))){

            String line;
            int IPCnt = 0;

            while((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;

                //Read each query.
                //System.out.println(line);
                String[] fields = line.split(",");
                String IP = fields[0].trim();
                IPAddr[IPCnt++] = IP;
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void initProxy(String IP) {
        //socks5 set up
        //System.setProperty("socksProxyHost", "199.101.97.161"); // set socks proxy server
        //System.setProperty("socksProxyPort", "61336"); // set socks proxy port

        //http setup
        System.setProperty("http.proxyHost", IP); // set proxy server
        System.setProperty("http.proxyPort", port); // set proxy port

        Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                authUser, authPassword.toCharArray());
                    }
                }
        );
    }

    public void testProxy() {
        String test_url = "http://www.toolsvoid.com/what-is-my-ip-address";
        try {
            HashMap<String,String> headers = new HashMap<String,String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "en-US,en;q=0.8");
            Document doc = Jsoup.connect(test_url).headers(headers).userAgent(USER_AGENT).timeout(10000).get();
            String iP = doc.select("body > section.articles-section > div > div > div > div.col-md-8.display-flex > div > div.table-responsive > table > tbody > tr:nth-child(1) > td:nth-child(2) > strong").first().text(); //get used IP.
            System.out.println("IP-Address: " + iP);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<Ad> getAmazonProds(String query, double bidPrice, int campaignId, int queryGroupID, int pageNum) {
        String queryURL = query.replaceAll(" ", "+");
        System.out.println(query);
        String url = AMAZON_QUERY_URL + queryURL + "&page=" + pageNum;
        //System.out.println("url: " + url);

        List<Ad> ads = new ArrayList<>();

        try {
            //HashMap<String, String> headers = new HashMap<>();
            //headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            //headers.put("Accept-Encoding", "gzip, deflate, br");
            //headers.put("Accept-Language", "en-US,en;q=0.8");
            //Document doc = Jsoup.connect(url).maxBodySize(0).headers(headers).userAgent(USER_AGENT).timeout(10000).get();
            Document doc = Jsoup.connect(url).maxBodySize(0).userAgent(USER_AGENT).timeout(10000).get();
            //System.out.println(doc.text());
            Integer docSize = doc.text().length();
            //System.out.println("page size: " + docSize);


            // all products in the list.
            Elements prods = doc.getElementsByClass("s-result-item celwidget ");

            System.out.println("number of prod: " + prods.size());
            if(prods.size()==0) return null;
            int startInd =  Integer.valueOf(prods.get(0).id().substring(7));

            // get categoryStr
            Element category = doc.select("#leftNavContainer > ul:nth-child(2) > div > li:nth-child(1) > span > a > h4").first();
            String categoryStr = category.text();
            //System.out.println("prod category: " + categoryStr);



            for(Integer i = startInd;i < prods.size()+startInd; i++){
                // initialize a new Ad prod
                Ad ad = new Ad();


                //get current product through prod id.
                String id = "result_" + i.toString();
                Element prodsById = doc.getElementById(id);

                //if current result_i not exit.
                if(prodsById==null){
                    //System.out.println(id + "not exist");

                    continue;
                }

                //Use asin as HashSet key value for dedupe.
                String asin = prodsById.attr("data-asin");
                //System.out.println("prod asin: " + asin);
                if(existProd.contains(asin)){
                    //ads.add(null);
                    continue; //dedupe
                }

                // get Title
                Elements titleEleList = prodsById.getElementsByAttribute("title");
                if(titleEleList == null || titleEleList.size()==0) continue;
                String title = null;
                for(Element titleEle : titleEleList) {
                    //System.out.println("prod title: " + titleEle.attr("title"));
                    title = titleEle.attr("title");
                }

                // convert to keywords
                List<String> keywords = getKeyWordsFromTitle(title);
                //System.out.println("KeyWords: " + keywords.toString());

                // get prodURL
                Elements prodURLs = prodsById.getElementsByClass("a-link-normal s-access-detail-page  s-color-twister-title-link a-text-normal");
                String prodURL = prodURLs.attr("href");
                if(prodURL == null || prodURL == "") continue; //not a product
                if(prodURL.startsWith("/gp/")){
                    int startURL = prodURL.indexOf("https");
                    int endURL = prodURL.lastIndexOf("psc");
                    prodURL = prodURL.substring(startURL, endURL);
                    prodURL = java.net.URLDecoder.decode(prodURL, "UTF-8");

                    //URLDecoder decoder = new URLDecoder();

                }
                //System.out.println("prod URL: " + prodURL);


                //get Price
                Elements prodWholePrices = prodsById.getElementsByClass("a-color-base sx-zero-spacing");
                if(prodWholePrices == null || prodWholePrices.size()==0) continue;
                double price = 0.0;
                for(Element wp : prodWholePrices) {

                    String priceRange = wp.attr("aria-label").replace("$", "").replace(",", "").replace(" ","");
                    if(priceRange.contains("-")){
                        String[] prices = priceRange.split("-");
                        priceRange = prices[0];
                    }
                    price = Double.valueOf(priceRange);
                }
                //System.out.println("prod price: " + price);


                //get prod image url
                Elements prodImages = prodsById.getElementsByTag("img");
                String prodImage = prodImages.attr("src");
                //System.out.println("prod image: " + prodImage);

                // get product's brand
                Elements prodBrandsEle = prodsById.getElementsByClass("a-row a-spacing-none");
                List<String> prodBrands = prodBrandsEle.eachText();
                String brand = null;
                for(String prodBrand: prodBrands){
                    if(prodBrand.startsWith("by ")){
                        brand = prodBrand.substring(3);
                        //System.out.println("prod brand: "+ brand);
                    }
                }


                ad.adId = adID++;
                ad.campaignId = campaignId;
                ad.relevanceScore = relevanceScore;
                ad.pClick = pClick;
                ad.bidPrice = bidPrice;
                ad.rankScore = rankScore;
                ad.qualityScore = qualityScore;
                ad.costPerClick = costPerClick;
                ad.position = position;
                ad.description = description;

                ad.query = query;
                ad.query_group_id = queryGroupID;

                ad.category = categoryStr;
                ad.title = title;
                ad.price = price;
                ad.thumbnail = prodImage;
                ad.detail_url = prodURL;
                ad.keyWords = keywords;
                ad.brand = brand;

                ads.add(ad);

                // save asin for dedupe.
                existProd.add(asin);

            }
            //#leftNavContainer > ul:nth-child(2) > div > li:nth-child(1) > span > a > h4





        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ads;

    }


    public void parseAmazonProdPage(String url) {
        try {
            HashMap<String,String> headers = new HashMap<String,String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "en-US,en;q=0.8");
            Document doc = Jsoup.connect(url).headers(headers).userAgent(USER_AGENT).timeout(10000).get();
            Element titleEle = doc.getElementById("productTitle");
            String title = titleEle.text();
            System.out.println("title: " + title);

            Element priceEle =doc.getElementById("priceblock_ourprice");
            String price = priceEle.text();
            System.out.println("price: " + price);

            //review
            //#cm-cr-dp-review-list
            Elements reviews = doc.getElementsByClass("a-expander-content a-expander-partial-collapse-content");
            System.out.println("number of reviews: " + reviews.size());
            for (Element review : reviews) {
                System.out.println("review content: " + review.text());
            }

            //#customer_review-R188VC0CBW8NLR > div:nth-child(4) > span > div > div.a-expander-content.a-expander-partial-collapse-content



        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public List<Ad> GetAdsInfoByQuery(String query, double bidPrice, int campaignId, int queryGroupID, int pageNum){

        AssignIPByRoundRobin();
        return getAmazonProds(query, bidPrice, campaignId, queryGroupID, pageNum);
    }

    private void AssignIPByRoundRobin(){
        String curIP = IPAddr[roundRobinCnt % totalIPNum];

        initProxy(curIP);
        roundRobinCnt++;
        return;
    }



    private List<String> getKeyWordsFromTitle(String prodTitle){
        List<String> keywords = new ArrayList<>();

        prodTitle = prodTitle.replaceAll(",", "").replaceAll("[()]", "").replaceAll("[+]", "").replaceAll("!", "").replaceAll("'", " ").replaceAll("&", "").replaceAll(" -", "").replaceAll("/", " ").replaceAll(":", " ").replaceAll("\"","");

        //System.out.println(prodTitle);

        String[] keyWordsArray = prodTitle.split(" ");

        for(String str: keyWordsArray){
            if(str==null || str.equals("") ||str.equals(" ")) continue;
            if(!stopWords.containsWord(str.trim())){
                keywords.add(str.trim().toLowerCase());
            }
        }

        return keywords;
    }

}
