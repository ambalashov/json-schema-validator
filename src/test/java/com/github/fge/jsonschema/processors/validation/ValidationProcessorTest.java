/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonschema.processors.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.SyntaxChecker;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.CanonicalSchemaTree;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.tree.SimpleJsonTree;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class ValidationProcessorTest
{
    private static final String K1 = "k1";
    private static final ObjectNode RAWSCHEMA;
    private static final ArrayNode RAWINSTANCE;
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    static {
        final JsonNodeFactory factory = JacksonUtils.nodeFactory();

        RAWSCHEMA = factory.objectNode();
        RAWSCHEMA.put("minItems", 2)
            .put("items", factory.objectNode().put(K1, 0));

        RAWINSTANCE = factory.arrayNode();
        RAWINSTANCE.add(1);
    }

    private Processor<FullData, FullData> processor;

    @BeforeMethod
    public void init()
    {
        final Keyword keyword = Keyword.newBuilder(K1)
            .withSyntaxChecker(mock(SyntaxChecker.class))
            .withIdentityDigester(NodeType.ARRAY, NodeType.values())
            .withValidatorClass(K1Validator.class)
            .freeze();
        final Library library = DraftV4Library.get().thaw()
            .addKeyword(keyword).freeze();
        final ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
            .setDefaultLibrary("foo://bar#", library).freeze();
        final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(cfg).freeze();
        processor = factory.getProcessor();
        COUNT.set(0);
    }

    @Test
    public void childrenAreNotExploredByDefaultIfContainerFails()
        throws ProcessingException
    {
        final SchemaTree schema = new CanonicalSchemaTree(RAWSCHEMA);
        final JsonTree instance = new SimpleJsonTree(RAWINSTANCE);
        final FullData data = new FullData(schema, instance);
        final ProcessingReport report = mock(ProcessingReport.class);
        processor.process(report, data);
        assertEquals(COUNT.get(), 0);
    }

    @Test
    public void childrenAreExploredOnDemandEvenIfContainerFails()
        throws ProcessingException
    {
        final SchemaTree schema = new CanonicalSchemaTree(RAWSCHEMA);
        final JsonTree instance = new SimpleJsonTree(RAWINSTANCE);
        final FullData data = new FullData(schema, instance, true);
        final ProcessingReport report = mock(ProcessingReport.class);
        processor.process(report, data);
        assertEquals(COUNT.get(), 1);
    }

    public static final class K1Validator
        extends AbstractKeywordValidator
    {
        public K1Validator(final JsonNode digest)
        {
            super(K1);
        }

        @Override
        public void validate(final Processor<FullData, FullData> processor,
            final ProcessingReport report, final MessageBundle bundle,
            final FullData data)
            throws ProcessingException
        {
            COUNT.incrementAndGet();
        }

        @Override
        public String toString()
        {
            return K1;
        }
    }
}