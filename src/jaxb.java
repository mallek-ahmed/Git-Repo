package com.example.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeXmlAdapter extends XmlAdapter<String, OffsetDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public OffsetDateTime unmarshal(String value) {
        return value != null ? OffsetDateTime.parse(value, FORMATTER) : null;
    }

    @Override
    public String marshal(OffsetDateTime value) {
        return value != null ? FORMATTER.format(value) : null;
    }
}



import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
private OffsetDateTime myTimestamp;




<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.0</version>
</dependency>

<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>4.0.3</version>
</dependency>
