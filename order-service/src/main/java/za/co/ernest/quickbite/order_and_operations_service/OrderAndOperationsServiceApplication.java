package za.co.ernest.quickbite.order_and_operations_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrderAndOperationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderAndOperationsServiceApplication.class, args);
	}

}
