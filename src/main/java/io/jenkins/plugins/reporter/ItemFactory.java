package io.jenkins.plugins.reporter;

import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Item;

import java.util.Map;

public class ItemFactory {
    
    public ItemFactory() {
        
    }
    
    public Object createNewItemView(final String link, final Run<?, ?> owner, final ReportViewModel parent, final Item item, Map<String, String> colors) {
        String url = parent.getUrl() + "/" + link;
        return new ReportViewModel(owner, url, item, String.format("Module: %s", item.getId()), colors);
    } 
}
