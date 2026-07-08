package com.jerry.salesmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.scheduling.enabled=false")
class SalesmanagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
