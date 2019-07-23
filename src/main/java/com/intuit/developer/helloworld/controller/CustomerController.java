package com.intuit.developer.helloworld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import com.intuit.developer.helloworld.helper.QBOServiceHelper;
import com.intuit.developer.helloworld.model.Customer;
import com.intuit.ipp.data.EmailAddress;
import com.intuit.ipp.data.Error;
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
import java.util.List;

@Controller
public class CustomerController {
    @Autowired
    OAuth2PlatformClientFactory factory;

    @Autowired
    public QBOServiceHelper helper;

    private static final Logger logger = Logger.getLogger(InvoiceController.class);

    @GetMapping("/customerForm")
	public String customerForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customerForm";
	}

    @PostMapping("/customerForm")
    public String customerForm(HttpSession session, Model model, @ModelAttribute Customer customer) {
        logger.info("inside customerForm post"  );
        String realmId = (String)session.getAttribute("realmId");
        if (StringUtils.isEmpty(realmId)) {
            model.addAttribute("response", new JSONObject().put("response","No realm ID.  QBO calls only work if the accounting scope was passed!").toString());
            return "connected";
        }
        String accessToken = (String)session.getAttribute("access_token");
        try {
            com.intuit.ipp.data.Customer qboCustomer = new com.intuit.ipp.data.Customer();

            DataService service = helper.getDataService(realmId, accessToken);

            qboCustomer.setDisplayName(customer.getName());

            EmailAddress em = new EmailAddress();
            em.setAddress(customer.getEmail());
            qboCustomer.setPrimaryEmailAddr(em);

            com.intuit.ipp.data.Customer savedCustomer = service.add(qboCustomer);

            model.addAttribute("response", createResponse(savedCustomer));
            return "connected";
        }
        catch (InvalidTokenException e) {
            model.addAttribute("response", new JSONObject().put("response","InvalidToken - Refresh token and try again").toString());
            return "connected";
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            model.addAttribute("response", new JSONObject().put("response", "Failed").toString());
            return "connected";
        }
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

//Code to upload pdf attachment:
/* File file = new File("/home/codeinside/Postman/files/test first 11-07-2019 .pdf");
InputStream in = new FileInputStream(file);

Attachable attachable = new Attachable();
attachable.setFileName("test first 11-07-2019 .pdf");
attachable.setContentType("application/pdf");

AttachableRef ref = new AttachableRef();
ReferenceType customerRef = new ReferenceType();
customerRef.setValue(savedCustomer.getId());
customerRef.setType("Customer");
ref.setEntityRef(customerRef);
List<AttachableRef> listAttachRef = new ArrayList<>();
listAttachRef.add(ref);
attachable.setAttachableRef(listAttachRef);
Attachable savedAttachable = service.upload(attachable, in);*/