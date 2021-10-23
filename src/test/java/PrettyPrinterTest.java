import de.fraunhofer.iais.eis.PersonBuilder;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class aims to demonstrate current problems with the Infomodel serializer
 *
 * First relating issue: https://github.com/International-Data-Spaces-Association/Java-Representation-of-IDS-Information-Model/issues/12
 *
 * While the ObjectMapper from jackson itself is thread-safe, pretty print seems to have problems with extended children.
 * 3 year old Thread: https://groups.google.com/g/jackson-user/c/TTBmwZ_2HaM
 *
 * Jackson Doc mentioning the Instantiatable Interface to avoid corruption: https://fasterxml.github.io/jackson-core/javadoc/2.12/com/fasterxml/jackson/core/PrettyPrinter.html
 * This might be the case with JsonLDSerializer extending BeanSerializer.
 *
 * @author j.schneider@isst.fraunhofer.de
 */
@Slf4j
public class PrettyPrinterTest {

    static Serializer serializer = new Serializer();

    /**
     * This test will fail in most cases.
     * Demonstrating, that the "@context" field necessary for compact json-ld deserialization is missing (sometimes).
     *
     * On some machines a higher loop count is needed, dependent on the scheduler.
     */
    @Test
    public void parallelContextMissing() throws ExecutionException, InterruptedException {
        var infoModelObject = new PersonBuilder()._familyName_("Datenschmidt").build();

        Runnable serFunction = () -> {
            try {
                for (int i = 0; i < 1000000; i++) {
                    String idsJson = serializer.serialize(infoModelObject);
                    assertTrue(idsJson.contains("@context"), "If failed, Context is missing!");
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        };

        Runnable mapperFunction = () -> {
            for (int i = 0; i < 1000000; i++) {
                System.out.println(infoModelObject);
            }

        };

        var future1 = CompletableFuture.runAsync(serFunction);
        var future2 = CompletableFuture.runAsync(mapperFunction);

        // gather the threads so we get the output
        var combined = CompletableFuture.allOf(future1, future2);
        combined.get();
        assertTrue(combined.isDone());
    }
}
