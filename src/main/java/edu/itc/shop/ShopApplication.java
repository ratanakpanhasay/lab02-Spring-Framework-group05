package edu.itc.shop;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import edu.itc.shop.service.WelcomeService;
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

    @Bean
    @ConditionalOnBean(WelcomeService.class)
    CommandLineRunner showBeans(
            ApplicationContext context,
            WelcomeService welcomeService) {
        return args -> {
            String[] notifierNames = Arrays.stream(context.getBeanDefinitionNames())
                    .filter(name -> name.toLowerCase().contains("notifier"))
                    .sorted()
                    .toArray(String[]::new);

            System.out.println(context.getBeanDefinitionCount()
                    + " beans; notifiers = "
                    + Arrays.toString(notifierNames));

            System.out.println("prototype instances differ: "
                    + welcomeService.hasDifferentReportBuilderInstances());

            welcomeService.welcome("ann", false);
            welcomeService.welcome("bob", true);
        };
    }
}
