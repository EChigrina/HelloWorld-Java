package com.intuit.developer.helloworld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import com.intuit.developer.helloworld.helper.QBOServiceHelper;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.services.DataService;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class InvoiceController {

    @Autowired
    OAuth2PlatformClientFactory factory;

    @Autowired
    public QBOServiceHelper helper;

    private static final Logger logger = Logger.getLogger(InvoiceController.class);

    @GetMapping("/invoice")
    public String invoiceForm(Model model, HttpSession session) {
        String realmId = (String)session.getAttribute("realmId");
        if (StringUtils.isEmpty(realmId)) {
            model.addAttribute("response", new JSONObject().put("response","No realm ID.  QBO calls only work if the accounting scope was passed!").toString());
            return "connected";
        }
        String accessToken = (String)session.getAttribute("access_token");

        try {
            DataService service = helper.getDataService(realmId, accessToken);

            List<Customer> customers = QBOServiceHelper.executeQuery(service, "select * from Customer");
            model.addAttribute("customers", customers);

            List<Item> items = QBOServiceHelper.executeQuery(service, "select * from Item");
            model.addAttribute("items", items);

            List<Account> accounts = QBOServiceHelper.executeQuery(service, String.format("select * from Account where AccountType='%s' maxresults 1", AccountTypeEnum.INCOME.value()));
            model.addAttribute("accounts", accounts);

            model.addAttribute("invoice", new com.intuit.developer.helloworld.model.Invoice());

            return "invoiceForm";
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            model.addAttribute("response", new JSONObject().put("response", "Failed").toString());
            return "connected";
        }
    }

    @PostMapping("/invoice")
    public String uploadInvoice(HttpSession session, Model model, @ModelAttribute com.intuit.developer.helloworld.model.Invoice invoice) {
        String realmId = (String) session.getAttribute("realmId");
        if (StringUtils.isEmpty(realmId)) {
            model.addAttribute("response", new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString());
            return "connected";
        }
        String accessToken = (String) session.getAttribute("access_token");
        try {
            Invoice qboInvoice = new Invoice();
            DataService service = helper.getDataService(realmId, accessToken);

            List<Customer> customers = QBOServiceHelper.executeQuery(service, String.format("select * from Customer where Id='%s' maxresults 1", invoice.getCustomerId()));

            if (customers.isEmpty()) {
                model.addAttribute("response", "Error accessing the chosen customer!");
                return "connected";
            }
            qboInvoice.setCustomerRef(createRef(customers.get(0)));
            //fill in email
            EmailAddress em = customers.get(0).getPrimaryEmailAddr();
            qboInvoice.setBillEmail(em);
            //fill sales term
            qboInvoice.setSalesTermRef(customers.get(0).getSalesTermRef());
          //  System.out.println("HELLO " + customers.get(0).getDefaultTaxCodeRef().getValue());
            List<TaxCode> taxCodes = QBOServiceHelper.executeQuery(service, String.format("select * from TaxCode where Id='%s' maxresults 1", customers.get(0).getDefaultTaxCodeRef().getValue()));
            if (taxCodes.isEmpty()) {
                model.addAttribute("response", "Error accessing the taxCode!");
                return "connected";
            }
            TxnTaxDetail txnTaxDetail = new TxnTaxDetail();
            ReferenceType referenceType = new ReferenceType();
            referenceType.setValue(taxCodes.get(0).getId());
            referenceType.setName(taxCodes.get(0).getName());
            //txnTaxDetail.setDefaultTaxCodeRef(referenceType);
            txnTaxDetail.setTxnTaxCodeRef(referenceType);
            qboInvoice.setTxnTaxDetail(txnTaxDetail);

            TransactionDeliveryInfo transactionDeliveryInfo = new TransactionDeliveryInfo();
            if(customers.get(0).getPreferredDeliveryMethod().equals("Email")) {
                transactionDeliveryInfo.setDeliveryType(DeliveryTypeEnum.EMAIL);
                qboInvoice.setDeliveryInfo(transactionDeliveryInfo);
            }

            String itemIds = String.join("', '", invoice.getItemId());
            List<Item> items = QBOServiceHelper.executeQuery(service, String.format("select * from Item where Id in ('%s')", itemIds));
            if (items.isEmpty()) {
                model.addAttribute("response", "Error accessing the chosen item!");
                return "connected";
            }

            List<Account> accounts = QBOServiceHelper.executeQuery(service, String.format("select * from Account where Id='%s' maxresults 1", invoice.getAccountId()));
            if (items.isEmpty()) {
                model.addAttribute("response", "Error accessing the chosen item!");
                return "connected";
            }
            Account account = accounts.get(0);
            List<Line> invLine = new ArrayList<>();
            for(Item item : items) {
                item.setIncomeAccountRef(createRef(account));
                SalesItemLineDetail silDetails = new SalesItemLineDetail();
                silDetails.setQty(new BigDecimal(1));
                silDetails.setItemRef(createRef(item));
                item.setSalesTaxCodeRef(referenceType);
               // item.setSalesTaxIncluded(true);
                Line line = new Line();
                line.setAmount(item.getUnitPrice());
                line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
                line.setSalesItemLineDetail(silDetails);
                invLine.add(line);
            }
            qboInvoice.setGlobalTaxCalculation(GlobalTaxCalculationEnum.TAX_EXCLUDED);
            qboInvoice.setLine(invLine);


            Invoice savedInvoice = service.add(qboInvoice);
            model.addAttribute("response", createResponse(savedInvoice));
            return "connected";
        }

        catch (InvalidTokenException e) {
            model.addAttribute("response", new JSONObject().put("response","InvalidToken - Refresh token and try again").toString());
            return "connected";
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            model.addAttribute("response", e.getMessage());
            return "connected";
        }
    }

    private ReferenceType createRef(IntuitEntity entity) {
        ReferenceType referenceType = new ReferenceType();
        referenceType.setValue(entity.getId());
        return referenceType;
    }

    private String createResponse(Object entity) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString;
        try {
            jsonInString = mapper.writeValueAsString(entity);
        }
        catch (Exception e) {
            logger.error("Exception while calling QBO ", e);
            return new JSONObject().put("response","Failed").toString();
        }
        return jsonInString;
    }


}
