package com.example.staffapp.controller;

import com.example.staffapp.mapper.ItemAPIMapper;
import com.example.staffapp.mapper.InventoryAllocMapper;
import com.example.staffapp.mapper.StaffMapper;
import com.example.staffapp.model.InventoryAlloc;
import com.example.staffapp.model.Item;
import com.example.staffapp.model.Staff;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("api/staff")
public class StaffAppController {
    private StaffMapper staffMapper;
    private ItemAPIMapper itemAPIMapper;
    private InventoryAllocMapper allocMapper;

    public StaffAppController(StaffMapper staffMapper, ItemAPIMapper itemAPIMapper, InventoryAllocMapper allocMapper){
        this.staffMapper = staffMapper;
        this.itemAPIMapper = itemAPIMapper;
        this.allocMapper = allocMapper;
    }

    @GetMapping("/all")
    public List<Staff> getAll(){
        return staffMapper.getAll();
    }

    @GetMapping()
    public Staff getOne(@RequestParam String idStaff){
        return staffMapper.getById(Integer.parseInt(idStaff));
    }

    // ADD STAFF
    @PostMapping()
    public ResponseEntity<?> addStaff(@RequestBody Staff staff) {
        staffMapper.insert(staff);
        Staff inserted = staffMapper.getById(staff.getIdStaff());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("newStaff", inserted);
        jsonObject.put("status", "Staff inserted successfully");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }

    // EDIT STAFF
    @PutMapping()
    public ResponseEntity<?> editStaff(@RequestBody Staff staff) {
        staffMapper.update(staff);
        Staff edited = staffMapper.getById(staff.getIdStaff());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("editedStaff", edited);
        jsonObject.put("status", "Edited staff saved successfully");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }

    // DELETE STAFF
    @DeleteMapping()
    public ResponseEntity<?> deleteStaff(@RequestBody Staff staff) {
        Staff selected = staffMapper.getById(staff.getIdStaff());
        if (selected == null) {
            return new ResponseEntity<>("Staff with ID " + staff.getIdStaff()  + " not found", HttpStatus.NOT_FOUND);
        }
        staffMapper.delete(staff.getIdStaff());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Staff deleted successfully");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }



    // ASSIGN/ALLOCATE INVENTORY TO STAFF
    @PostMapping("/assignItem")
    public ResponseEntity<?> assignItem(@RequestBody InventoryAlloc alloc) throws IOException, TimeoutException {
        // check Inventory and staff availability
        if(itemAPIMapper.getById(alloc.getIdItem()) == null){
            return new ResponseEntity<>("Inventory with ID " + alloc.getIdItem()  + " not found", HttpStatus.NOT_FOUND);
        } else if (!itemAPIMapper.getById(alloc.getIdItem()).isAvailable()){
            return new ResponseEntity<>("Inventory with ID " + alloc.getIdItem()  + " not available", HttpStatus.NOT_FOUND);
        } else if (staffMapper.getById(alloc.getIdStaff()) == null){
            return new ResponseEntity<>("Staff with ID " + alloc.getIdStaff()  + " not found", HttpStatus.NOT_FOUND);
        }

        // insert to invAlloc table
        allocMapper.insert(alloc);

        // update availability in inventory table. But because it's in a different microservice app,
        // we dont have access to edit the inventory database. We use MQ to request for an update.
        JSONObject mqPayload = new JSONObject();
        mqPayload.put("action", "updateAvail");
        mqPayload.put("idItem", alloc.getIdItem());
        mqPayload.put("available", false);
        // kirim ke MQ
        sendMQtoInventoryApp(mqPayload);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Item allocated to user successfully");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }

    // SHOW REPORT BY STAFF
    @GetMapping("invReport/staff")
    public ResponseEntity<?> getInventoryReportByStaff(){
        JSONObject jsonObject = new JSONObject();
        List<Staff> staffList = staffMapper.getAll();
        JSONArray arrReport = new JSONArray();
        for (Staff s : staffList){
            JSONObject jsonReport = new JSONObject();
            jsonReport.put("staff", s.toJsonObject());
            // get the items
            JSONArray arrItems = new JSONArray();
            List<InventoryAlloc> allocs = allocMapper.getAllByStaff(s.getIdStaff());
            for(InventoryAlloc alloc : allocs){
                Item item = itemAPIMapper.getById(alloc.getIdItem());
                arrItems.add(item.toJsonObject());
            }
            jsonReport.put("items",arrItems);
            arrReport.add(jsonReport);
        }
        jsonObject.put("allInvReportByStaff", arrReport);
        jsonObject.put("status", "Retrieval success");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }

    // SHOW REPORT BY ITEM
    @GetMapping("invReport/item")
    public ResponseEntity<?> getInventoryReportByItem(){
        JSONObject jsonObject = new JSONObject();
        List<Item> itemList = itemAPIMapper.getAll();
        JSONArray arrReport = new JSONArray();
        for (Item it : itemList){
            JSONObject jsonReport = new JSONObject();
            jsonReport.put("item", it.toJsonObject());
            // get the staff.. Karena satu item cuman bisa di satu staff, disini tidak perlu array
            InventoryAlloc alloc = allocMapper.getByItem(it.getIdItem());
            if (alloc != null){
                Staff staff = staffMapper.getById(alloc.getIdStaff());
                jsonReport.put("staff",staff.toJsonObject());
                arrReport.add(jsonReport);
            } else {
                jsonReport.put("staff", "not assigned yet");
                arrReport.add(jsonReport);
            }
        }
        jsonObject.put("allInvReportByItem", arrReport);
        jsonObject.put("status", "Retrieval success");
        return new ResponseEntity<>(jsonObject, HttpStatus.CREATED);
    }


    // method send MQ
    private static final String TASK_QUEUE_NAME = "assignItem";

    @Async
    public void sendMQtoInventoryApp(JSONObject mqPayload) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            String message = String.join(" ", mqPayload.toJSONString());
            channel.basicPublish("", TASK_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }


}
