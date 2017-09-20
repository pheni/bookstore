package cu.pdi.bookstore.domain.inventory.department.specs;

import cu.pdi.bookstore.domain.inventory.department.Department;
import cu.pdi.bookstore.domain.inventory.department.entry.InventoryEntry;
import cu.pdi.bookstore.domain.inventory.supply.TitleSupply;
import cu.pdi.bookstore.domain.kernel.specification.Specification;

import java.util.List;

/**
 * Created by taiyou
 * on 9/1/17.
 */
public class StockAvailabilitySpecification implements Specification<Department> {

    private TitleSupply titleSupply;

    private StockAvailabilitySpecification(TitleSupply titleSupply) {
        this.titleSupply = titleSupply;
    }

    public static StockAvailabilitySpecification of(TitleSupply titleSupply) {
        return new StockAvailabilitySpecification(titleSupply);
    }

    @Override
    public boolean isSatisfiedBy(Department department) {
        if (ExternalDepartmentSpecification.instance().isSatisfiedBy(department)) {
            return true;
        } else {
            List<InventoryEntry> inventoryEntries = department.listExistentEntriesForTitles(titleSupply.titlesISBN());

            return inventoryEntries.stream()
                    .anyMatch((InventoryEntry inventoryEntry) ->
                            inventoryEntry.getCurrentStock().isGreaterEqualThan(titleSupply.getStockForTitle(inventoryEntry.getTitle())));
        }
    }
}
