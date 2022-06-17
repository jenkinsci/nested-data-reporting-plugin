package io.jenkins.plugins.reporter;

import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableModel;

import java.util.ArrayList;
import java.util.List;

public class ReportTableModel extends TableModel {
    
    String id;
    
    public ReportTableModel(String id) {
        super();
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();
        
        columns.add(new TableColumn("ID", "id"));
        columns.add(new TableColumn("Items", "items"));
        
        return columns;
    }

    @Override
    public List<Object> getRows() {
        List<Object> items = new ArrayList<>();
        items.add(new TableRow("Blah"));
        items.add(new TableRow("Keks"));
        return items;
    }
    
    public static class TableRow {
        
        private String id;
        
        TableRow(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id; 
        }
        
        public String getItems() {
            return id + " items";
        }
    }
}
