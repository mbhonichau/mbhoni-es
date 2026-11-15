package com.mbhoni_creative.api.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.mbhoni_creative.api.dto.BillingDraftRequest;
import com.mbhoni_creative.api.error.BillingWriteProblem;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class BillingDraftParser {
    private final ObjectMapper mapper;
    public BillingDraftParser(ObjectMapper shared) {
        mapper=shared.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.coercionConfigFor(LogicalType.Textual).setCoercion(CoercionInputShape.Integer,CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float,CoercionAction.Fail).setCoercion(CoercionInputShape.Boolean,CoercionAction.Fail);
        mapper.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    public record Parsed(BillingDraftRequest request,String canonical) {}
    public Parsed parse(String body,boolean invoice) {
        if(body==null||body.length()>262144) throw new BillingWriteProblem(400,"INVALID_REQUEST");
        try {
            JsonNode tree=mapper.readTree(body);
            if(tree==null||!tree.isObject()||tree.has(invoice?"validUntilDate":"dueDate")) throw new BillingWriteProblem(400,"INVALID_REQUEST");
            var r=mapper.treeToValue(tree,BillingDraftRequest.class);
            text(r.customerName(),255,true); text(r.customerEmail(),255,false); text(r.customerPhone(),50,false);
            text(r.customerAddress(),500,false); text(r.notes(),1000,false);
            var end=invoice?r.dueDate():r.validUntilDate();
            if(r.issueDate()==null||end==null||end.isBefore(r.issueDate())||r.issueDate().getYear()<1900||end.getYear()>9999)
                throw new BillingWriteProblem(422,"VALIDATION_FAILED");
            if(r.items()==null||r.items().isEmpty()||r.items().size()>200) throw new BillingWriteProblem(422,"VALIDATION_FAILED");
            for(var line:r.items()) {
                if(line==null) throw new BillingWriteProblem(422,"VALIDATION_FAILED");
                text(line.description(),255,true); money(line.quantity()); money(line.unitPrice());
            }
            return new Parsed(r,mapper.writeValueAsString(canonical(tree)));
        } catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new BillingWriteProblem(400,"INVALID_REQUEST"); }
    }
    public static BigDecimal money(String s) {
        if(s==null||!s.matches("(?:0|[1-9][0-9]{0,9})(?:\\.[0-9]{1,2})?")) throw new BillingWriteProblem(422,"INVALID_MONEY");
        return new BigDecimal(s);
    }
    private static void text(String s,int length,boolean required) {
        if((required&&(s==null||s.isBlank()))||(s!=null&&s.length()>length)) throw new BillingWriteProblem(422,"VALIDATION_FAILED");
    }
    private JsonNode canonical(JsonNode n) {
        if(n.isObject()) { ObjectNode result=mapper.createObjectNode(); var names=new TreeSet<String>();n.fieldNames().forEachRemaining(names::add);
            names.forEach(name->result.set(name,canonical(n.get(name))));return result; }
        if(n.isArray()) { ArrayNode result=mapper.createArrayNode();n.forEach(value->result.add(canonical(value)));return result; }
        return n;
    }
}
