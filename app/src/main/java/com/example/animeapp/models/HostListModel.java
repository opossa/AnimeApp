package com.example.animeapp.models;

import java.util.List;
import java.util.Map;

public class HostListModel {
    private Map<String, List<String>> hostList;

    public Map<String, List<String>> getHostList() {
        return hostList;
    }

    public void setHostList(Map<String, List<String>> hostList) {
        this.hostList = hostList;
    }
}