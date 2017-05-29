package com.ditcherj.contextio.deserializers;

import com.ditcherj.contextio.dto.EmailAddress;
import com.ditcherj.contextio.responses.ListEmailAddressesResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by Jonathan Ditcher on 08/04/2017.
 */
public class EmailAddressesDeserializer extends StdDeserializer<ListEmailAddressesResponse> {

    public EmailAddressesDeserializer() {
        this((Class<?>) null);
    }

    public EmailAddressesDeserializer(Class<?> vc) {
        super(vc);
    }

    public EmailAddressesDeserializer(JavaType valueType) {
        super(valueType);
    }

    public EmailAddressesDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public ListEmailAddressesResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        ListEmailAddressesResponse response = new ListEmailAddressesResponse();

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if(node.has("error")) {
            response.setError(node.get("error").textValue());
        }

        List<EmailAddress> emailAddresses = new LinkedList<>();

        node.iterator().forEachRemaining(new Consumer<JsonNode>() {
            @Override
            public void accept(JsonNode jsonNode) {

                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setEmail(jsonNode.get("email").textValue());
                emailAddress.setPrimary(jsonNode.get("primary").asBoolean());

                if(jsonNode.has("validated"))
                    emailAddress.setValidated(jsonNode.get("validated").asLong());

                if(jsonNode.has("resource_url"))
                    emailAddress.setResource_url(jsonNode.get("resource_url").textValue());

                emailAddresses.add(emailAddress);
            }
        });

        response.setEmailAddresses(emailAddresses);

        return response;
    }
}
