////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.regression.generator;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.github.checkstyle.regression.git.DiffParser;
import com.github.checkstyle.regression.git.GitChange;

/**
 * The base class for config generators.
 * Contains the common initialization and generation logic.
 * @author LuoLiangchen
 */
public abstract class AbstractConfigGenerator {
    /** The name of a module element. */
    static final String ELEMENT_MODULE = "module";

    /** The attribute name of a module element. */
    static final String ATTR_NAME = "name";

    /** The "doctype-public" value of the config. */
    private static final String DOCTYPE_PUBLIC =
            "-//Puppy Crawl//DTD Check Configuration 1.3//EN";

    /** The "doctype-system" value of the config. */
    private static final String DOCTYPE_SYSTEM =
            "http://www.puppycrawl.com/dtds/configuration_1_3.dtd";

    /** The config template. */
    private static final String BASE_CONFIG = ""
            + "<?xml version=\"1.0\" standalone=\"yes\"?>"
            + "<module name=\"Checker\">\n"
            + "  <property name=\"charset\" value=\"UTF-8\"/>\n"
            + "  <property name=\"severity\" value=\"warning\"/>\n"
            + "  <property name=\"haltOnException\" value=\"false\"/>\n"
            + "  <module name=\"BeforeExecutionExclusionFileFilter\">\n"
            + "    <property name=\"fileNamePattern\" value=\"module\\-info\\.java$\" />\n"
            + "  </module>\n"
            + "\n"
            + "  <module name=\"TreeWalker\"></module>\n"
            + "</module>";

    /** The config XML document. */
    private static Document xmlDocument;

    /** The XML transformer. */
    private static Transformer xmlTransformer;

    /** The path of checkstyle repository. */
    private String repoPath;

    /** The name of the branch to be compared with main code-base. */
    private String branch;

    static {
        try {
            xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(BASE_CONFIG)));
            xmlTransformer = createXmlTransformer();
        }
        // -@cs[IllegalCatch] remove this after finding out a proper way to log message
        catch (Exception ignore) {
            // ignore, to-do: show error msg
        }
    }

    /**
     * Sets the path of checkstyle repository.
     * @param repoPath the path of checkstyle repository
     */
    final void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    /**
     * Sets the name of the branch to be compared with main code-base.
     * @param branch the name of the branch to be compared with main code-base
     */
    final void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * Creates a new {@code Transformer} instance and do initialization.
     * @return a new {@code Transformer} instance
     * @throws TransformerConfigurationException fails to instantiate the {@code Transformer}
     */
    private static Transformer createXmlTransformer() throws TransformerConfigurationException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, DOCTYPE_PUBLIC);
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCTYPE_SYSTEM);
        return transformer;
    }

    /**
     * Generates the config XML.
     */
    public void generate() {
        try {
            final Node treeWalkerNode = getTreeWalkerModuleNode();
            final List<GitChange> gitChanges = new DiffParser(repoPath).parse(branch);
            gitChanges.removeIf(this::whetherToSkipChange);
            createModuleElements(gitChanges).forEach(treeWalkerNode::appendChild);
            // Currently the output is printed to System.out
            // It should be exported to a file in the future
            xmlTransformer.transform(new DOMSource(xmlDocument), new StreamResult(System.out));
        }
        // -@cs[IllegalCatch] remove this after finding out a proper way to log message
        catch (Exception ex) {
            // ignore, to-do: show error msg
        }
    }

    /**
     * Gets the "TreeWalker" element node of the config document.
     * @return the "TreeWalker" element node
     */
    private static Node getTreeWalkerModuleNode() {
        final NodeList nodeList = xmlDocument.getDocumentElement()
                .getElementsByTagName(ELEMENT_MODULE);
        Node returnValue = null;
        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node node = nodeList.item(i);
            final Node nameAttr = node.getAttributes().getNamedItem(ATTR_NAME);
            if (nameAttr != null && "TreeWalker".equals(nameAttr.getNodeValue())) {
                returnValue = node;
                break;
            }
        }
        return returnValue;
    }

    /**
     * Gets the config XML document.
     * @return the config XML document
     */
    static Document getXmlDocument() {
        return xmlDocument;
    }

    /**
     * Creates checkstyle module elements from a list of {@code GitChange}.
     * @param gitChanges the git changes to be processed
     * @return the generated module elements
     */
    protected abstract List<Element> createModuleElements(List<GitChange> gitChanges);

    /**
     * Returns whether to skip the given change when generator processes the git changes.
     * @param gitChange the git change to be judged
     * @return true if the change would be skipped
     */
    protected boolean whetherToSkipChange(GitChange gitChange) {
        return false;
    }
}
