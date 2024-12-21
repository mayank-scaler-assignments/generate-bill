package com.scaler.controllers;

import com.scaler.dtos.*;
import com.scaler.models.*;
import com.scaler.repositories.CustomerSessionRepository;
import com.scaler.repositories.OrderRepository;
import com.scaler.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestOrderController {

    private OrderController orderController;
    private OrderRepository orderRepository;
    private CustomerSessionRepository customerSessionRepository;
    private OrderService orderService;


    @BeforeEach
    public void setupTest() throws Exception {
        initializeComponents();
    }

    private void initializeComponents() throws Exception {
        initializeRepositories();
        initializeOrderService();
        initializeTicketController();
    }

    private <T> T createInstance(Class<T> interfaceClass, Reflections reflections) throws Exception {
        Set<Class<? extends T>> implementations = reflections.getSubTypesOf(interfaceClass);
        if (implementations.isEmpty()) {
            throw new Exception("No implementation for " + interfaceClass.getSimpleName() + " found");
        }

        Class<? extends T> implementationClass = implementations.iterator().next();
        Constructor<? extends T> constructor = implementationClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private <T> T createInstanceWithArgs(Class<T> interfaceClass, Reflections reflections, List<Object> dependencies) throws Exception {
        Set<Class<? extends T>> implementations = reflections.getSubTypesOf(interfaceClass);
        if (implementations.isEmpty()) {
            throw new Exception("No implementation for " + interfaceClass.getSimpleName() + " found");
        }
        Class<? extends T> implementationClass = implementations.iterator().next();
        Constructor<?>[] constructors = implementationClass.getConstructors();
        Constructor<?> constructor = Arrays.stream(constructors)
                .filter(constructor1 -> constructor1.getParameterCount() == dependencies.size())
                .findFirst().orElseThrow(() -> new Exception("No constructor with " + dependencies.size() + " arguments found"));
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            for (Object dependency : dependencies) {
                if (constructor.getParameterTypes()[i].isInstance(dependency)) {
                    args[i] = dependency;
                    break;
                }
            }
        }
        return (T) constructor.newInstance(args);
    }

    private void initializeRepositories() throws Exception {
        Reflections repositoryReflections = new Reflections(CustomerSessionRepository.class.getPackageName(), new SubTypesScanner(false));
        this.customerSessionRepository = createInstance(CustomerSessionRepository.class, repositoryReflections);
        this.orderRepository = createInstance(OrderRepository.class, repositoryReflections);
    }


    private void initializeOrderService() throws Exception {
        Reflections serviceReflections = new Reflections(OrderService.class.getPackageName(), new SubTypesScanner(false));
        this.orderService = createInstanceWithArgs(OrderService.class, serviceReflections, Arrays.asList(this.customerSessionRepository, this.orderRepository));
    }

    private void initializeTicketController() {
        this.orderController = new OrderController(this.orderService);
    }




    @Test
    public void testGenerateBill_1_Order_Success(){
        User user = setupUser();
        List<MenuItem> menuItems = setupMenuItems();
        Map<MenuItem, Integer> orderedItems = new HashMap<>();
        orderedItems.put(menuItems.get(0), 2);
        orderedItems.put(menuItems.get(1), 1);

        CustomerSession customerSession = new CustomerSession();
        customerSession.setCustomerSessionStatus(CustomerSessionStatus.ACTIVE);
        customerSession.setUser(user);
        customerSession = this.customerSessionRepository.save(customerSession);

        Order order = new Order();
        order.setCustomerSession(customerSession);
        order.setOrderedItems(orderedItems);
        order = this.orderRepository.save(order);


        GenerateBillRequestDto generateBillRequestDto = new GenerateBillRequestDto();
        generateBillRequestDto.setUserId(user.getId());
        GenerateBillResponseDto generateBillResponseDto = orderController.generateBill(generateBillRequestDto);
        assertEquals(generateBillResponseDto.getResponseStatus(), ResponseStatus.SUCCESS, "Generate bill should be successful");
        assertNotNull(generateBillResponseDto.getBill(), "Bill should not be null");
        Bill bill = generateBillResponseDto.getBill();
        assertEquals(2, bill.getOrderedItems().size(), "Bill should have 2 items");
        double totalAmount = 0;
        for (Map.Entry<MenuItem, Integer> entrySet : orderedItems.entrySet()) {
            totalAmount += menuItems.stream().filter(menuItem -> menuItem.getId() == entrySet.getKey().getId()).findFirst().get().getPrice() * entrySet.getValue();
        }
        assertEquals(totalAmount * 1.15, bill.getTotalAmount(), 0.1, "Total amount should be correct");
        assertEquals(totalAmount * 0.05, bill.getGst(), 0.1, "GST should be correct");
        assertEquals(totalAmount * 0.1, bill.getServiceCharge(), 0.1, "Service charge should be correct");

        this.customerSessionRepository.findActiveCustomerSessionByUserId(user.getId()).ifPresent(customerSession1 -> {
            assertEquals(CustomerSessionStatus.ENDED, customerSession1.getCustomerSessionStatus(), "Customer session status should be ended");
        });
    }

    @Test
    public void testGenerateBill_2_Orders_Success(){
        User user = setupUser();

        CustomerSession customerSession = new CustomerSession();
        customerSession.setCustomerSessionStatus(CustomerSessionStatus.ACTIVE);
        customerSession.setUser(user);
        customerSession = this.customerSessionRepository.save(customerSession);

        List<MenuItem> menuItems = setupMenuItems();
        Map<MenuItem, Integer> firstOrder = new HashMap<>();
        firstOrder.put(menuItems.get(0), 2);
        firstOrder.put(menuItems.get(1), 1);

        Order order1 = new Order();
        order1.setOrderedItems(firstOrder);
        order1.setCustomerSession(customerSession);
        order1 = this.orderRepository.save(order1);

        Map<MenuItem, Integer> secondOrder = new HashMap<>();
        secondOrder.put(menuItems.get(1), 1);
        secondOrder.put(menuItems.get(2), 1);
        secondOrder.put(menuItems.get(3), 2);

        Order order2 = new Order();
        order2.setOrderedItems(secondOrder);
        order2.setCustomerSession(customerSession);
        order2 = this.orderRepository.save(order2);

        GenerateBillRequestDto generateBillRequestDto = new GenerateBillRequestDto();
        generateBillRequestDto.setUserId(user.getId());
        GenerateBillResponseDto generateBillResponseDto = orderController.generateBill(generateBillRequestDto);
        assertEquals(generateBillResponseDto.getResponseStatus(), ResponseStatus.SUCCESS, "Generate bill should be successful");
        assertNotNull(generateBillResponseDto.getBill(), "Bill should not be null");
        Bill bill = generateBillResponseDto.getBill();
        assertEquals(4, bill.getOrderedItems().size(), "Bill should have 3 items");
        double totalAmount = 0;
        for (Map.Entry<MenuItem, Integer> entrySet : firstOrder.entrySet()) {
            totalAmount += menuItems.stream().filter(menuItem -> menuItem.getId() == entrySet.getKey().getId()).findFirst().get().getPrice() * entrySet.getValue();
        }
        for (Map.Entry<MenuItem, Integer> entrySet : secondOrder.entrySet()) {
            totalAmount += menuItems.stream().filter(menuItem -> menuItem.getId() == entrySet.getKey().getId()).findFirst().get().getPrice() * entrySet.getValue();
        }
        assertEquals(totalAmount * 1.15, bill.getTotalAmount(), 0.1, "Total amount should be correct");
        assertEquals(totalAmount * 0.05, bill.getGst(), 0.1, "GST should be correct");
        assertEquals(totalAmount * 0.1, bill.getServiceCharge(), 0.1, "Service charge should be correct");

        this.customerSessionRepository.findActiveCustomerSessionByUserId(user.getId()).ifPresent(customerSession1 -> {
            assertEquals(CustomerSessionStatus.ENDED, customerSession1.getCustomerSessionStatus(), "Customer session status should be ended");
        });
    }

    @Test
    public void testGenerateBill_InvalidCustomerSession(){
        User user = setupUser();
        List<MenuItem> menuItems = setupMenuItems();
        CustomerSession customerSession = new CustomerSession();
        customerSession.setCustomerSessionStatus(CustomerSessionStatus.ACTIVE);
        customerSession.setUser(user);
        customerSession = this.customerSessionRepository.save(customerSession);

        Map<MenuItem, Integer> orderedItems = new HashMap<>();
        orderedItems.put(menuItems.get(0), 2);
        orderedItems.put(menuItems.get(1), 1);

        Order order = new Order();
        order.setCustomerSession(customerSession);
        order.setOrderedItems(orderedItems);
        this.orderRepository.save(order);

        GenerateBillRequestDto generateBillRequestDto = new GenerateBillRequestDto();
        generateBillRequestDto.setUserId(user.getId() + 1);
        GenerateBillResponseDto generateBillResponseDto = orderController.generateBill(generateBillRequestDto);
        assertEquals(generateBillResponseDto.getResponseStatus(), ResponseStatus.FAILURE, "Generate bill should be failure");
        assertNull(generateBillResponseDto.getBill(), "Bill should be null");
    }

    public User setupUser(){
        User user = new User();
        user.setName("Test User");
        user.setPhone("1234567890");
        user.setUserType(UserType.CUSTOMER);
        user.setId(1L);
        return user;
    }

    public List<MenuItem> setupMenuItems(){
        List<MenuItem> menuItems = new ArrayList<>();
        MenuItem menuItem = new MenuItem();
        menuItem.setName("Paneer Tikka");
        menuItem.setPrice(200);
        menuItem.setDietaryRequirement(DietaryRequirement.VEG);
        menuItem.setItemType(ItemType.REGULAR);
        menuItem.setDescription("Paneer Tikka is a vegetarian dish from the Indian subcontinent made from paneer marinated in spices and grilled in a tandoor.");
        menuItem.setId(1L);
        menuItems.add(menuItem);

        menuItem = new MenuItem();
        menuItem.setName("Chicken Tikka");
        menuItem.setPrice(300);
        menuItem.setDietaryRequirement(DietaryRequirement.NON_VEG);
        menuItem.setItemType(ItemType.REGULAR);
        menuItem.setDescription("Chicken tikka is a chicken dish originating in the Indian subcontinent; the dish is popular in India, Bangladesh and Pakistan.");
        menuItem.setId(2L);
        menuItems.add(menuItem);

        menuItem = new MenuItem();
        menuItem.setName("Chicken Tikka Masala");
        menuItem.setPrice(400);
        menuItem.setDietaryRequirement(DietaryRequirement.NON_VEG);
        menuItem.setItemType(ItemType.REGULAR);
        menuItem.setDescription("Chicken tikka masala is a dish consisting of roasted marinated chicken chunks (chicken tikka) in spiced curry sauce.");
        menuItem.setId(3L);
        menuItems.add(menuItem);

        menuItem = new MenuItem();
        menuItem.setName("Paneer Tandoori");
        menuItem.setPrice(250);
        menuItem.setDietaryRequirement(DietaryRequirement.VEG);
        menuItem.setItemType(ItemType.REGULAR);
        menuItem.setDescription("Paneer tikka is an Indian dish made from chunks of paneer marinated in spices and grilled in a tandoor.");
        menuItem.setId(4L);
        menuItems.add(menuItem);

        menuItem = new MenuItem();
        menuItem.setName("Paneer Tikka Masala");
        menuItem.setPrice(350);
        menuItem.setDietaryRequirement(DietaryRequirement.VEG);
        menuItem.setItemType(ItemType.REGULAR);
        menuItem.setDescription("Paneer tikka masala is an Indian dish of marinated paneer cheese served in a spiced gravy.");
        menuItem.setId(5L);
        menuItems.add(menuItem);

        return menuItems;
    }

}
