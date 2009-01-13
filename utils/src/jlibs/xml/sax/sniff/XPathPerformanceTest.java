/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff;

import jlibs.xml.DefaultNamespaceContext;
import jlibs.xml.dom.DOMUtil;
import jlibs.xml.sax.helpers.SAXHandler;
import jlibs.xml.sax.helpers.NamespaceSupportReader;
import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Enumeration;

/**
 * @author Santhosh Kumar T
 */
public class XPathPerformanceTest{
    public List<List<String>> translateJDKResults(TestCase testCase){
        List<List<String>> results = new ArrayList<List<String>>();
        for(NodeList nodeSet: testCase.jdkResult){
            List<String> result = new ArrayList<String>();
            for(int i=0; i<nodeSet.getLength(); i++){
                Node node = nodeSet.item(i);
                if(node instanceof Attr)
                    result.add(node.getNodeValue());
                else if(node instanceof Element){
                    StringBuilder buff = new StringBuilder();
                    while(!(node instanceof Document)){
                        String prefix = testCase.nsContext.getPrefix(node.getNamespaceURI());
                        buff.insert(0, "["+DOMUtil.getPosition((Element)node)+"]");
                        buff.insert(0, prefix.length()==0 ? node.getLocalName() : prefix+':'+node.getLocalName());
                        buff.insert(0, '/');
                        node = node.getParentNode();
                    }
                    result.add(buff.toString());
                }else if(node instanceof Text)
                    result.add(node.getNodeValue());
            }
            results.add(result);
        }
        return results;
    }

    private List<TestCase> testCases = new ArrayList<TestCase>();

    public void readTestCases(String configFile) throws Exception{
        new NamespaceSupportReader(true).parse(configFile, new SAXHandler(){
            TestCase testCase;
            CharArrayWriter contents = new CharArrayWriter();

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
                if(localName.equals("testcase")){
                    testCases.add(testCase = new TestCase());
                    Enumeration<String> enumer = nsSupport.getPrefixes();
                    while(enumer.hasMoreElements()){
                        String prefix = enumer.nextElement();
                        testCase.nsContext.declarePrefix(prefix, nsSupport.getURI(prefix));
                    }
                    if(nsSupport.getURI("")!=null)
                        testCase.nsContext.declarePrefix("", nsSupport.getURI(""));
                }
                contents.reset();
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException{
                contents.write(ch, start, length);
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException{
                if(localName.equals("file"))
                    testCase.file = contents.toString().trim();
                else if(localName.equals("xpath"))
                    testCase.xpaths.add(contents.toString().trim());

            }
        });
    }

    public void run(String configFile) throws Exception{
        readTestCases(configFile);

        long time = System.nanoTime();
        for(TestCase testCase: testCases)
            testCase.dogResult = testCase.usingXMLDog();
        long dogTime = System.nanoTime() - time;

        time = System.nanoTime();
        for(TestCase testCase: testCases)
            testCase.jdkResult = testCase.usingJDK();
        long jdkTime = System.nanoTime() - time;

        int total= 0;
        int failed = 0;
        for(TestCase testCase: testCases){
            total += testCase.jdkResult.size();
            List<List<String>> jdkResults = translateJDKResults(testCase);

            PrintStream stream = System.out;

            for(int i=0; i<testCase.xpaths.size(); i++){
                List<String> jdkResult = jdkResults.get(i);
                List<String> dogResult = testCase.dogResult.get(i);

                if(testCase.xpaths.get(i).contains("@")){
                    Collections.sort(jdkResult);
                    Collections.sort(dogResult);
                }

                boolean matched = jdkResult.equals(dogResult);

                System.out.println(matched ? "SUCCESSFULL:" : "FAILED:");
                if(!matched)
                    failed++;

                stream.println("         xpath : "+testCase.xpaths.get(i));
                stream.println("    jdk result : "+jdkResult);
                stream.println("  jdk hitcount : "+jdkResult.size());
                stream.println("    dog result : "+dogResult);
                stream.println("  dog hitcount : "+dogResult.size());
                stream.flush();

                System.out.println("-------------------------------------------------");
                System.out.flush();
            }
        }
        System.out.format("testcases are executed: total=%d failed=%d %n", total, failed);

        System.out.println("      jdk time : "+jdkTime+" nanoseconds");
        System.out.println("      dog time : "+dogTime+" nanoseconds");
        double faster = (1.0*Math.max(dogTime, jdkTime)/Math.min(dogTime, jdkTime));
        System.out.format("        WINNER : %s (%.2fx faster) %n", dogTime<=jdkTime ? "XMLDog" : "XALAN", faster);
        long diff = Math.abs(dogTime - jdkTime);
        System.out.format("    Difference : %d nanoseconds/%.2f seconds %n", diff, diff*1E-09);

        System.out.println("\n--------------------------------------------------------------\n\n");

        if(failed>0){
            for(int i=0; i<10; i++)
                System.out.println("FAILED FAILED FAILED FAILED FAILED");
        }
    }

    public static void main(String[] args) throws Exception{
        new XPathPerformanceTest().run(args.length==0 ? "xpaths.xml" : args[0]);
    }
}

class TestCase{
    String file;
    List<String> xpaths = new ArrayList<String>();
    DefaultNamespaceContext nsContext = new DefaultNamespaceContext();
    Document doc;

    public void createDocument() throws ParserConfigurationException, IOException, SAXException{
        doc = DOMUtil.newDocumentBuilder(true, false, false).parse(new InputSource(file));
    }

    List<NodeList> jdkResult;
    public List<NodeList> usingJDK() throws Exception{
        if(doc==null)
            createDocument();

        List<NodeList> results = new ArrayList<NodeList>(xpaths.size());
        for(String xpath: xpaths){
            XPath xpathObj = XPathFactory.newInstance().newXPath();
            xpathObj.setNamespaceContext(nsContext);
            results.add((NodeList)xpathObj.evaluate(xpath, doc, XPathConstants.NODESET));
        }
        return results;
    }

    List<List<String>> dogResult;
    public List<List<String>> usingXMLDog() throws Exception{
        InputSource source = new InputSource(file);
        XMLDog dog = new XMLDog(nsContext);
        jlibs.xml.sax.sniff.XPath xpathObjs[] = new jlibs.xml.sax.sniff.XPath[xpaths.size()];
        for(int i=0; i<xpaths.size(); i++)
            xpathObjs[i] = dog.add(xpaths.get(i));

        XPathResults dogResults = dog.sniff(source);

        List<List<String>> results = new ArrayList<List<String>>(xpaths.size());
        for(jlibs.xml.sax.sniff.XPath xpathObj: xpathObjs)
            results.add(dogResults.getResult(xpathObj));
        return results;
    }
}