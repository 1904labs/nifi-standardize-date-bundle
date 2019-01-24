/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nineteen04labs.processors.standardizedate;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nineteen04labs.processors.util.FormatStream;
import com.nineteen04labs.processors.util.ManipulateDate;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.codehaus.jackson.node.NullNode;

@Tags({"date", "time", "datetime", "standardize", "standardization"})
@CapabilityDescription("NiFi processor to standardize date fields in a FlowFile.")
public class StandardizeDate extends AbstractProcessor {

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(StandardizeDateProperties.FLOW_FORMAT);
        descriptors.add(StandardizeDateProperties.AVRO_SCHEMA);
        descriptors.add(StandardizeDateProperties.INVALID_DATES);
        descriptors.add(StandardizeDateProperties.TIMEZONE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(StandardizeDateRelationships.REL_SUCCESS);
        relationships.add(StandardizeDateRelationships.REL_FAILURE);
        relationships.add(StandardizeDateRelationships.REL_BYPASS);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        try {
            final String invalidDatesString = context.getProperty(StandardizeDateProperties.INVALID_DATES).evaluateAttributeExpressions(flowFile).getValue();
            if ("".equals(invalidDatesString) || invalidDatesString == null) {
                session.transfer(flowFile, StandardizeDateRelationships.REL_BYPASS);
                return;
            }
            final String flowFormat = context.getProperty(StandardizeDateProperties.FLOW_FORMAT).getValue();
            final String schemaString = context.getProperty(StandardizeDateProperties.AVRO_SCHEMA).evaluateAttributeExpressions(flowFile).getValue();
            final String timezone = context.getProperty(StandardizeDateProperties.TIMEZONE).evaluateAttributeExpressions(flowFile).getValue();
            
            session.write(flowFile, new StreamCallback(){
                @Override
                public void process(InputStream in, OutputStream out) throws IOException {
                    Map<String,String> invalidDates = new ObjectMapper()
                        .readValue(invalidDatesString, new TypeReference<CaseInsensitiveMap<String,String>>(){});

                    JsonFactory jsonFactory = new JsonFactory().setRootValueSeparator(null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    JsonParser jsonParser;
                    JsonGenerator jsonGen = jsonFactory.createGenerator(baos);
                    
                    Schema schema = null;
                    List<Schema.Field> schemaFields;
                    Set<Schema.Field> newSchemaFields = new HashSet<>();
                    if (flowFormat.equals("AVRO")) {
                        try {
                            schema = new Schema.Parser().parse(schemaString);
                        } catch (NullPointerException e) {
                            schema = FormatStream.getEmbeddedSchema(in);
                            in.reset();
                        }
                        schemaFields = schema.getFields();
                        for(Schema.Field f : schemaFields) {
                            Schema.Field oldField = new Schema.Field(f.name(), f.schema(), f.doc(), f.defaultVal());
                            newSchemaFields.add(oldField);
                        }
                        in = FormatStream.avroToJson(in, schema);
                    }

                    Reader r = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(r);
                    String line;

                    while ((line = br.readLine()) != null) {
                        jsonParser = jsonFactory.createParser(line);
                        while (jsonParser.nextToken() != null) {
                            jsonGen.copyCurrentEvent(jsonParser);
                            String tokenString = jsonParser.getText();
                            if(jsonParser.getCurrentToken() == JsonToken.FIELD_NAME && invalidDates.containsKey(tokenString)) {
                                jsonParser.nextToken();
                                jsonGen.copyCurrentEvent(jsonParser);

                                String newFieldName = tokenString + "_standardized";
                                jsonGen.writeFieldName(newFieldName);

                                String invalidDate = jsonParser.getText();
                                String invalidDateFormat = invalidDates.get(tokenString);
                                String standardizedDate;

                                try {
                                    if (invalidDate != "null") {
                                        standardizedDate = ManipulateDate.standardize(invalidDate, invalidDateFormat, timezone);
                                        jsonGen.writeString(standardizedDate);
                                    }
                                    else
                                        jsonGen.writeNull();
                                } catch (Exception e) {
                                    throw new ProcessException("Couldn't convert '" + invalidDate + "' with format '" + invalidDateFormat + "' with timezone '" + timezone + "'");
                                }

                                if (flowFormat.equals("AVRO")) {
                                    if (schema.getField(tokenString).schema().getType() == Schema.Type.UNION) {
                                        ArrayList<Schema> unionSchema = new ArrayList<>();
                                        unionSchema.add(Schema.create(Schema.Type.NULL));
                                        unionSchema.add(Schema.create(Schema.Type.STRING));
                                        newSchemaFields.add(new Schema.Field(newFieldName, Schema.createUnion(unionSchema), null, NullNode.getInstance()));
                                    } else
                                        newSchemaFields.add(new Schema.Field(newFieldName, Schema.create(Type.STRING), null, "null"));
                                }
                            }
                        }
                        jsonGen.writeRaw("\n");
                    }
                    jsonGen.flush();

                    if (flowFormat.equals("AVRO")) {
                        Schema newSchema = Schema.createRecord(schema.getName(), schema.getDoc(), schema.getNamespace(), schema.isError());
                        newSchema.setFields(new ArrayList<>(newSchemaFields));
                        baos = FormatStream.jsonToAvro(baos, newSchema);
                    }

                    baos.writeTo(out);
                }
            });
            
            session.transfer(flowFile, StandardizeDateRelationships.REL_SUCCESS);

        } catch (ProcessException e) {
            getLogger().error("Something went wrong", e);
            session.transfer(flowFile, StandardizeDateRelationships.REL_FAILURE);
        }
    }
}
