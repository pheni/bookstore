package cu.pdi.bookstore.domain;

import cu.pdi.bookstore.application.config.AppConfig;
import cu.pdi.bookstore.domain.accounting.document.*;
import cu.pdi.bookstore.domain.accounting.reception.InvoiceNumber;
import cu.pdi.bookstore.domain.accounting.reception.ReceptionReport;
import cu.pdi.bookstore.domain.accounting.reception.SourceWarehouse;
import cu.pdi.bookstore.domain.accounting.sales.SaleVoucherNumber;
import cu.pdi.bookstore.domain.inventory.department.sales.PointOfSale;
import cu.pdi.bookstore.domain.accounting.sales.SalesSummary;
import cu.pdi.bookstore.domain.accounting.transfer.DeliveryVoucher;
import cu.pdi.bookstore.domain.builders.TitleInfoBuilder;
import cu.pdi.bookstore.domain.builders.TitleSetFactory;
import cu.pdi.bookstore.domain.inventory.department.Bookstore;
import cu.pdi.bookstore.domain.inventory.department.Department;
import cu.pdi.bookstore.domain.inventory.department.DepartmentFactory;
import cu.pdi.bookstore.domain.inventory.department.DepartmentRepository;
import cu.pdi.bookstore.domain.kernel.title.TitleSale;
import cu.pdi.bookstore.domain.kernel.title.TitleSupply;
import cu.pdi.bookstore.domain.kernel.DepartmentCode;
import cu.pdi.bookstore.domain.kernel.ISBN;
import cu.pdi.bookstore.domain.kernel.Plan;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static cu.pdi.bookstore.domain.assertions.InventoryAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@ActiveProfiles("dev")
public class BookstoreTest {

    @Autowired
    private DepartmentFactory departmentFactory;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private Bookstore bookstore;

    @Autowired
    private AccountingDocumentService documentService;

    @Autowired
    private PointOfSale pointOfSale;

    /**
     * This happen when a new place is ready to receive books.
     * It should be created and added to the Department list, identified by its department code.
     */
    @Test
    public void shouldEnableANewDepartment() {
        //ToDo: Create Sales Room and Book Depot as default departments
        //GIVEN
        Department coffeeSaloon = departmentFactory.createDepartment(DepartmentCode.forCode("07"), "Coffee Saloon");
        //WHEN
        bookstore.enableDepartment(coffeeSaloon);
        //THEN
        assertThat(departmentRepository.findDepartmentByCode(coffeeSaloon.getCode())).isNotNull();
    }

    /**
     * This happen when Book Depot department is supplied by the main Warehouse,
     * the supply comes forReceptionReport an stock of books for each TitleInventoryInfo and Titles info.
     * As a result each title get its own Inventory Entry and Transfer Entry
     * in Book Depot department depending on if the TitleInventoryInfo is new or has a previous inventory entry.
     * Beside a Reception Report is created as evidence of the reception at the Bookstore
     */
    @Test
    public void shouldReceiveASupplyOfTitlesFromMainWarehouse() {
        //GIVEN
        Department bookDepot = departmentFactory.bookDepotDepartment();

        Department warehouse = DepartmentFactory.WAREHOUSE;

        TitleSupply titleSupply = TitleSetFactory.createTitleSupplyForTitles(
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("90238127823"))
                        .withDescription("The Hollow")
                        .build(),
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("937238292201"))
                        .withDescription("The Lighter")
                        .build()
        );
        int lastDocumentIndex, currentDocumentAmount;
        lastDocumentIndex = currentDocumentAmount = documentService.listDocuments().size();

        //WHEN
        bookDepot.receiveTitles(warehouse, titleSupply);

        //THEN
        assertThat(bookDepot).hasInventoryEntriesForAllTitles(titleSupply);

        //AND WHEN
        List<DeliveryVoucher> deliveryVouchers = documentService.listDocuments();
        DeliveryVoucher receptionReport = deliveryVouchers.get(lastDocumentIndex);

        //THEN
        Assertions.assertThat(deliveryVouchers).hasSize(currentDocumentAmount + 1);
        Assertions.assertThat(receptionReport.associatedTransferLogs()).hasSize(2);
        Assertions.assertThat(receptionReport).isInstanceOf(ReceptionReport.class);
        Assertions.assertThat(receptionReport.getAccountingDocumentType()).isEqualTo(AccountingDocumentType.RECEPTION_REPORT);

        //AND WHEN
        documentService.completeDocument(receptionReport,
                AccountingDocumentInfo.forReceptionReport(Consecutive.of("1"),
                        InvoiceNumber.of("21938123"),
                        Plan.withName("Regular"),
                        SourceWarehouse.withName("Central")));
        //THEN
        receptionReport = documentService.findDocumentWithConsecutive(receptionReport.getConsecutive());
        Assertions.assertThat(((ReceptionReport) receptionReport).getInvoiceNumber()).isEqualTo(InvoiceNumber.of("21938123"));
        Assertions.assertThat(((ReceptionReport) receptionReport).getPlan()).isEqualTo(Plan.withName("Regular"));
    }


    /**
     * This happen when Sales Room department is supplied by the Book Depot department or vice versa,
     * the supply comes with an stock of books for each TitleInventoryInfo and Titles info.
     * As a result each title get its own Transfer Entry in Sales Room department and an Inventory Entry
     * depending on if the TitleInventoryInfo is new or has a previous inventory entry.
     * Beside a Delivery Voucher is created as evidence of the transfer between departments
     */
    @Test
    public void shouldTransferTitlesFromOneDepartmentToAnother() {
        //GIVEN
        Department bookDepot = departmentFactory.bookDepotDepartment();

        Department salesRoom = departmentFactory.salesRoomDepartment();

        Department warehouse = DepartmentFactory.WAREHOUSE;

        TitleSupply titleSupply = TitleSetFactory.createTitleSupplyForTitles(
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("90238127823"))
                        .withDescription("The Hollow")
                        .build(),
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("937238292201"))
                        .withDescription("The Lighter")
                        .build()
        );

        bookDepot.receiveTitles(warehouse, titleSupply);

        int lastDocumentIndex, currentDocumentAmount;
        List<DeliveryVoucher> deliveryVouchers = documentService.listDocuments();
        lastDocumentIndex = currentDocumentAmount = deliveryVouchers.size();

        DeliveryVoucher receptionReport = deliveryVouchers.get(lastDocumentIndex - 1);
        documentService.completeDocument(receptionReport,
                AccountingDocumentInfo.forReceptionReport(Consecutive.of("2"),
                        InvoiceNumber.of("21938123"),
                        Plan.withName("Regular"),
                        SourceWarehouse.withName("Central")));
        //WHEN
        salesRoom.receiveTitles(bookDepot, titleSupply);

        //THEN
        assertThat(salesRoom).hasInventoryEntriesForAllTitles(titleSupply);

        //AND WHEN
        deliveryVouchers = documentService.listDocuments();
        DeliveryVoucher deliveryVoucher = deliveryVouchers.get(lastDocumentIndex);

        //THEN
        Assertions.assertThat(deliveryVouchers).hasSize(currentDocumentAmount + 1);
        Assertions.assertThat(deliveryVoucher.associatedTransferLogs()).hasSize(2);
        Assertions.assertThat(deliveryVoucher).isNotInstanceOfAny(ReceptionReport.class, SalesSummary.class);
        Assertions.assertThat(deliveryVoucher.getAccountingDocumentType()).isEqualTo(AccountingDocumentType.DELIVERY_VOUCHER);

        //AND WHEN
        documentService.completeDocument(deliveryVoucher,
                AccountingDocumentInfo.forDeliveryVoucher(Consecutive.of("3")));
        //THEN
        deliveryVoucher = documentService.findDocumentWithConsecutive(deliveryVoucher.getConsecutive());
        Assertions.assertThat(deliveryVoucher.getConsecutive()).isEqualTo(Consecutive.of("3"));

    }


    /**
     * This happen when in Sales Room department ends a day and summarize the Titles sales through
     * the Sales Vouchers created each time a sale is executed.Then a Sales Summary is created as
     * evidence of the sales of that day. A Sales Summary hold the number of the start and end
     * Sales Voucher including the total stock for each Plan available. For each sale a Transfer
     * is registered.
     */
    @Test
    public void shouldRegisterSoldTitlesFromSalesDepartment() {
        //GIVEN
        Department bookDepot = departmentFactory.bookDepotDepartment();

        Department salesRoom = departmentFactory.salesRoomDepartment();

        Department warehouse = DepartmentFactory.WAREHOUSE;

        TitleSupply titleSupply = TitleSetFactory.createTitleSupplyForTitles(
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("90238127823"))
                        .withDescription("The Hollow")
                        .build(),
                TitleInfoBuilder.createTitle().withISBN(ISBN.of("937238292201"))
                        .withDescription("The Lighter")
                        .build()
        );


        bookDepot.receiveTitles(warehouse, titleSupply);

        int lastDocumentIndex, currentDocumentAmount;
        List<DeliveryVoucher> deliveryVouchers = documentService.listDocuments();
        lastDocumentIndex = deliveryVouchers.size();

        DeliveryVoucher receptionReport = deliveryVouchers.get(lastDocumentIndex - 1);
        documentService.completeDocument(receptionReport,
                AccountingDocumentInfo.forReceptionReport(Consecutive.of("4"),
                        InvoiceNumber.of("21938123"),
                        Plan.withName("Regular"),
                        SourceWarehouse.withName("Central")));

        salesRoom.receiveTitles(bookDepot, titleSupply);

        deliveryVouchers = documentService.listDocuments();
        DeliveryVoucher deliveryVoucher = deliveryVouchers.get(lastDocumentIndex);
        currentDocumentAmount = lastDocumentIndex = deliveryVouchers.size();

        documentService.completeDocument(deliveryVoucher,
                AccountingDocumentInfo.forDeliveryVoucher(Consecutive.of("5")));

        TitleSale titleSale = TitleSetFactory.createTitleSaleBasedOnSupply(titleSupply);

        //WHEN
        pointOfSale.atDepartment(salesRoom).registerSales(titleSale);

        //THEN
        deliveryVouchers = documentService.listDocuments();

        DeliveryVoucher salesSummary = deliveryVouchers.get(lastDocumentIndex);

        Assertions.assertThat(deliveryVouchers).hasSize(currentDocumentAmount + 1);
        Assertions.assertThat(salesSummary.associatedTransferLogs()).hasSize(2);
        Assertions.assertThat(salesSummary).isInstanceOf(SalesSummary.class);
        Assertions.assertThat(salesSummary.getAccountingDocumentType()).isEqualTo(AccountingDocumentType.SALES_SUMMARY);

        //AND WHEN
        documentService.completeDocument(salesSummary,
                AccountingDocumentInfo.forSalesSumary(Consecutive.of("6"),
                        SaleVoucherNumber.of("4"),
                        SaleVoucherNumber.of("8")));
        //THEN
        deliveryVoucher = documentService.findDocumentWithConsecutive(salesSummary.getConsecutive());
        Assertions.assertThat(deliveryVoucher.getConsecutive()).isEqualTo(Consecutive.of("6"));



    }
}
