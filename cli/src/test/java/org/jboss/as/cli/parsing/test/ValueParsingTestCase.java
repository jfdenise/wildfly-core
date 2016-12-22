/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.parsing.test;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.impl.aesh.parser.CompositeParser;
import org.jboss.as.cli.impl.aesh.parser.HeadersParser;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class ValueParsingTestCase {

    @Test
    public void testHeaders() throws CommandFormatException {
        String value = "    {ax=true; rollout ( server_group ) toto;}";
        String others = " --doit";
        String remain = new HeadersParser().parse(value + others, null, null);
        assertTrue(remain, others.equals(remain));
    }

    @Test
    public void testObject1() throws CommandFormatException {
        String value = "    {az=10,ax={}, az=[{},{},{}]}";
        String others = " --doit={}";
        String remain = new CompositeParser().parse(value + others, null, null);
        assertTrue(remain, others.equals(remain));
    }

    @Test
    public void testObject2() throws CommandFormatException {
        String value = "{}";
        String others = " --doit={}";
        String remain = new CompositeParser().parse(value + others, null, null);
        assertTrue(remain, others.equals(remain));
    }

    @Test
    public void testList1() throws CommandFormatException {
        String value = "[{az=10,ax={}, az=[{},{},{}]}, {az=10,ax={}, az=[{},{},{}]}]";
        String others = " --doit={}";
        String remain = new CompositeParser().parse(value + others, null, null);
        assertTrue(remain, others.equals(remain));
    }

    @Test
    public void testList2() throws CommandFormatException {
        String value = "         [{az=10,ax={}, az=[{},{},{}]}, {az=10,ax={}, az=[{},{},{}]}]";
        String others = "     --doit={}";
        String remain = new CompositeParser().parse(value + others, null, null);
        assertTrue(remain, others.equals(remain));
    }

}
