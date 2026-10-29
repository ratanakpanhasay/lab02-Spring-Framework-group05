package edu.itc.shop.service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Notifier notifier;
    private final Notifier urgent;
    private final Clock clock;
    private final ObjectProvider<ReportBuilder> reportBuilderProvider;

    public WelcomeService(
            Notifier notifier,
            @Qualifier("smsNotifier") Notifier urgent,
            Clock clock,
            ObjectProvider<ReportBuilder> reportBuilderProvider) {
        this.notifier = notifier;
        this.urgent = urgent;
        this.clock = clock;
        this.reportBuilderProvider = reportBuilderProvider;
    }

    public void welcome(String user, boolean urgentMessage) {
        String time = LocalTime.now(clock).format(TIME_FORMAT);
        String message = "Welcome " + user + "! It is " + time;

        if (urgentMessage) {
            urgent.send(user, message);
        } else {
            notifier.send(user, message);
        }
    }

    public boolean hasDifferentReportBuilderInstances() {
        ReportBuilder first = reportBuilderProvider.getObject();
        ReportBuilder second = reportBuilderProvider.getObject();

        System.out.println(
                "ReportBuilder identity hash codes: "
                        + System.identityHashCode(first) + ", "
                        + System.identityHashCode(second));

        return first != second;
    }
}
