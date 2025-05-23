package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.common.Attributes; // Import Attributes
import io.opentelemetry.api.common.AttributeKey; // Import AttributeKey
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger; // SLF4J Logger
import org.slf4j.LoggerFactory; // SLF4J LoggerFactory

@RestController
public class HelloController {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.demo.HelloController");
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class); // SLF4J Logger

    @GetMapping("/hello")
    public String hello() {
        // Start a custom span
        Span span = tracer.spanBuilder("hello-method-span").startSpan();
        try {
            // Add an event (like a structured log) to the span
            span.addEvent("Processing hello request", Attributes.of(AttributeKey.stringKey("message"), "Starting to process /hello"));

            logger.info("Inside /hello endpoint, custom span active."); // Regular log

            // Simulate some work
            Thread.sleep(100); // Optional: to make the span visible

            String response = "Hello, World!";
            span.setAttribute("http.response.body", response); // Add an attribute to the span

            span.addEvent("Finished processing hello request", Attributes.of(AttributeKey.stringKey("message"), "Successfully processed /hello"));
            
            return response;
        } catch (InterruptedException e) {
            span.recordException(e); // Record exception if any
            Thread.currentThread().interrupt();
            return "Error processing request";
        } finally {
            span.end(); // Always end the span
        }
    }
}
