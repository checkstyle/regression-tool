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

package com.github.checkstyle.regression.configuration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.checkstyle.regression.data.ModuleInfo;

/**
 * Generates the config XML and output it to a specific file.
 * @author LuoLiangchen
 */
public final class ConfigGenerator {
    /** The "doctype-public" value of the config. */
    public static final String DOCTYPE_PUBLIC =
            "-//Puppy Crawl//DTD Check Configuration 1.3//EN";

    /** The "doctype-system" value of the config. */
    private static final String DOCTYPE_SYSTEM =
            "http://checkstyle.sourceforge.net/dtds/configuration_1_3.dtd";

    /** The parent name "Checker". */
    private static final String PARENT_CHECKER = "Checker";

    /** The parent name "TreeWalker". */
    private static final String PARENT_TREE_WALKER = "TreeWalker";

    /** The name of a module element. */
    private static final String ELEMENT_MODULE = "module";

    /** The attribute name "name". */
    private static final String ATTR_NAME = "name";

    /** Prevents instantiation. */
    private ConfigGenerator() {
    }

    /**
     * Creates a new {@link Document} instance from the {@code BASE_CONFIG}.
     * @return a new {@link Document} instance
     */
    private static Document createXmlDocument() {
        final Document document;

        try {
            final URL baseConfig = ConfigGenerator.class.getResource(
                    "/com/github/checkstyle/regression/configuration/base_config.xml");
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(baseConfig.toURI().toString());
        }
        catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            throw new IllegalStateException("cannot instantiate Document instance", ex);
        }

        return document;
    }

    /**
     * Creates a new {@link Transformer} instance and do initialization.
     * @return a new {@link Transformer} instance
     */
    private static Transformer createXmlTransformer() {
        final Transformer transformer;

        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, DOCTYPE_PUBLIC);
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCTYPE_SYSTEM);
        }
        catch (TransformerConfigurationException ex) {
            throw new IllegalStateException("cannot instantiate Transformer instance", ex);
        }

        return transformer;
    }

    /**
     * Generates the config XML file from the given module infos.
     * @param path        the path of the generated config
     * @param moduleInfos the given module infos
     * @return the generated config file
     * @throws IOException          failure of creating and writing the file
     * @throws TransformerException failure of transforming the XML document
     */
    public static File generateConfig(String path, List<ModuleInfo> moduleInfos)
            throws IOException, TransformerException {
        final File file = new File(path);
        Files.write(
                file.toPath(), generateConfigText(moduleInfos).getBytes(Charset.forName("UTF-8")),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    /**
     * Generates the plain text of the config XML from the given module infos.
     * @param moduleInfos the given module infos
     * @return the generated plain text of the config
     * @throws TransformerException failure of transforming the XML document
     */
    private static String generateConfigText(List<ModuleInfo> moduleInfos)
            throws TransformerException {
        final Document document = createXmlDocument();

        final Node checkerNode = getCheckerModuleNode(document);
        final Node treeWalkerNode = getTreeWalkerModuleNode(document);

        for (ModuleInfo moduleInfo : moduleInfos) {
            final Node moduleNode = createModuleNode(document, moduleInfo);
            final String parent = moduleInfo.moduleExtractInfo().parent();
            if (PARENT_CHECKER.equals(parent)) {
                checkerNode.appendChild(moduleNode);
            }
            else if (PARENT_TREE_WALKER.equals(parent)) {
                treeWalkerNode.appendChild(moduleNode);
            }
        }

        final Transformer transformer = createXmlTransformer();
        final StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * Creates a XML element node which represents the settings of a checkstyle module.
     * The information to create the node is grabbed from the given module info.
     * @param document   the XML document to create element
     * @param moduleInfo the given module info
     * @return a checkstyle module node
     */
    private static Node createModuleNode(Document document, ModuleInfo moduleInfo) {
        final Element moduleNode = document.createElement(ELEMENT_MODULE);
        moduleNode.setAttribute(ATTR_NAME, moduleInfo.name());
        return moduleNode;
    }

    /**
     * Gets the "TreeWalker" element node of the config document.
     * @param document the XML document to search
     * @return the "TreeWalker" element node
     */
    private static Node getTreeWalkerModuleNode(Document document) {
        Node returnValue = null;
        final NodeList nodeList = document.getDocumentElement()
                .getElementsByTagName(ELEMENT_MODULE);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node node = nodeList.item(i);
            final Node nameAttr = node.getAttributes().getNamedItem(ATTR_NAME);
            if (PARENT_TREE_WALKER.equals(nameAttr.getNodeValue())) {
                returnValue = node;
                break;
            }
        }

        return returnValue;
    }

    /**
     * Gets the "Checker" element node of the config document.
     * @param document the XML document to search
     * @return the "Checker" element node
     */
    private static Node getCheckerModuleNode(Document document) {
        return document.getDocumentElement();
    }
}
