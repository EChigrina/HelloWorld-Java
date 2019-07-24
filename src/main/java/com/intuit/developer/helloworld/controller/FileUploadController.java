package com.intuit.developer.helloworld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import com.intuit.developer.helloworld.helper.QBOServiceHelper;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Controller
public class FileUploadController {
    @Autowired
    OAuth2PlatformClientFactory factory;

    @Autowired
    public QBOServiceHelper helper;

    private static final Logger logger = Logger.getLogger(FileUploadController.class);
    @GetMapping("/uploadFile")
    public String uploadFileForm(Model model, HttpSession session) {
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
            model.addAttribute("file", new com.intuit.developer.helloworld.model.File());
            return "uploadFileForm";
        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            List<String> errorMessages = new ArrayList<>();
            list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
            list.forEach(error -> errorMessages.add("Error while calling the API :: " + error.getMessage()));
            model.addAttribute("response", String.join("\n", errorMessages));
            return "connected";
        }
    }

    @PostMapping("/uploadFile")
    public String importQuestion(@Valid @RequestParam("uploadedFileName")
                                         MultipartFile multipart, @ModelAttribute com.intuit.developer.helloworld.model.File file, HttpSession session, Model model) {
        logger.debug("Post method of uploaded Questions ");
        logger.debug("Uploaded fileToUpload Name : " + multipart.getResource().getFilename());
        String realmId = (String) session.getAttribute("realmId");
        if (StringUtils.isEmpty(realmId)) {
            model.addAttribute("response", new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString());
            return "connected";
        }
        String accessToken = (String) session.getAttribute("access_token");
        try {
            DataService service = helper.getDataService(realmId, accessToken);

            Attachable attachable = new Attachable();
            attachable.setFileName(multipart.getOriginalFilename());
            attachable.setContentType("application/pdf");

            AttachableRef ref = new AttachableRef();
            ReferenceType customerRef = new ReferenceType();
            customerRef.setValue(file.getCustomerId());
            customerRef.setType("Customer");
            ref.setEntityRef(customerRef);
            List<AttachableRef> listAttachRef = new ArrayList<>();
            listAttachRef.add(ref);
            attachable.setAttachableRef(listAttachRef);
            Attachable savedAttachable = service.upload(attachable, multipart.getInputStream());
            model.addAttribute("response", createResponse(savedAttachable));
            return "connected";
        }
        catch(Exception e) {
            model.addAttribute("response", new JSONObject().put("response", e.getMessage()).toString());
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
