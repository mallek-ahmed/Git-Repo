<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <version>3.6.0</version>
  <executions>
    <execution>
      <goals>
        <goal>analyze</goal>
      </goals>
    </execution>
  </executions>
</plugin>
    
    
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

package com.example.jaxb;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests complets pour {@link OffsetDateTimeXmlAdapter}.
 */
class OffsetDateTimeXmlAdapterTest {

    // SUT (System Under Test)
    private final XmlAdapter<String, OffsetDateTime> adapter = new OffsetDateTimeXmlAdapter();

    @Test
    @DisplayName("marshal() doit renvoyer une date ISO-8601 avec offset")
    void givenOffsetDateTime_whenMarshal_thenReturnsIsoString() throws Exception {
        OffsetDateTime odt =
                OffsetDateTime.of(2025, 7, 3, 15, 30, 45, 123_000_000, ZoneOffset.UTC);
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String actual = adapter.marshal(odt);

        assertEquals(expected, actual, "Le format ISO-8601 avec offset doit être respecté");
    }

    @Test
    @DisplayName("unmarshal() doit reconstituer l'OffsetDateTime d'origine")
    void givenIsoString_whenUnmarshal_thenReturnsOffsetDateTime() throws Exception {
        String iso = "2025-07-03T15:30:45.123Z";
        OffsetDateTime expected = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        OffsetDateTime actual = adapter.unmarshal(iso);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("marshal(null) et unmarshal(null) doivent renvoyer null")
    void givenNull_whenMarshalOrUnmarshal_thenReturnsNull() throws Exception {
        assertNull(adapter.marshal(null));
        assertNull(adapter.unmarshal(null));
    }

    @Test
    @DisplayName("unmarshal() doit lever DateTimeParseException sur format invalide")
    void givenInvalidString_whenUnmarshal_thenThrowsException() {
        String invalid = "03/07/2025 15:30";

        assertThrows(DateTimeParseException.class, () -> adapter.unmarshal(invalid));
    }

    @Test
    @DisplayName("Round-trip marshal→unmarshal doit être idempotent")
    void givenOffsetDateTime_whenMarshalAndUnmarshal_thenEqual() throws Exception {
        OffsetDateTime original = OffsetDateTime.now(ZoneOffset.ofHours(2)).withNano(0); // nanos=0 pour comparaison facile

        String xml = adapter.marshal(original);
        OffsetDateTime result = adapter.unmarshal(xml);

        assertEquals(original, result, "Après un aller-retour JAXB, la valeur doit être identique");
    }
}



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
