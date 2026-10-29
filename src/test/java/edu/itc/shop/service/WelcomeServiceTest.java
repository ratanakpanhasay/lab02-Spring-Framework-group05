package edu.itc.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class WelcomeServiceTest {

    @Test
    void welcomeSendsExpectedMessage() {
        FakeNotifier fake = new FakeNotifier();

        Clock fixedClock = Clock.fixed(
                Instant.parse("2025-01-01T09:15:02Z"),
                ZoneOffset.UTC);

        WelcomeService service = new WelcomeService(
                fake,
                new FakeNotifier(),
                fixedClock,
                (ObjectProvider<ReportBuilder>) null);

        service.welcome("ann", false);

        assertEquals("ann", fake.to);
        assertEquals("Welcome ann! It is 09:15:02", fake.text);
    }

    private static class FakeNotifier implements Notifier {
        private String to;
        private String text;

        @Override
        public void send(String to, String text) {
            this.to = to;
            this.text = text;
        }
    }
}
