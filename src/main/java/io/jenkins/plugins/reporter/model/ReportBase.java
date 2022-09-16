package io.jenkins.plugins.reporter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ReportBase {

    /**
     * Find the item with id in given list of items.
     * 
     * @param id
     *          the id to find.
     * @param items
     *          the items to search in.
     * @return
     *          the {@link Item} as {@link Optional}.
     */
    public Optional<Item> findItem(String id, List<Item> items) {
        if (items != null) {
            for (Item i: items) {
                if (i.getId().equals(id)) {
                    return Optional.of(i);
                } else {
                    Optional<Item> sub = findItem(id, i.getItems());
                    if (sub.isPresent()) {
                        return sub;
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Flatten all items of report.
     * 
     * @param items
     *          the items to flatten.
     *          
     * @return
     *          list with all items.
     */
    List<Item> flattItems(List<Item> items)
    {
        List<Item> flatten = new ArrayList<>();

        for (Item i: items) {
            if (i.hasItems()) {
                flatten.addAll(flattItems(i.getItems()));
            }

            flatten.add(i);
        }

        return flatten;
    }
    
}
