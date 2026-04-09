package by.dzarembo.traineeorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TraineeOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraineeOrderServiceApplication.class, args);
    }

}
