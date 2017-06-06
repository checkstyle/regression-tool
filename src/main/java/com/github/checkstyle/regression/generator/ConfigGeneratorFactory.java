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

/**
 * Defines a factory API that enables developers to obtain a
 * generator that processes the git changes and generates config XML.
 * @author LuoLiangchen
 */
public final class ConfigGeneratorFactory {
    /** The path of checkstyle repository. */
    private String repoPath;

    /** The name of the branch to be compared with main code-base. */
    private String branch;

    /** Prevents instantiation. */
    private ConfigGeneratorFactory() {
    }

    /**
     * Gets a new instance of {@code ConfigGeneratorFactory}.
     * @param repoPath the path of checkstyle repository
     * @param branch the name of the branch to be compared with main code-base
     * @return the new instance of {@code ConfigGeneratorFactory}
     */
    public static ConfigGeneratorFactory newInstance(String repoPath, String branch) {
        final ConfigGeneratorFactory factory = new ConfigGeneratorFactory();
        factory.repoPath = repoPath;
        factory.branch = branch;
        return factory;
    }

    /**
     * Returns a new {@code DefaultConfigGenerator} instance.
     * @return the new {@code DefaultConfigGenerator} instance
     */
    public AbstractConfigGenerator newDefaultConfigGenerator() {
        final AbstractConfigGenerator generator = new DefaultConfigGenerator();
        initializeGenerator(generator);
        return generator;
    }

    /**
     * Initializes the generator.
     * @param generator the generator to be initialized.
     */
    private void initializeGenerator(AbstractConfigGenerator generator) {
        generator.setRepoPath(repoPath);
        generator.setBranch(branch);
    }
}
