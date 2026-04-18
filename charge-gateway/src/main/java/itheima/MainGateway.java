package itheima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MainGateway {
    public static void main(String[] args) {
        SpringApplication.run(MainGateway.class, args);
    }
}