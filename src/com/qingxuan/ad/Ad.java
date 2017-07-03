package com.qingxuan.ad;

import java.io.Serializable;
import java.util.List;

/**
 * Created by qingxuan on 6/29/17.
 */
public class Ad implements Serializable {

    private static final long serialVersionUID = 1L;
    public long adId;
    public int campaignId; //provided
    public List<String> keyWords; //get
    public double relevanceScore; //0.0
    public double pClick; //0.0
    public double bidPrice; //provided
    public double rankScore; //0.0
    public double qualityScore; //0.0
    public double costPerClick; //0.0
    public int position; //0
    public String title; //get
    public double price; //get
    public String thumbnail; //get prod image url.
    public String description; //null
    public String brand; //get
    public String detail_url; //get
    public String query; //provided
    public int query_group_id; //provided
    public String category; //get



}
