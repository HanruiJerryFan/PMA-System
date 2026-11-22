package com.jerry.salesmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.scheduling.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:salesmanagement_context_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password="
})
class SalesmanagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
