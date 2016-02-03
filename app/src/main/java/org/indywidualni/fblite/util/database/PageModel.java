package org.indywidualni.fblite.util.database;

public class PageModel {

    private String url;
    private String html;

    public PageModel(String url, String html) {
        this.url = url;
        this.html = html;
    }

    public String getUrl() {
        return url;
    }

    public String getHtml() {
        return html;
    }

}