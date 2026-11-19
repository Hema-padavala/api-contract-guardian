package com.apicontractguardian;

import com.apiguardian.service.JavaSourceAnalysisService;

public class JavaSourceAnalysisTest {

    public static void main(String[] args) {

        JavaSourceAnalysisService service =
                new JavaSourceAnalysisService();

        String oldCode = """
                public class OrderController {

                    public void getOrders() {
                    }

                    public void createOrder() {
                    }
                }
                """;

        String newCode = """
                public class OrderController {

                    public void getOrders() {
                    }

                    public void updateOrder() {
                    }
                }
                """;

        System.out.println(
                service.compareMethods(oldCode, newCode)
        );
        String controllerCode = """
                public class OrderController {

                    @GetMapping("/orders")
                    public void getOrders() {
                    }

                    @PostMapping("/orders")
                    public void createOrder() {
                    }

                    @DeleteMapping("/orders/{id}")
                    public void deleteOrder() {
                    }
                }
                """;

        System.out.println("Extracted Endpoints: "
                + service.extractEndpoints(controllerCode));
        String oldController = """
                public class OrderController {

                    @GetMapping("/orders")
                    public void getOrders() {}

                    @PostMapping("/orders")
                    public void createOrder() {}
                }
                """;

        String newController = """
                public class OrderController {

                    @GetMapping("/orders")
                    public void getOrders() {}

                    @PutMapping("/orders/{id}")
                    public void updateOrder() {}
                }
                """;

        System.out.println("Endpoint Comparison: "
                + service.compareEndpoints(oldController, newController));
    }
}